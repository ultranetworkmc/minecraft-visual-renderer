package net.ultranetwork.render.graphics;

import java.awt.image.BufferedImage;
import org.jetbrains.annotations.NotNull;

/**
 * A software rasterizer working in Minecraft GUI coordinates.
 *
 * <p>Geometry is specified in GUI pixels and rasterized at an integer GUI scale the same way the game's GPU path does:
 * a pixel is covered when its centre lies inside the primitive, textures are sampled with nearest filtering and
 * colours are combined with standard alpha blending.</p>
 */
public final class Canvas {
  private final float originX;
  private final float originY;
  private final int scale;
  private final int width;
  private final int height;
  private final int[] pixels;

  /**
   * @param originX   GUI x coordinate of the canvas' left edge
   * @param originY   GUI y coordinate of the canvas' top edge
   * @param guiWidth  width in GUI pixels
   * @param guiHeight height in GUI pixels
   * @param scale     output pixels per GUI pixel
   */
  public Canvas(float originX, float originY, int guiWidth, int guiHeight, int scale) {
    if (scale < 1) {
      throw new IllegalArgumentException("Scale must be at least 1");
    }
    this.originX = originX;
    this.originY = originY;
    this.scale = scale;
    this.width = Math.max(0, guiWidth) * scale;
    this.height = Math.max(0, guiHeight) * scale;
    this.pixels = new int[this.width * this.height];
  }

  public int width() {
    return this.width;
  }

  public int height() {
    return this.height;
  }

  public int scale() {
    return this.scale;
  }

  public int pixel(int x, int y) {
    return this.pixels[y * this.width + x];
  }

  private int firstColumn(float guiX) {
    return (int) Math.ceil((guiX - this.originX) * this.scale - 0.5f);
  }

  private int firstRow(float guiY) {
    return (int) Math.ceil((guiY - this.originY) * this.scale - 0.5f);
  }

  private float centreX(int px) {
    return this.originX + (px + 0.5f) / this.scale;
  }

  private float centreY(int py) {
    return this.originY + (py + 0.5f) / this.scale;
  }

  public void fill(float x0, float y0, float x1, float y1, int argb) {
    if ((argb >>> 24) == 0) {
      return;
    }
    final int left = Math.max(0, this.firstColumn(Math.min(x0, x1)));
    final int right = Math.min(this.width, this.firstColumn(Math.max(x0, x1)));
    final int top = Math.max(0, this.firstRow(Math.min(y0, y1)));
    final int bottom = Math.min(this.height, this.firstRow(Math.max(y0, y1)));

    final float a = (argb >>> 24) / 255f;
    final float r = (argb >> 16 & 0xFF) / 255f;
    final float g = (argb >> 8 & 0xFF) / 255f;
    final float b = (argb & 0xFF) / 255f;
    for (int py = top; py < bottom; py++) {
      for (int px = left; px < right; px++) {
        this.blend(py * this.width + px, r, g, b, a);
      }
    }
  }

