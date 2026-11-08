package net.ultranetwork.render.graphics;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.jetbrains.annotations.NotNull;

/**
 * An immutable ARGB (non-premultiplied) pixel buffer.
 */
public record Texture(int width, int height, int @NotNull [] argb) {

  public Texture {
    if (width < 0 || height < 0 || argb.length != width * height) {
      throw new IllegalArgumentException("Invalid texture size " + width + "x" + height);
    }
  }

  public static @NotNull Texture of(@NotNull BufferedImage image) {
    final int w = image.getWidth();
    final int h = image.getHeight();
    return new Texture(w, h, image.getRGB(0, 0, w, h, null, 0, w));
  }

  public static @NotNull Texture read(@NotNull InputStream in) throws IOException {
    final BufferedImage image = ImageIO.read(in);
    if (image == null) {
      throw new IOException("Unsupported image format");
    }
    return of(image);
  }

  public int get(int x, int y) {
    return this.argb[y * this.width + x];
  }

  public @NotNull BufferedImage toImage() {
    final BufferedImage image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
    image.setRGB(0, 0, this.width, this.height, this.argb, 0, this.width);
    return image;
  }
}
