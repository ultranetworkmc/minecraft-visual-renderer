package net.ultranetwork.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import net.kyori.adventure.text.Component;
import net.ultranetwork.render.assets.MinecraftAssets;
import net.ultranetwork.render.text.Glyph;
import net.ultranetwork.render.text.MinecraftFont;
import net.ultranetwork.render.text.TextRenderer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Renders with the real game assets. Downloads them from Mojang, so it only runs when asked:
 * {@code ./gradlew test -Pmvr.assets.version=26.3}.
 */
@EnabledIfSystemProperty(named = "mvr.assets.version", matches = ".+")
class VanillaAssetsTest {
  private static MinecraftAssets assets;
  private static RenderEnvironment env;

  @BeforeAll
  static void load() throws IOException {
    final Path cache = Path.of(System.getProperty("mvr.assets.cache", "build/minecraft-assets"));
    assets = MinecraftAssets.download(System.getProperty("mvr.assets.version"), cache);
    env = RenderEnvironment.fromAssets(assets);
  }

  @AfterAll
  static void close() throws IOException {
    assets.close();
  }

  @Test
  void rendersSamples() throws IOException {
    Samples.write("vanilla_tooltip", Samples.tooltip(env));
    Samples.write("vanilla_tab_list", Samples.tabList(env));
    Samples.write("vanilla_tab_list_transparent", Samples.tabList(env, true, null));
    Samples.write("vanilla_tab_list_sky", Samples.tabList(env, false, Backdrop.sky()));
    Samples.write("vanilla_tab_list_transparent_sky", Samples.tabList(env, true, Backdrop.sky()));
  }

  @Test
  void translatesWithLanguageFile() {
    final var text = env.text(Component.translatable("item.minecraft.diamond_sword"));
    assertEquals("Diamond Sword", text.plainText());
  }

  @Test
  void usesUnifontForOtherScripts() {
    final Glyph glyph = env.font(null).glyph('中');
    assertEquals(0.5f, glyph.texelSize());
    assertTrue(glyph.visible());
  }

  /**
   * Minecraftia (the fallback font) is a recreation, not a copy: these glyphs are drawn slightly differently.
   */
  @Test
  void fallbackFontDiffersFromVanillaOnlyInKnownGlyphs() {
    final MinecraftFont vanilla = env.font(null);
    final MinecraftFont fallback = MinecraftFont.fallback();
    final StringBuilder advanceMismatches = new StringBuilder();
    final StringBuilder shapeMismatches = new StringBuilder();
    for (int c = 0x20; c < 0x7F; c++) {
      final Glyph v = vanilla.glyph(c);
      final Glyph f = fallback.glyph(c);
      if (v.advance() != f.advance()) {
        advanceMismatches.append((char) c);
      }
      if (v.visible() && !shape(v).equals(shape(f))) {
        shapeMismatches.append((char) c);
      }
    }
    assertEquals("()*{}", advanceMismatches.toString());
    assertEquals("()*+,-.2:;@G`t{|}", shapeMismatches.toString());
  }

  @Test
  void widthsMatchBetweenEnvironments() {
    final var sample = Component.text("The quick brown fox jumps over the lazy dog 0123456789");
    assertEquals(TextRenderer.width(env, env.text(sample)),
        TextRenderer.width(RenderEnvironment.fallback(), RenderEnvironment.fallback().text(sample)));
  }

  /**
   * The glyph's opaque pixels as GUI-pixel coordinates relative to the pen position and line top.
   */
  private static String shape(Glyph glyph) {
    final StringBuilder builder = new StringBuilder();
    if (!glyph.visible()) {
      return "";
    }
    for (int y = 0; y < glyph.texelHeight(); y++) {
      for (int x = 0; x < glyph.texelWidth(); x++) {
        if ((glyph.texture().get(glyph.u() + x, glyph.v() + y) >>> 24) != 0) {
          builder.append(Math.round(glyph.left() + x * glyph.texelSize())).append(',')
              .append(Math.round(glyph.top() + y * glyph.texelSize())).append(' ');
        }
      }
    }
    return builder.toString();
  }
}
