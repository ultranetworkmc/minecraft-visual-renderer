package net.ultranetwork.render.text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import net.ultranetwork.render.graphics.Texture;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code bitmap} font provider: a grid of glyph cells in a single image.
 */
final class BitmapProvider implements GlyphProvider {
  private final Map<Integer, Glyph> glyphs = new HashMap<>();

  BitmapProvider(Texture sheet, int height, int ascent, List<int[]> rows) {
    final int columns = rows.isEmpty() ? 0 : rows.getFirst().length;
    if (columns == 0) {
      return;
    }
    final int cellWidth = sheet.width() / columns;
    final int cellHeight = sheet.height() / rows.size();
    final float scale = (float) height / cellHeight;

    for (int row = 0; row < rows.size(); row++) {
      final int[] codepoints = rows.get(row);
      for (int column = 0; column < codepoints.length; column++) {
        final int codepoint = codepoints[column];
        if (codepoint == 0) {
          continue;
        }
        final int u = column * cellWidth;
        final int v = row * cellHeight;
        final int actualWidth = actualWidth(sheet, u, v, cellWidth, cellHeight);
        final int advance = (int) (0.5d + actualWidth * scale) + 1;
        this.glyphs.put(codepoint, new Glyph(sheet, u, v, cellWidth, cellHeight, scale,
            0f, 7f - ascent, advance, 1f, 1f));
      }
    }
  }

  private static int actualWidth(Texture sheet, int u, int v, int cellWidth, int cellHeight) {
    for (int x = cellWidth - 1; x >= 0; x--) {
      for (int y = 0; y < cellHeight; y++) {
        if ((sheet.get(u + x, v + y) >>> 24) != 0) {
          return x + 1;
        }
      }
    }
    return 0;
  }

  @Override
  public @Nullable Glyph glyph(int codepoint) {
    return this.glyphs.get(codepoint);
  }

  @Override
  public void forEachCodepoint(IntConsumer consumer) {
    this.glyphs.keySet().forEach(consumer::accept);
  }
}