  /**
   * Draws a textured parallelogram. The top edge is shifted horizontally by {@code shearTop} and the bottom edge by
   * {@code shearBottom}, matching how italic glyph quads are built.
   *
   * @param u0 left texel coordinate of the sampled region
   * @param v0 top texel coordinate (may be greater than {@code v1} to flip)
   */
  public void texturedQuad(
      @NotNull Texture texture,
      float u0, float v0, float u1, float v1,
      float x0, float y0, float x1, float y1,
      float shearTop, float shearBottom,
      int colour)
  {
    if ((colour >>> 24) == 0 || x1 <= x0 || y1 <= y0) {
      return;
    }
    final float cr = (colour >> 16 & 0xFF) / 255f;
    final float cg = (colour >> 8 & 0xFF) / 255f;
    final float cb = (colour & 0xFF) / 255f;
    final float ca = (colour >>> 24) / 255f;

    final int minU = (int) Math.floor(Math.min(u0, u1));
    final int maxU = (int) Math.ceil(Math.max(u0, u1)) - 1;
    final int minV = (int) Math.floor(Math.min(v0, v1));
    final int maxV = (int) Math.ceil(Math.max(v0, v1)) - 1;

    final int top = Math.max(0, this.firstRow(y0));
    final int bottom = Math.min(this.height, this.firstRow(y1));
    for (int py = top; py < bottom; py++) {
      final float t = (this.centreY(py) - y0) / (y1 - y0);
      final float shift = shearTop + (shearBottom - shearTop) * t;
      final float left = x0 + shift;
      final float right = x1 + shift;
      final int texY = clamp((int) Math.floor(v0 + (v1 - v0) * t), minV, maxV);
      if (texY < 0 || texY >= texture.height()) {
        continue;
      }

      final int startX = Math.max(0, this.firstColumn(left));
      final int endX = Math.min(this.width, this.firstColumn(right));
      for (int px = startX; px < endX; px++) {
        final float s = (this.centreX(px) - left) / (right - left);
        final int texX = clamp((int) Math.floor(u0 + (u1 - u0) * s), minU, maxU);
        if (texX < 0 || texX >= texture.width()) {
          continue;
        }
        final int texel = texture.get(texX, texY);
        final int ta = texel >>> 24;
        if (ta == 0) {
          continue;
        }
        this.blend(py * this.width + px,
            (texel >> 16 & 0xFF) / 255f * cr,
            (texel >> 8 & 0xFF) / 255f * cg,
            (texel & 0xFF) / 255f * cb,
            ta / 255f * ca);
      }
    }
  }

  /**
   * Draws the texture region {@code (u, v, regionWidth, regionHeight)} stretched over the GUI rectangle.
   */
  public void blit(@NotNull Texture texture, float u, float v, float regionWidth, float regionHeight,
                   float x, float y, float w, float h, int colour)
  {
    this.texturedQuad(texture, u, v, u + regionWidth, v + regionHeight, x, y, x + w, y + h, 0, 0, colour);
  }

  public void blitSprite(@NotNull Sprite sprite, int x, int y, int w, int h) {
    if (w <= 0 || h <= 0) {
      return;
    }
    final Texture texture = sprite.texture();
    switch (sprite.scaling()) {
      case SpriteScaling.Stretch ignored -> this.blit(texture, 0, 0, texture.width(), texture.height(), x, y, w, h, -1);
      case SpriteScaling.Tile tile -> this.blitTiled(texture, tile.width(), tile.height(), x, y, w, h,
          0, 0, tile.width(), tile.height());
      case SpriteScaling.NineSlice slice -> this.blitNineSliced(texture, slice, x, y, w, h);
    }
  }

  /**
   * Blits a {@code w x h} region of a sprite 1:1. {@code u}/{@code v} are in the sprite's logical units.
   */
  private void blitRegion(Texture texture, int logicalW, int logicalH, int u, int v, int x, int y, int w, int h) {
    if (w <= 0 || h <= 0) {
      return;
    }
    final float sx = (float) texture.width() / logicalW;
    final float sy = (float) texture.height() / logicalH;
    this.blit(texture, u * sx, v * sy, w * sx, h * sy, x, y, w, h, -1);
  }

  private void blitTiled(Texture texture, int logicalW, int logicalH, int x, int y, int w, int h,
                         int u, int v, int tileW, int tileH)
  {
    if (w <= 0 || h <= 0 || tileW <= 0 || tileH <= 0) {
      return;
    }
    for (int i = 0; i < w; i += tileW) {
      final int partW = Math.min(tileW, w - i);
      for (int j = 0; j < h; j += tileH) {
        final int partH = Math.min(tileH, h - j);
        this.blitRegion(texture, logicalW, logicalH, u, v, x + i, y + j, partW, partH);
      }
    }
  }

