package net.ultranetwork.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * What is painted behind a render's translucent background, standing in for the game world.
 */
public sealed interface Backdrop {

  /**
   * Vanilla's default daytime sky: the plains sky colour fading to the horizon fog colour.
   */
  Backdrop SKY = gradient(new Color(0x78A7FF), new Color(0xC0D8FF));

  static @NotNull Backdrop solid(@NotNull Color color) {
    return new Solid(Objects.requireNonNull(color, "color"));
  }

  /**
   * A vertical gradient from {@code top} to {@code bottom}.
   */
  static @NotNull Backdrop gradient(@NotNull Color top, @NotNull Color bottom) {
    return new Gradient(Objects.requireNonNull(top, "top"), Objects.requireNonNull(bottom, "bottom"));
  }

  static @NotNull Backdrop sky() {
    return SKY;
  }

  void paint(@NotNull Graphics2D graphics, int width, int height);

  record Solid(@NotNull Color color) implements Backdrop {
    @Override
    public void paint(@NotNull Graphics2D graphics, int width, int height) {
      graphics.setColor(this.color);
      graphics.fillRect(0, 0, width, height);
    }
  }

  record Gradient(@NotNull Color top, @NotNull Color bottom) implements Backdrop {
    @Override
    public void paint(@NotNull Graphics2D graphics, int width, int height) {
      for (int y = 0; y < height; y++) {
        final float t = height <= 1 ? 0f : (float) y / (height - 1);
        graphics.setColor(new Color(
            lerp(this.top.getRed(), this.bottom.getRed(), t),
            lerp(this.top.getGreen(), this.bottom.getGreen(), t),
            lerp(this.top.getBlue(), this.bottom.getBlue(), t),
            lerp(this.top.getAlpha(), this.bottom.getAlpha(), t)));
        graphics.fillRect(0, y, width, 1);
      }
    }

    private static int lerp(int from, int to, float t) {
      return Math.round(from + (to - from) * t);
    }
  }
}
