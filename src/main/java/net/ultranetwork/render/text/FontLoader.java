package net.ultranetwork.render.text;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.ultranetwork.render.assets.MinecraftAssets;
import net.ultranetwork.render.graphics.Texture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Loads font definitions ({@code assets/<namespace>/font/<path>.json}) from vanilla assets or resource packs.
 */
public final class FontLoader {
  private static final System.Logger LOGGER = System.getLogger(FontLoader.class.getName());

  /**
   * Font options that provider filters are checked against; these are vanilla's defaults.
   */
  private static final Map<String, Boolean> OPTIONS = Map.of("uniform", false, "jp", false);

  private FontLoader() {
  }

  /**
   * @return the font, or {@code null} if no definition exists for it
   */
  public static @Nullable MinecraftFont load(@NotNull MinecraftAssets assets, @NotNull Key id) throws IOException {
    final JsonObject definition = readJson(assets, path(id, "font/", ".json"));
    if (definition == null) {
      return null;
    }
    final List<GlyphProvider> providers = new ArrayList<>();
    final Set<Key> visited = new HashSet<>();
    visited.add(id);
    addProviders(assets, definition, providers, visited);
    return new MinecraftFont(providers);
  }

  private static void addProviders(MinecraftAssets assets, JsonObject definition, List<GlyphProvider> out,
                                   Set<Key> visited)
  {
    final JsonArray providers = definition.getAsJsonArray("providers");
    if (providers == null) {
      return;
    }
    for (JsonElement element : providers) {
      final JsonObject provider = element.getAsJsonObject();
      if (!matchesFilter(provider.getAsJsonObject("filter"))) {
        continue;
      }
      final String type = provider.get("type").getAsString();
      try {
        switch (type) {
          case "reference" -> {
            final Key referenced = Key.key(provider.get("id").getAsString());
            if (visited.add(referenced)) {
              final JsonObject nested = readJson(assets, path(referenced, "font/", ".json"));
              if (nested != null) {
                addProviders(assets, nested, out, visited);
              }
            }
          }
          case "bitmap" -> out.add(bitmap(assets, provider));
          case "space" -> out.add(space(provider));
          case "unihex" -> out.add(unihex(assets, provider));
          default -> LOGGER.log(System.Logger.Level.DEBUG, "Skipping unsupported font provider type {0}", type);
        }
      }
      catch (IOException | RuntimeException e) {
        LOGGER.log(System.Logger.Level.WARNING, "Skipping broken font provider " + provider, e);
      }
    }
  }

  private static boolean matchesFilter(@Nullable JsonObject filter) {
    if (filter == null) {
      return true;
    }
    for (Map.Entry<String, JsonElement> entry : filter.entrySet()) {
      final boolean expected = OPTIONS.getOrDefault(entry.getKey(), false);
      if (entry.getValue().getAsBoolean() != expected) {
        return false;
      }
    }
    return true;
  }

  private static GlyphProvider bitmap(MinecraftAssets assets, JsonObject provider) throws IOException {
    final String file = path(Key.key(provider.get("file").getAsString()), "textures/", "");
    final Texture sheet;
    try (InputStream in = open(assets, file)) {
      sheet = Texture.read(in);
    }
    final int height = provider.has("height") ? provider.get("height").getAsInt() : 8;
    final int ascent = provider.get("ascent").getAsInt();
    final List<int[]> rows = new ArrayList<>();
    for (JsonElement row : provider.getAsJsonArray("chars")) {
      rows.add(row.getAsString().codePoints().toArray());
    }
    return new BitmapProvider(sheet, height, ascent, rows);
  }

  private static GlyphProvider space(JsonObject provider) {
    final Map<Integer, Float> advances = new HashMap<>();
    for (Map.Entry<String, JsonElement> entry : provider.getAsJsonObject("advances").entrySet()) {
      advances.put(entry.getKey().codePointAt(0), entry.getValue().getAsFloat());
    }
    return new SpaceProvider(advances);
  }

  private static GlyphProvider unihex(MinecraftAssets assets, JsonObject provider) throws IOException {
    final List<UnihexProvider.OverrideRange> overrides = new ArrayList<>();
    final JsonArray sizeOverrides = provider.getAsJsonArray("size_overrides");
    if (sizeOverrides != null) {
      for (JsonElement element : sizeOverrides) {
        final JsonObject range = element.getAsJsonObject();
        overrides.add(new UnihexProvider.OverrideRange(
            range.get("from").getAsString().codePointAt(0),
            range.get("to").getAsString().codePointAt(0),
            range.get("left").getAsInt(),
            range.get("right").getAsInt()));
      }
    }
    final String file = path(Key.key(provider.get("hex_file").getAsString()), "", "");
    try (InputStream in = open(assets, file)) {
      return new UnihexProvider(in, overrides);
    }
  }

  static @Nullable JsonObject readJson(MinecraftAssets assets, String path) throws IOException {
    try (InputStream in = assets.open(path)) {
      if (in == null) {
        return null;
      }
      return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
    }
  }

  private static InputStream open(MinecraftAssets assets, String path) throws IOException {
    final InputStream in = assets.open(path);
    if (in == null) {
      throw new IOException("Missing asset " + path);
    }
    return in;
  }

  static String path(Key key, String prefix, String suffix) {
    return "assets/" + key.namespace() + "/" + prefix + key.value() + suffix;
  }
}
