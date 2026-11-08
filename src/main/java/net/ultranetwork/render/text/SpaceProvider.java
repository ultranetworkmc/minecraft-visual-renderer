package net.ultranetwork.render.text;

import java.util.Map;
import java.util.function.IntConsumer;
import org.jetbrains.annotations.Nullable;

final class SpaceProvider implements GlyphProvider {
  private final Map<Integer, Glyph> glyphs;

  SpaceProvider(Map<Integer, Float> advances) {
    this.glyphs = new java.util.HashMap<>();
    advances.forEach((codepoint, advance) -> this.glyphs.put(codepoint, Glyph.space(advance)));
  }

  static SpaceProvider vanillaDefault() {
    return new SpaceProvider(Map.of((int) ' ', 4f, 0x200C, 0f));
  }

  @Override
  public @Nullable Glyph glyph(int codepoint) {
    return this.glyphs.get(codepoint);
  }

  @Override
  public void forEachCodepoint(IntConsumer consumer) {
  }
}
