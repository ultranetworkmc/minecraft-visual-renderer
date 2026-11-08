package net.ultranetwork.render.text;

import java.util.function.IntConsumer;
import org.jetbrains.annotations.Nullable;

interface GlyphProvider {

  @Nullable Glyph glyph(int codepoint);

  /**
   * Visits every codepoint this provider can supply, used to pick replacement glyphs for obfuscated text.
   */
  void forEachCodepoint(IntConsumer consumer);
}
