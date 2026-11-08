package net.ultranetwork.render.graphics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.ultranetwork.render.assets.MinecraftAssets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class AssetSpriteSource implements SpriteSource {
  private final MinecraftAssets assets;
  private final Map<Key, Optional<Sprite>> sprites = new ConcurrentHashMap<>();
  private final Map<Key, Optional<Texture>> textures = new ConcurrentHashMap<>();

  AssetSpriteSource(@NotNull MinecraftAssets assets) {
    this.assets = assets;
  }

  @Override
  public @Nullable Sprite sprite(@NotNull Key id) {
    return this.sprites.computeIfAbsent(id, key -> {
      final Texture texture = this.texture(Key.key(key.namespace(), "gui/sprites/" + key.value()));
      if (texture == null) {
        return Optional.empty();
      }
      return Optional.of(new Sprite(texture, SpriteScaling.parse(this.readMcmeta(key))));
    }).orElse(null);
  }

  @Override
  public @Nullable Texture texture(@NotNull Key id) {
    return this.textures.computeIfAbsent(id, key -> {
      try (InputStream in = this.assets.open(path(key, ".png"))) {
        return in == null ? Optional.empty() : Optional.of(Texture.read(in));
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }).orElse(null);
  }

  private @Nullable JsonObject readMcmeta(Key sprite) {
    final Key texture = Key.key(sprite.namespace(), "gui/sprites/" + sprite.value());
    try (InputStream in = this.assets.open(path(texture, ".png.mcmeta"))) {
      if (in == null) {
        return null;
      }
      return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String path(Key key, String suffix) {
    return "assets/" + key.namespace() + "/textures/" + key.value() + suffix;
  }
}
