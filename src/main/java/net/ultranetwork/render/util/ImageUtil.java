package net.ultranetwork.render.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImageUtil {

  public static byte @Nullable [] createImageBytes(
      @NotNull BufferedImage image
  ) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", baos);
      return baos.toByteArray();
    }
    catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static boolean saveImageToFile(
      @NotNull BufferedImage image,
      @NotNull File file
  ) {
    try {
      return ImageIO.write(image, "png", file); // returns false if no appropriate writer is found
    }
    catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }
}
