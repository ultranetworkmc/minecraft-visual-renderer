package net.ultranetwork.render.graphics;

import java.util.Map;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Code-generated versions of the GUI sprites this library needs. The tooltip sprites match vanilla's layout and
 * colours (the vanilla frame gradient is hand-dithered, so it differs by a few colour units); the signal bars match
 * vanilla exactly.
 */
final class FallbackSprites implements SpriteSource {
  static final FallbackSprites INSTANCE = new FallbackSprites();

  private static final int BACKGROUND = 0xF0100010;
  private static final int FRAME_TOP = 0x505000FF;
  private static final int FRAME_BOTTOM = 0x5028007F;
  private static final int BAR = 0xFF00FF21;
  private static final int BAR_SHADOW = 0xFF00870F;
  private static final int EMPTY_BAR = 0xFF5B5B5B;
  private static final int EMPTY_BAR_SHADOW = 0xFF383838;
  private static final int CROSS = 0xFFFF0000;
  private static final int CROSS_SHADOW = 0xFF820000;

  private final Map<Key, Sprite> sprites;

  private FallbackSprites() {
    this.sprites = Map.of(
        Key.key("tooltip/background"), new Sprite(tooltipBackground(), new SpriteScaling.NineSlice(100, 100, 9, 9, 9, 9, false)),
        Key.key("tooltip/frame"), new Sprite(tooltipFrame(), new SpriteScaling.NineSlice(100, 100, 10, 10, 10, 10, true)),
        Key.key("icon/ping_unknown"), new Sprite(pingUnknown(), SpriteScaling.STRETCH),
        Key.key("icon/ping_1"), new Sprite(ping(1), SpriteScaling.STRETCH),
        Key.key("icon/ping_2"), new Sprite(ping(2), SpriteScaling.STRETCH),
        Key.key("icon/ping_3"), new Sprite(ping(3), SpriteScaling.STRETCH),
        Key.key("icon/ping_4"), new Sprite(ping(4), SpriteScaling.STRETCH),
        Key.key("icon/ping_5"), new Sprite(ping(5), SpriteScaling.STRETCH)
    );
  }

  @Override
  public @Nullable Sprite sprite(@NotNull Key id) {
    final Sprite sprite = this.sprites.get(id);
    if (sprite == null && id.value().startsWith("tooltip/")) {
      return this.sprites.get(Key.key(id.value().endsWith("_frame") ? "tooltip/frame" : "tooltip/background"));
    }
    return sprite;
  }

  @Override
  public @Nullable Texture texture(@NotNull Key id) {
    return null;
  }

  private static Texture tooltipBackground() {
    final int[] argb = new int[100 * 100];
    for (int y = 8; y <= 91; y++) {
      final boolean edgeRow = y == 8 || y == 91;
      for (int x = edgeRow ? 9 : 8; x <= (edgeRow ? 90 : 91); x++) {
        argb[y * 100 + x] = BACKGROUND;
      }
    }
    return new Texture(100, 100, argb);
  }

  private static Texture tooltipFrame() {
    final int[] argb = new int[100 * 100];
    for (int x = 10; x <= 89; x++) {
      argb[9 * 100 + x] = FRAME_TOP;
      argb[90 * 100 + x] = FRAME_BOTTOM;
    }
    for (int y = 10; y <= 89; y++) {
      final int colour = lerp((y - 10 + 0.5f) / 80f, FRAME_TOP, FRAME_BOTTOM);
      argb[y * 100 + 9] = colour;
      argb[y * 100 + 90] = colour;
    }
    return new Texture(100, 100, argb);
  }

  private static int lerp(float t, int from, int to) {
    int result = 0;
    for (int shift = 0; shift <= 24; shift += 8) {
      final int a = from >>> shift & 0xFF;
      final int b = to >>> shift & 0xFF;
      result |= Math.round(a + (b - a) * t) << shift;
    }
    return result;
  }

  private static Texture ping(int level) {
    final int[] argb = new int[10 * 8];
    for (int bar = 0; bar < 5; bar++) {
      final boolean active = bar < level;
      final int top = 5 - bar;
      for (int y = top; y <= 6; y++) {
        argb[y * 10 + bar * 2] = active ? BAR : EMPTY_BAR;
      }
      for (int y = top + 1; y <= 7; y++) {
        argb[y * 10 + bar * 2 + 1] = active ? BAR_SHADOW : EMPTY_BAR_SHADOW;
      }
    }
    return new Texture(10, 8, argb);
  }

  private static Texture pingUnknown() {
    final int[] argb = ping(0).argb().clone();
    for (int i = 1; i <= 7; i++) {
      cross(argb, i, i);
      cross(argb, 8 - i, i);
    }
    return new Texture(10, 8, argb);
  }

  private static void cross(int[] argb, int x, int y) {
    argb[y * 10 + x] = CROSS;
    if (x + 1 < 10) {
      argb[y * 10 + x + 1] = CROSS_SHADOW;
    }
  }
}
