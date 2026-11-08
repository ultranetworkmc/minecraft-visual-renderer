package net.ultranetwork.render.skin;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import net.ultranetwork.render.playerlist.PlayerListEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Fetches player skin textures (the full 64x64 or legacy 64x32 skin image, not a pre-rendered head).
 */
@FunctionalInterface
public interface SkinProvider {

  /**
   * @return the skin image, or a future completing with {@code null} if the player has no skin or it could not be
   * fetched (the default skin is used instead)
   */
  @NotNull CompletableFuture<BufferedImage> fetchSkin(@NotNull PlayerListEntry entry);

  /**
   * Uses the entry's skin URL when present, otherwise Mojang's session server (by UUID, or by name). Results are
   * cached for 30 minutes.
   */
  static @NotNull SkinProvider mojang() {
    return MojangSkinProvider.SHARED;
  }

  static @NotNull SkinProvider cached(@NotNull SkinProvider delegate, @NotNull Duration ttl) {
    return new CachingSkinProvider(delegate, ttl);
  }

  /**
   * Never fetches anything, so every player gets their default skin.
   */
  static @NotNull SkinProvider none() {
    return entry -> CompletableFuture.completedFuture(null);
  }
}