  private void blitInnerSegment(Texture texture, SpriteScaling.NineSlice slice, int x, int y, int w, int h,
                                int u, int v, int regionW, int regionH)
  {
    if (w <= 0 || h <= 0) {
      return;
    }
    if (slice.stretchInner()) {
      final float sx = (float) texture.width() / slice.width();
      final float sy = (float) texture.height() / slice.height();
      this.blit(texture, u * sx, v * sy, regionW * sx, regionH * sy, x, y, w, h, -1);
    }
    else {
      this.blitTiled(texture, slice.width(), slice.height(), x, y, w, h, u, v, regionW, regionH);
    }
  }

  private void blitNineSliced(Texture texture, SpriteScaling.NineSlice slice, int x, int y, int w, int h) {
    final int sw = slice.width();
    final int sh = slice.height();
    final int left = Math.min(slice.left(), w / 2);
    final int right = Math.min(slice.right(), w / 2);
    final int top = Math.min(slice.top(), h / 2);
    final int bottom = Math.min(slice.bottom(), h / 2);

    if (w == sw && h == sh) {
      this.blitRegion(texture, sw, sh, 0, 0, x, y, w, h);
    }
    else if (h == sh) {
      this.blitRegion(texture, sw, sh, 0, 0, x, y, left, h);
      this.blitInnerSegment(texture, slice, x + left, y, w - right - left, h, left, 0, sw - right - left, sh);
      this.blitRegion(texture, sw, sh, sw - right, 0, x + w - right, y, right, h);
    }
    else if (w == sw) {
      this.blitRegion(texture, sw, sh, 0, 0, x, y, w, top);
      this.blitInnerSegment(texture, slice, x, y + top, w, h - bottom - top, 0, top, sw, sh - bottom - top);
      this.blitRegion(texture, sw, sh, 0, sh - bottom, x, y + h - bottom, w, bottom);
    }
    else {
      final int innerW = w - right - left;
      final int innerH = h - bottom - top;
      final int regionW = sw - right - left;
      final int regionH = sh - bottom - top;
      this.blitRegion(texture, sw, sh, 0, 0, x, y, left, top);
      this.blitInnerSegment(texture, slice, x + left, y, innerW, top, left, 0, regionW, top);
      this.blitRegion(texture, sw, sh, sw - right, 0, x + w - right, y, right, top);
      this.blitRegion(texture, sw, sh, 0, sh - bottom, x, y + h - bottom, left, bottom);
      this.blitInnerSegment(texture, slice, x + left, y + h - bottom, innerW, bottom, left, sh - bottom, regionW, bottom);
      this.blitRegion(texture, sw, sh, sw - right, sh - bottom, x + w - right, y + h - bottom, right, bottom);
      this.blitInnerSegment(texture, slice, x, y + top, left, innerH, 0, top, left, regionH);
      this.blitInnerSegment(texture, slice, x + left, y + top, innerW, innerH, left, top, regionW, regionH);
      this.blitInnerSegment(texture, slice, x + w - right, y + top, right, innerH, sw - right, top, right, regionH);
    }
  }

  private void blend(int index, float r, float g, float b, float a) {
    if (a <= 0f) {
      return;
    }
    final int dst = this.pixels[index];
    final float da = (dst >>> 24) / 255f;
    final float remaining = da * (1f - a);
    final float outA = a + remaining;
    final float outR = (r * a + (dst >> 16 & 0xFF) / 255f * remaining) / outA;
    final float outG = (g * a + (dst >> 8 & 0xFF) / 255f * remaining) / outA;
    final float outB = (b * a + (dst & 0xFF) / 255f * remaining) / outA;
    this.pixels[index] = channel(outA) << 24 | channel(outR) << 16 | channel(outG) << 8 | channel(outB);
  }

  private static int channel(float value) {
    return clamp(Math.round(value * 255f), 0, 255);
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  public @NotNull BufferedImage toImage() {
    final BufferedImage image = new BufferedImage(Math.max(1, this.width), Math.max(1, this.height),
        BufferedImage.TYPE_INT_ARGB);
    if (this.width > 0 && this.height > 0) {
      image.setRGB(0, 0, this.width, this.height, this.pixels, 0, this.width);
    }
    return image;
  }
}
