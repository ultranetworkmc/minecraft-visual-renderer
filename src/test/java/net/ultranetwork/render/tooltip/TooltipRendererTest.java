package net.ultranetwork.render.tooltip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import java.awt.image.BufferedImage;
import net.kyori.adventure.text.Component;
import net.ultranetwork.render.RenderEnvironment;
import org.junit.jupiter.api.Test;

class TooltipRendererTest {

  private static TooltipRenderer.Builder tooltip() {
    return TooltipRenderer.builder().environment(RenderEnvironment.fallback()).scale(1);
  }

  @Test
  void singleLineSize() {
    // "Hi" is 8px wide and a single line is 8px tall; the visible box adds 4px on each side.
    final BufferedImage image = tooltip().line(Component.text("Hi")).render();
    assertEquals(16, image.getWidth());
    assertEquals(16, image.getHeight());
  }

  @Test
  void multiLineSizeIncludesFirstLineGap() {
    final BufferedImage image = tooltip().line(Component.text("Hi")).line(Component.text("Hi")).render();
    assertEquals(16, image.getWidth());
    assertEquals(20 + 8, image.getHeight());
  }

  @Test
  void scaleMultipliesSize() {
    final BufferedImage image = tooltip().scale(3).line(Component.text("Hi")).render();
    assertEquals(48, image.getWidth());
    assertEquals(48, image.getHeight());
  }

  @Test
  void backgroundAndFrameMatchVanillaLayout() {
    final BufferedImage image = tooltip().line(Component.text("Hi")).render();
    assertEquals(0, image.getRGB(0, 0) >>> 24, "outer corner is cut");
    assertEquals(0xF0100010, image.getRGB(1, 1), "inner corner is plain background");
    assertEquals(0xF0100010, image.getRGB(0, 4), "left edge is background");
    // The frame starts one pixel in from the corner on the top edge.
    final int frame = image.getRGB(2, 1);
    assertEquals(0xF5, frame >>> 24);
    assertEquals(0, image.getRGB(15, 0) >>> 24);
  }

  @Test
  void backdropFillsBehindTranslucentPixels() {
    final BufferedImage image = tooltip().backdrop(Color.WHITE).line(Component.text("Hi")).render();
    assertEquals(0xFFFFFFFF, image.getRGB(0, 0));
    assertEquals(0xFF, image.getRGB(1, 1) >>> 24);
  }

  @Test
  void requiresALine() {
    assertThrows(IllegalStateException.class, () -> tooltip().render());
  }

  @Test
  void builderIsReusable() {
    final TooltipRenderer.Builder builder = tooltip().line(Component.text("Hi"));
    final BufferedImage first = builder.build().join();
    final BufferedImage second = builder.build().join();
    assertEquals(first.getHeight(), second.getHeight());
  }
}
