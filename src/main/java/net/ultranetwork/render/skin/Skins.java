package net.ultranetwork.render.skin;

import java.awt.image.BufferedImage;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.ultranetwork.render.graphics.Texture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Skin texture helpers following the client's skin processing.
 */
public final class Skins {
  private static final String[] DEFAULT_SKINS = {
      "slim/alex", "slim/ari", "slim/efe", "slim/kai", "slim/makena", "slim/noor", "slim/steve", "slim/sunny",
      "slim/zuri",
      "wide/alex", "wide/ari", "wide/efe", "wide/kai", "wide/makena", "wide/noor", "wide/steve", "wide/sunny",
      "wide/zuri"
  };
  private static final int FALLBACK_DEFAULT_SKIN = 6;

  private Skins() {
  }

  /**
   * Normalises a downloaded skin like the client does: legacy 64x32 skins are expanded to 64x64 and the opaque
   * regions are forced opaque.
   *
   * @return the processed skin, or {@code null} if the image is not a valid skin
   */
  public static @Nullable Texture process(@Nullable BufferedImage image) {
    if (image == null || image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32)) {
      return null;
    }
    final boolean legacy = image.getHeight() == 32;
    final int[] argb = new int[64 * 64];
    image.getRGB(0, 0, 64, image.getHeight(), argb, 0, 64);

    if (legacy) {
      notchTransparencyHack(argb, 32, 0, 64, 32);
    }
    setNoAlpha(argb, 0, 0, 32, 16);
    setNoAlpha(argb, 0, 16, 64, 32);
    setNoAlpha(argb, 16, 48, 48, 64);
    return new Texture(64, 64, argb);
  }

  /**
   * @return the texture id of the default skin vanilla assigns to this profile
   */
  public static @NotNull Key defaultSkin(@Nullable UUID uuid) {
    final int index = uuid == null ? FALLBACK_DEFAULT_SKIN : Math.floorMod(uuid.hashCode(), DEFAULT_SKINS.length);
    return Key.key("entity/player/" + DEFAULT_SKINS[index]);
  }

  private static void notchTransparencyHack(int[] argb, int x0, int y0, int x1, int y1) {
    for (int y = y0; y < y1; y++) {
      for (int x = x0; x < x1; x++) {
        if ((argb[y * 64 + x] >>> 24) < 128) {
          return;
        }
      }
    }
    for (int y = y0; y < y1; y++) {
      for (int x = x0; x < x1; x++) {
        argb[y * 64 + x] &= 0x00FFFFFF;
      }
    }
  }

  private static void setNoAlpha(int[] argb, int x0, int y0, int x1, int y1) {
    for (int y = y0; y < y1; y++) {
      for (int x = x0; x < x1; x++) {
        argb[y * 64 + x] |= 0xFF000000;
      }
    }
  }
}
