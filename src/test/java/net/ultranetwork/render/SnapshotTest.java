package net.ultranetwork.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.ultranetwork.render.util.ImageUtil;
import org.junit.jupiter.api.Test;

/**
 * Compares renders against committed golden images. Regenerate with {@code -Pmvr.updateGolden=true} after an
 * intentional rendering change, and review the diff.
 */
class SnapshotTest {
  private static final Path GOLDEN = Path.of("src", "test", "resources", "golden");

  @Test
  void tooltip() throws IOException {
    check("tooltip", Samples.tooltip(RenderEnvironment.fallback()));
  }

  @Test
  void tabList() throws IOException {
    check("tab_list", Samples.tabList(RenderEnvironment.fallback()));
  }

  private static void check(String name, BufferedImage actual) throws IOException {
    Samples.write(name, actual);
    if (Boolean.getBoolean("mvr.updateGolden")) {
      ImageUtil.writePng(actual, GOLDEN.resolve(name + ".png"));
      return;
    }

    final BufferedImage expected;
    try (InputStream in = SnapshotTest.class.getResourceAsStream("/golden/" + name + ".png")) {
      assertNotNull(in, "Missing golden image " + name + "; run with -Pmvr.updateGolden=true");
      expected = ImageIO.read(in);
    }
    assertEquals(expected.getWidth(), actual.getWidth(), name + " width");
    assertEquals(expected.getHeight(), actual.getHeight(), name + " height");
    for (int y = 0; y < expected.getHeight(); y++) {
      for (int x = 0; x < expected.getWidth(); x++) {
        final int e = expected.getRGB(x, y);
        final int a = actual.getRGB(x, y);
        if (e != a) {
          throw new AssertionError(String.format("%s differs at (%d, %d): expected %08x but was %08x"
              + " (actual written to build/test-output)", name, x, y, e, a));
        }
      }
    }
  }
}
