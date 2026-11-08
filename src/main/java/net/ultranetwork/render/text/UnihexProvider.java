package net.ultranetwork.render.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.ultranetwork.render.graphics.Texture;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code unihex} font provider (GNU Unifont {@code .hex} files), used by vanilla for most non-Latin scripts.
 */
final class UnihexProvider implements GlyphProvider {
  private static final int GLYPH_HEIGHT = 16;

  record OverrideRange(int from, int to, int left, int right) {
  }

  private final Map<Integer, int[]> lines = new HashMap<>();
  private final Map<Integer, Long> dimensions = new HashMap<>();
  private final Map<Integer, Glyph> baked = new ConcurrentHashMap<>();

  UnihexProvider(InputStream zip, List<OverrideRange> overrides) throws IOException {
    try (ZipInputStream in = new ZipInputStream(zip)) {
      ZipEntry entry;
      while ((entry = in.getNextEntry()) != null) {
        if (entry.getName().endsWith(".hex")) {
          this.readHex(in, overrides);
        }
      }
    }
  }

  private void readHex(InputStream in, List<OverrideRange> overrides) throws IOException {
    final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));
    String line;
    while ((line = reader.readLine()) != null) {
      final int colon = line.indexOf(':');
      if (colon <= 0) {
        continue;
      }
      final int codepoint = Integer.parseInt(line, 0, colon, 16);
      final String data = line.substring(colon + 1).trim();
      final int digitsPerRow = data.length() / GLYPH_HEIGHT;
      if (data.length() % GLYPH_HEIGHT != 0 || digitsPerRow < 2 || digitsPerRow > 8 || digitsPerRow % 2 != 0) {
        continue;
      }
      final int bitWidth = digitsPerRow * 4;
      final int[] rows = new int[GLYPH_HEIGHT];
      for (int row = 0; row < GLYPH_HEIGHT; row++) {
        final long bits = Long.parseLong(data, row * digitsPerRow, (row + 1) * digitsPerRow, 16);
        rows[row] = (int) (bits << (32 - bitWidth));
      }
      this.lines.put(codepoint, rows);
      this.dimensions.put(codepoint, dimensions(codepoint, rows, bitWidth, overrides));
    }
  }

  private static long dimensions(int codepoint, int[] rows, int bitWidth, List<OverrideRange> overrides) {
    for (OverrideRange range : overrides) {
      if (codepoint >= range.from() && codepoint <= range.to()) {
        return pack(range.left(), range.right());
      }
    }
    int mask = 0;
    for (int row : rows) {
      mask |= row;
    }
    if (mask == 0) {
      return pack(0, bitWidth);
    }
    return pack(Integer.numberOfLeadingZeros(mask), 32 - Integer.numberOfTrailingZeros(mask) - 1);
  }

  private static long pack(int left, int right) {
    return ((long) left << 32) | (right & 0xFFFFFFFFL);
  }

  @Override
  public @Nullable Glyph glyph(int codepoint) {
    final int[] rows = this.lines.get(codepoint);
    if (rows == null) {
      return null;
    }
    return this.baked.computeIfAbsent(codepoint, cp -> {
      final long dims = this.dimensions.get(cp);
      final int left = (int) (dims >> 32);
      final int right = (int) dims;
      final int width = right - left + 1;
      final int[] argb = new int[Math.max(0, width) * GLYPH_HEIGHT];
      for (int y = 0; y < GLYPH_HEIGHT; y++) {
        for (int x = 0; x < width; x++) {
          final int bit = 31 - (left + x);
          if (bit >= 0 && bit < 32 && ((rows[y] >>> bit) & 1) != 0) {
            argb[y * width + x] = 0xFFFFFFFF;
          }
        }
      }
      final Texture texture = new Texture(Math.max(0, width), GLYPH_HEIGHT, argb);
      return new Glyph(texture, 0, 0, Math.max(0, width), GLYPH_HEIGHT, 0.5f, 0f, 0f, width / 2 + 1, 0.5f, 0.5f);
    });
  }

  @Override
  public void forEachCodepoint(IntConsumer consumer) {
    this.lines.keySet().forEach(consumer::accept);
  }
}
