package net.ultranetwork.render.graphics;

import org.jetbrains.annotations.NotNull;

public record Sprite(@NotNull Texture texture, @NotNull SpriteScaling scaling) {
}
