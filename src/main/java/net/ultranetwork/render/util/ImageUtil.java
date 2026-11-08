package net.ultranetwork.render.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ImageUtil {

  private ImageUtil() {
  }

  public static byte @NotNull [] toPng(@NotNull BufferedImage image) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      writePng(image, out);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return out.toByteArray();
  }

  public static void writePng(@NotNull BufferedImage image, @NotNull OutputStream out) throws IOException {
    if (!ImageIO.write(image, "png", out)) {
      throw new IOException("No PNG writer available");
    }
  }

  /**
   * Writes the image as a PNG, creating parent directories as needed.
   */
  public static void writePng(@NotNull BufferedImage image, @NotNull Path file) throws IOException {
    final Path parent = file.toAbsolutePath().getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    try (OutputStream out = Files.newOutputStream(file)) {
      writePng(image, out);
    }
  }

  /**
   * Crops away fully transparent rows and columns around the image's content.
   */
  public static @NotNull BufferedImage cropTransparent(@NotNull BufferedImage image) {
    int minX = image.getWidth();
    int minY = image.getHeight();
    int maxX = -1;
    int maxY = -1;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if ((image.getRGB(x, y) >>> 24) != 0) {
          minX = Math.min(minX, x);
          minY = Math.min(minY, y);
          maxX = Math.max(maxX, x);
          maxY = Math.max(maxY, y);
        }
      }
    }
    if (maxX < 0) {
      return image;
    }
    return copy(image.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1));
  }

  /**
   * @return the image composited over a solid backdrop, or the image itself when {@code backdrop} is {@code null}
   */
  public static @NotNull BufferedImage withBackdrop(@NotNull BufferedImage image, @Nullable Color backdrop) {
    if (backdrop == null) {
      return image;
    }
    final BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
    final Graphics2D graphics = result.createGraphics();
    try {
      graphics.setColor(backdrop);
      graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
      graphics.drawImage(image, 0, 0, null);
    }
    finally {
      graphics.dispose();
    }
    return result;
  }

  private static BufferedImage copy(BufferedImage image) {
    final BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
    result.setRGB(0, 0, image.getWidth(), image.getHeight(),
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()), 0, image.getWidth());
    return result;
  }
}
