package net.ultranetwork.render.graphics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * How a GUI sprite is scaled to the requested size, mirroring the {@code gui.scaling} section of a sprite's
 * {@code .mcmeta} file.
 */
public sealed interface SpriteScaling {

  Stretch STRETCH = new Stretch();

  record Stretch() implements SpriteScaling {
  }

  record Tile(int width, int height) implements SpriteScaling {
  }

  record NineSlice(int width, int height, int left, int top, int right, int bottom, boolean stretchInner)
      implements SpriteScaling {
  }

  /**
   * @param mcmeta the parsed {@code .mcmeta} root, or {@code null} if the sprite has none
   */
  static @NotNull SpriteScaling parse(@Nullable JsonObject mcmeta) {
    if (mcmeta == null || !(mcmeta.get("gui") instanceof JsonObject gui)
        || !(gui.get("scaling") instanceof JsonObject scaling)) {
      return STRETCH;
    }

    final String type = scaling.has("type") ? scaling.get("type").getAsString() : "stretch";
    return switch (type) {
      case "tile" -> new Tile(scaling.get("width").getAsInt(), scaling.get("height").getAsInt());
      case "nine_slice" -> {
        final JsonElement border = scaling.get("border");
        final int left;
        final int top;
        final int right;
        final int bottom;
        if (border instanceof JsonObject sides) {
          left = sides.get("left").getAsInt();
          top = sides.get("top").getAsInt();
          right = sides.get("right").getAsInt();
          bottom = sides.get("bottom").getAsInt();
        }
        else {
          left = top = right = bottom = border.getAsInt();
        }
        final boolean stretchInner = scaling.has("stretch_inner") && scaling.get("stretch_inner").getAsBoolean();
        yield new NineSlice(scaling.get("width").getAsInt(), scaling.get("height").getAsInt(),
            left, top, right, bottom, stretchInner);
      }
      default -> STRETCH;
    };
  }
}
