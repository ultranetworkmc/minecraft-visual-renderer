package net.ultranetwork.render.skin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import net.ultranetwork.render.playerlist.PlayerListEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class MojangSkinProvider implements SkinProvider {
  static final SkinProvider SHARED = new CachingSkinProvider(new MojangSkinProvider(), Duration.ofMinutes(30));

  private static final System.Logger LOGGER = System.getLogger(MojangSkinProvider.class.getName());
  private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
  private static final String PROFILE_BY_NAME = "https://api.minecraftservices.com/minecraft/profile/lookup/name/";
  private static final String SESSION_PROFILE = "https://sessionserver.mojang.com/session/minecraft/profile/";

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

  @Override
  public @NotNull CompletableFuture<BufferedImage> fetchSkin(@NotNull PlayerListEntry entry) {
    final CompletableFuture<URI> skinUrl;
    if (entry.skinUrl() != null) {
      skinUrl = CompletableFuture.completedFuture(entry.skinUrl());
    }
    else if (entry.uuid() != null) {
      skinUrl = this.skinUrl(entry.uuid());
    }
    else if (VALID_NAME.matcher(entry.name()).matches()) {
      skinUrl = this.get(URI.create(PROFILE_BY_NAME + entry.name()))
          .thenCompose(body -> {
            if (body == null) {
              return CompletableFuture.completedFuture(null);
            }
            final String id = JsonParser.parseString(new String(body, StandardCharsets.UTF_8))
                .getAsJsonObject().get("id").getAsString();
            return this.skinUrl(parseUndashed(id));
          });
    }
    else {
      return CompletableFuture.completedFuture(null);
    }

    return skinUrl
        .thenCompose(url -> url == null ? CompletableFuture.completedFuture(null) : this.get(url))
        .thenApply(MojangSkinProvider::decode)
        .exceptionally(error -> {
          LOGGER.log(System.Logger.Level.DEBUG, "Could not fetch skin for " + entry.name(), error);
          return null;
        });
  }

  private CompletableFuture<URI> skinUrl(UUID uuid) {
    final String id = uuid.toString().replace("-", "");
    return this.get(URI.create(SESSION_PROFILE + id)).thenApply(body -> {
      if (body == null) {
        return null;
      }
      final JsonObject profile = JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
      for (JsonElement element : profile.getAsJsonArray("properties")) {
        final JsonObject property = element.getAsJsonObject();
        if (!"textures".equals(property.get("name").getAsString())) {
          continue;
        }
        final JsonObject textures = JsonParser.parseString(new String(
                Base64.getDecoder().decode(property.get("value").getAsString()), StandardCharsets.UTF_8))
            .getAsJsonObject().getAsJsonObject("textures");
        if (textures == null || !(textures.get("SKIN") instanceof JsonObject skin)) {
          return null;
        }
        return URI.create(skin.get("url").getAsString().replaceFirst("^http://", "https://"));
      }
      return null;
    });
  }

  private CompletableFuture<byte @Nullable []> get(URI uri) {
    final String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
    if (!scheme.equals("https") && !scheme.equals("http")) {
      return CompletableFuture.failedFuture(new IllegalArgumentException("Unsupported skin URL " + uri));
    }
    final HttpRequest request = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(10))
        .header("User-Agent", "minecraft-visual-renderer")
        .GET()
        .build();
    return this.http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
        .thenApply(response -> response.statusCode() == 200 ? response.body() : null);
  }

  private static @Nullable BufferedImage decode(byte @Nullable [] png) {
    if (png == null) {
      return null;
    }
    try {
      return ImageIO.read(new ByteArrayInputStream(png));
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static UUID parseUndashed(String id) {
    return UUID.fromString(id.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
        "$1-$2-$3-$4-$5"));
  }
}
