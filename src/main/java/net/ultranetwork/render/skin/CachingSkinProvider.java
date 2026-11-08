package net.ultranetwork.render.skin;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.ultranetwork.render.playerlist.PlayerListEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Caches skins per player and shares in-flight requests, so rendering the same tab list repeatedly does not hit
 * Mojang's rate limits. Failed lookups are retried after a minute.
 */
final class CachingSkinProvider implements SkinProvider {
  private static final long FAILURE_TTL_NANOS = Duration.ofMinutes(1).toNanos();
  private static final int MAX_ENTRIES = 2048;

  private record Cached(CompletableFuture<BufferedImage> skin, long createdAt) {
  }

  private final SkinProvider delegate;
  private final long ttlNanos;
  private final Map<String, Cached> cache = new ConcurrentHashMap<>();

  CachingSkinProvider(@NotNull SkinProvider delegate, @NotNull Duration ttl) {
    this.delegate = delegate;
    this.ttlNanos = ttl.toNanos();
  }

  @Override
  public @NotNull CompletableFuture<BufferedImage> fetchSkin(@NotNull PlayerListEntry entry) {
    final String key = key(entry);
    final long now = System.nanoTime();
    final Cached cached = this.cache.compute(key, (k, existing) -> {
      if (existing != null && !this.expired(existing, now)) {
        return existing;
      }
      return new Cached(this.delegate.fetchSkin(entry), now);
    });
    if (this.cache.size() > MAX_ENTRIES) {
      this.cache.values().removeIf(value -> this.expired(value, now));
    }
    return cached.skin();
  }

  private boolean expired(Cached cached, long now) {
    final CompletableFuture<BufferedImage> skin = cached.skin();
    final boolean failed = skin.isCompletedExceptionally() || (skin.isDone() && skin.getNow(null) == null);
    return now - cached.createdAt() > (failed ? FAILURE_TTL_NANOS : this.ttlNanos);
  }

  private static String key(PlayerListEntry entry) {
    if (entry.skinUrl() != null) {
      return "url:" + entry.skinUrl();
    }
    if (entry.uuid() != null) {
      return "uuid:" + entry.uuid();
    }
    return "name:" + entry.name().toLowerCase(Locale.ROOT);
  }
}
