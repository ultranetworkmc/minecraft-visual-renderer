package net.ultranetwork.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.translation.GlobalTranslator;
import net.ultranetwork.render.assets.MinecraftAssets;
import net.ultranetwork.render.graphics.SpriteSource;
import net.ultranetwork.render.text.FontLoader;
import net.ultranetwork.render.text.Fonts;
import net.ultranetwork.render.text.MinecraftFont;
import net.ultranetwork.render.text.StyledText;
import net.ultranetwork.render.text.VanillaFlattener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Everything a renderer needs to look like the game: fonts, GUI sprites and translations.
 *
 * <ul>
 *   <li>{@link #fromAssets(MinecraftAssets)} uses real vanilla (or resource pack) assets and is pixel-identical to
 *   the client.</li>
 *   <li>{@link #fallback()} needs nothing: it uses the bundled Minecraftia font and generated sprites, which closely
 *   match vanilla for Latin text.</li>
 * </ul>
 *
 * <p>Environments are thread-safe and meant to be created once and reused.</p>
 */
public final class RenderEnvironment implements Fonts {
  private static final Key DEFAULT_FONT = Key.key("default");
  private static volatile @Nullable RenderEnvironment defaultEnvironment;

  private final @Nullable MinecraftAssets assets;
  private final MinecraftFont defaultFont;
  private final Map<Key, MinecraftFont> fonts = new ConcurrentHashMap<>();
  private final SpriteSource sprites;
  private final ComponentFlattener flattener;

  private RenderEnvironment(@Nullable MinecraftAssets assets, @NotNull MinecraftFont defaultFont,
                            @NotNull SpriteSource sprites, @NotNull ComponentFlattener flattener)
  {
    this.assets = assets;
    this.defaultFont = defaultFont;
    this.sprites = sprites;
    this.flattener = flattener;
  }

  public static @NotNull RenderEnvironment fallback() {
    return FallbackHolder.INSTANCE;
  }

  public static @NotNull RenderEnvironment fromAssets(@NotNull MinecraftAssets assets) throws IOException {
    return fromAssets(assets, "en_us");
  }

  /**
   * @param language language file used for translatable components, e.g. {@code en_us}
   */
  public static @NotNull RenderEnvironment fromAssets(@NotNull MinecraftAssets assets, @NotNull String language)
      throws IOException
  {
    final MinecraftFont font = FontLoader.load(assets, DEFAULT_FONT);
    if (font == null) {
      throw new IOException("Assets contain no minecraft:default font");
    }
    final Map<String, String> translations = new HashMap<>();
    final JsonObject lang = readLang(assets, language);
    if (lang != null) {
      for (Map.Entry<String, JsonElement> entry : lang.entrySet()) {
        translations.put(entry.getKey(), entry.getValue().getAsString());
      }
    }
    return new RenderEnvironment(assets, font, SpriteSource.assets(assets), VanillaFlattener.create(translations::get));
  }

  /**
   * The environment used by renderers that were not given one. {@link #fallback()} unless changed with
   * {@link #setDefault(RenderEnvironment)}.
   */
  public static @NotNull RenderEnvironment defaultEnvironment() {
    final RenderEnvironment environment = defaultEnvironment;
    return environment != null ? environment : fallback();
  }

  public static void setDefault(@Nullable RenderEnvironment environment) {
    defaultEnvironment = environment;
  }

  public boolean usesVanillaAssets() {
    return this.assets != null;
  }

  public @NotNull SpriteSource sprites() {
    return this.sprites;
  }

  public @NotNull ComponentFlattener flattener() {
    return this.flattener;
  }

  @Override
  public @NotNull MinecraftFont font(@Nullable Key id) {
    if (id == null || id.equals(DEFAULT_FONT) || this.assets == null) {
      return this.defaultFont;
    }
    return this.fonts.computeIfAbsent(id, key -> {
      try {
        final MinecraftFont font = FontLoader.load(Objects.requireNonNull(this.assets), key);
        return font != null ? font : MissingFontHolder.INSTANCE;
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

  /**
   * Resolves plugin translations through Adventure's {@link GlobalTranslator}, then flattens the component.
   */
  public @NotNull StyledText text(@NotNull Component component) {
    return StyledText.of(GlobalTranslator.render(component, Locale.US), this.flattener);
  }

  private static @Nullable JsonObject readLang(MinecraftAssets assets, String language) throws IOException {
    try (InputStream in = assets.open("assets/minecraft/lang/" + language + ".json")) {
      if (in == null) {
        return null;
      }
      return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
    }
  }

  private static final class FallbackHolder {
    static final RenderEnvironment INSTANCE = new RenderEnvironment(null, MinecraftFont.fallback(),
        SpriteSource.fallback(), PlatformFlattener.detect());
  }

  private static final class MissingFontHolder {
    static final MinecraftFont INSTANCE = MinecraftFont.empty();
  }

  /**
   * Uses Paper's server-side flattener (which knows vanilla's English translations) when running on Paper.
   */
  private static final class PlatformFlattener {
    static ComponentFlattener detect() {
      try {
        Class.forName("io.papermc.paper.text.PaperComponents");
        return net.ultranetwork.render.paper.PaperSupport.flattener();
      }
      catch (Throwable ignored) {
        return VanillaFlattener.create(key -> null);
      }
    }
  }
}
