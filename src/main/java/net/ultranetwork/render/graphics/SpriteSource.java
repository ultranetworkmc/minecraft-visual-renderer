package net.ultranetwork.render.graphics;

import net.kyori.adventure.key.Key;
import net.ultranetwork.render.assets.MinecraftAssets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Supplies GUI sprites and raw textures.
 */
public interface SpriteSource {

  /**
   * @param id a GUI sprite id such as {@code minecraft:tooltip/background}
   */
  @Nullable Sprite sprite(@NotNull Key id);

  /**
   * @param id a texture id relative to {@code textures/}, such as {@code minecraft:entity/player/wide/steve}
   */
  @Nullable Texture texture(@NotNull Key id);

  static @NotNull SpriteSource assets(@NotNull MinecraftAssets assets) {
    return new AssetSpriteSource(assets);
  }

  /**
   * Procedurally generated tooltip and ping sprites, used when no vanilla assets are configured.
   */
  static @NotNull SpriteSource fallback() {
    return FallbackSprites.INSTANCE;
  }
}
