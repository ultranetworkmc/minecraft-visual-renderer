package net.ultranetwork.render.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;
import net.ultranetwork.render.graphics.Texture;
import org.jetbrains.annotations.Nullable;

/**
 * Rasterizes the bundled Minecraftia font (a pixel-accurate recreation of the vanilla ASCII font) into 8px glyphs
 * using the same width rules as the vanilla {@code bitmap} provider. Used when no vanilla assets are configured.
 */
final class RasterizedFontProvider implements GlyphProvider {
  private static final String RESOURCE = "/minecraft_font.ttf";
  private static final int PEN_X = 8;
  private static final int BASELINE = 16;
  private static final int CANVAS = 32;

  private final Font font;
  private final Map<Integer, Optional<Glyph>> cache = new ConcurrentHashMap<>();
  // Canvas row that corresponds to the top of a vanilla glyph cell (capitals occupy cell rows 0-6).
  private final int cellTop;

  private RasterizedFontProvider(Font font) {
    this.font = font;
    final int[] capital = bounds(this.raster('H'));
    this.cellTop = capital == null ? BASELINE - 7 : capital[3] - 6;
  }

  static RasterizedFontProvider load() {
    try (InputStream in = RasterizedFontProvider.class.getResourceAsStream(RESOURCE)) {
      if (in == null) {
        throw new IllegalStateException("Missing bundled font " + RESOURCE);
      }
      return new RasterizedFontProvider(Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(8f));
    }
    catch (IOException | FontFormatException e) {
      throw new IllegalStateException("Could not load bundled font " + RESOURCE, e);
    }
  }

  @Override
  public @Nullable Glyph glyph(int codepoint) {
    return this.cache.computeIfAbsent(codepoint, this::rasterize).orElse(null);
  }

  private Optional<Glyph> rasterize(int codepoint) {
    if (!this.font.canDisplay(codepoint) || Character.isISOControl(codepoint)) {
      return Optional.empty();
    }

    final BufferedImage image = this.raster(codepoint);
    final int[] bounds = bounds(image);
    if (bounds == null) {
      return Optional.of(Glyph.space(Math.max(1, Math.round(
          this.font.getStringBounds(new String(Character.toChars(codepoint)),
              new FontRenderContext(null, false, false)).getWidth()))));
    }
    final int minX = bounds[0];
    final int minY = bounds[1];
    final int maxX = bounds[2];
    final int maxY = bounds[3];

    final int startX = Math.min(minX, PEN_X);
    final int width = maxX - startX + 1;
    final int height = maxY - minY + 1;
    final int[] argb = new int[width * height];
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if ((image.getRGB(startX + x, minY + y) >>> 24) >= 128) {
          argb[y * width + x] = 0xFFFFFFFF;
        }
      }
    }

    final int actualWidth = maxX - PEN_X + 1;
    return Optional.of(new Glyph(new Texture(width, height, argb), 0, 0, width, height, 1f,
        startX - PEN_X, minY - this.cellTop, Math.max(0, actualWidth) + 1, 1f, 1f));
  }

  private BufferedImage raster(int codepoint) {
    final BufferedImage image = new BufferedImage(CANVAS, CANVAS, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D graphics = image.createGraphics();
    try {
      graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
      graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
      graphics.setFont(this.font);
      graphics.setColor(Color.WHITE);
      graphics.drawString(new String(Character.toChars(codepoint)), PEN_X, BASELINE);
    }
    finally {
      graphics.dispose();
    }
    return image;
  }

  /**
   * @return {@code [minX, minY, maxX, maxY]} of the opaque pixels, or {@code null} if there are none
   */
  private static int @Nullable [] bounds(BufferedImage image) {
    int minX = CANVAS;
    int maxX = -1;
    int minY = CANVAS;
    int maxY = -1;
    for (int y = 0; y < CANVAS; y++) {
      for (int x = 0; x < CANVAS; x++) {
        if ((image.getRGB(x, y) >>> 24) >= 128) {
          minX = Math.min(minX, x);
          maxX = Math.max(maxX, x);
          minY = Math.min(minY, y);
          maxY = Math.max(maxY, y);
        }
      }
    }
    return maxX < 0 ? null : new int[] {minX, minY, maxX, maxY};
  }

  @Override
  public void forEachCodepoint(IntConsumer consumer) {
    for (int codepoint = 0x21; codepoint < 0x7F; codepoint++) {
      if (this.font.canDisplay(codepoint)) {
        consumer.accept(codepoint);
      }
    }
  }
}
