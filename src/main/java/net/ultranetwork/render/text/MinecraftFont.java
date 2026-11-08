package net.ultranetwork.render.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.ultranetwork.render.graphics.Texture;
import org.jetbrains.annotations.NotNull;

/**
 * A resolved font: an ordered list of glyph providers where the first provider supplying a codepoint wins, exactly
 * like a vanilla {@code FontSet}.
 */
public final class MinecraftFont {
  static final Glyph MISSING = missingGlyph();

  private final List<GlyphProvider> providers;
  private final Map<Integer, Glyph> cache = new ConcurrentHashMap<>();
  private volatile Map<Integer, int[]> glyphsByWidth;

  MinecraftFont(@NotNull List<GlyphProvider> providers) {
    this.providers = List.copyOf(providers);
  }

  /**
   * The built-in font used when no vanilla assets are configured.
   */
  public static @NotNull MinecraftFont fallback() {
    return Fallback.INSTANCE;
  }

  /**
   * A font with no providers, rendering every character as the missing glyph (vanilla's behaviour for unknown
   * fonts).
   */
  public static @NotNull MinecraftFont empty() {
    return new MinecraftFont(List.of());
  }

  public @NotNull Glyph glyph(int codepoint) {
    final Glyph cached = this.cache.get(codepoint);
    if (cached != null) {
      return cached;
    }
    Glyph found = MISSING;
    for (GlyphProvider provider : this.providers) {
      final Glyph glyph = provider.glyph(codepoint);
      if (glyph != null) {
        found = glyph;
        break;
      }
    }
    this.cache.put(codepoint, found);
    return found;
  }

  /**
   * Picks a random glyph with the same rounded-up advance, as used for obfuscated ({@code §k}) text.
   */
  @NotNull Glyph randomGlyph(@NotNull Glyph original, @NotNull Random random) {
    final int[] candidates = this.glyphsByWidth().get((int) Math.ceil(original.advance()));
    if (candidates == null || candidates.length == 0) {
      return MISSING;
    }
    return this.glyph(candidates[random.nextInt(candidates.length)]);
  }

  private Map<Integer, int[]> glyphsByWidth() {
    Map<Integer, int[]> result = this.glyphsByWidth;
    if (result == null) {
      synchronized (this) {
        result = this.glyphsByWidth;
        if (result == null) {
          final Map<Integer, List<Integer>> grouped = new HashMap<>();
          for (GlyphProvider provider : this.providers) {
            provider.forEachCodepoint(codepoint -> {
              final Glyph glyph = this.glyph(codepoint);
              grouped.computeIfAbsent((int) Math.ceil(glyph.advance()), w -> new ArrayList<>()).add(codepoint);
            });
          }
          result = new HashMap<>();
          for (Map.Entry<Integer, List<Integer>> entry : grouped.entrySet()) {
            result.put(entry.getKey(), entry.getValue().stream().distinct().mapToInt(Integer::intValue).toArray());
          }
          this.glyphsByWidth = result;
        }
      }
    }
    return result;
  }

  private static Glyph missingGlyph() {
    final int width = 5;
    final int height = 8;
    final int[] argb = new int[width * height];
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (x == 0 || x + 1 == width || y == 0 || y + 1 == height) {
          argb[y * width + x] = 0xFFFFFFFF;
        }
      }
    }
    return new Glyph(new Texture(width, height, argb), 0, 0, width, height, 1f, 0f, 0f, width + 1, 1f, 1f);
  }

  private static final class Fallback {
    static final MinecraftFont INSTANCE = new MinecraftFont(List.of(
        SpaceProvider.vanillaDefault(),
        RasterizedFontProvider.load()
    ));
  }
}
