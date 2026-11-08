package net.ultranetwork.render.text;

import net.ultranetwork.render.graphics.Texture;
import org.jetbrains.annotations.Nullable;

/**
 * A single font glyph, described in GUI pixels relative to the pen position and the top of the line.
 *
 * @param texture      glyph sheet, or {@code null} for invisible glyphs such as spaces
 * @param u            left texel of the glyph in {@code texture}
 * @param v            top texel of the glyph in {@code texture}
 * @param texelWidth   glyph width in texels
 * @param texelHeight  glyph height in texels
 * @param texelSize    GUI pixels per texel (1 / oversample)
 * @param left         horizontal offset of the glyph's left edge from the pen position
 * @param top          vertical offset of the glyph's top edge from the line's top
 * @param advance      how far the pen moves after this glyph
 * @param boldOffset   extra advance and offset of the second draw when bold
 * @param shadowOffset distance of the drop shadow
 */
public record Glyph(
    @Nullable Texture texture,
    int u,
    int v,
    int texelWidth,
    int texelHeight,
    float texelSize,
    float left,
    float top,
    float advance,
    float boldOffset,
    float shadowOffset
) {

  public static Glyph space(float advance) {
    return new Glyph(null, 0, 0, 0, 0, 1f, 0f, 0f, advance, 1f, 1f);
  }

  public boolean visible() {
    return this.texture != null && this.texelWidth > 0 && this.texelHeight > 0;
  }

  public float advance(boolean bold) {
    return this.advance + (bold ? this.boldOffset : 0f);
  }

  public float right() {
    return this.left + this.texelWidth * this.texelSize;
  }

  public float bottom() {
    return this.top + this.texelHeight * this.texelSize;
  }
}
