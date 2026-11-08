package net.ultranetwork.render.assets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

final class AssetDownloader {
  private static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
  private static final String RESOURCES_URL = "https://resources.download.minecraft.net/";
  private static final String INDEX_FONT_PREFIX = "minecraft/font/";
  private static final String COMPLETE_MARKER = ".complete";

  private static final HttpClient HTTP = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

  private AssetDownloader() {
  }

  static @NotNull MinecraftAssets download(@NotNull String version, @NotNull Path cacheDirectory) throws IOException {
    if (!version.matches("[A-Za-z0-9._-]+")) {
      throw new IllegalArgumentException("Invalid version: " + version);
    }
    final Path root = cacheDirectory.resolve(version);
    final Path clientJar = root.resolve("client.jar");
    final Path overlay = root.resolve("overlay");

    if (!Files.exists(root.resolve(COMPLETE_MARKER))) {
      Files.createDirectories(root);
      final JsonObject versionInfo = fetchVersion(version);

      final JsonObject client = versionInfo.getAsJsonObject("downloads").getAsJsonObject("client");
      downloadVerified(client.get("url").getAsString(), client.get("sha1").getAsString(), clientJar);

      final JsonObject indexInfo = versionInfo.getAsJsonObject("assetIndex");
      final JsonObject index = JsonParser.parseString(new String(
          fetch(indexInfo.get("url").getAsString(), indexInfo.get("sha1").getAsString()), StandardCharsets.UTF_8))
          .getAsJsonObject().getAsJsonObject("objects");
      for (Map.Entry<String, JsonElement> entry : index.entrySet()) {
        if (!entry.getKey().startsWith(INDEX_FONT_PREFIX)) {
          continue;
        }
        final String hash = entry.getValue().getAsJsonObject().get("hash").getAsString();
        final Path target = overlay.resolve("assets").resolve(entry.getKey()).normalize();
        if (!target.startsWith(overlay)) {
          throw new IOException("Unexpected asset path " + entry.getKey());
        }
        downloadVerified(RESOURCES_URL + hash.substring(0, 2) + "/" + hash, hash, target);
      }
      Files.writeString(root.resolve(COMPLETE_MARKER), version);
    }

    return MinecraftAssets.layered(MinecraftAssets.directory(overlay), MinecraftAssets.zip(clientJar));
  }

  private static JsonObject fetchVersion(String version) throws IOException {
    final JsonObject manifest = JsonParser.parseString(new String(fetch(MANIFEST_URL, null), StandardCharsets.UTF_8))
        .getAsJsonObject();
    for (JsonElement element : manifest.getAsJsonArray("versions")) {
      final JsonObject entry = element.getAsJsonObject();
      if (version.equals(entry.get("id").getAsString())) {
        return JsonParser.parseString(new String(
            fetch(entry.get("url").getAsString(), entry.get("sha1").getAsString()), StandardCharsets.UTF_8))
            .getAsJsonObject();
      }
    }
    throw new IOException("Unknown Minecraft version " + version);
  }

  private static void downloadVerified(String url, String sha1, Path target) throws IOException {
    if (Files.isRegularFile(target) && sha1.equalsIgnoreCase(sha1(Files.readAllBytes(target)))) {
      return;
    }
    final byte[] data = fetch(url, sha1);
    Files.createDirectories(target.getParent());
    final Path temp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".part");
    try {
      Files.write(temp, data);
      Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
    finally {
      Files.deleteIfExists(temp);
    }
  }

  private static byte[] fetch(String url, String expectedSha1) throws IOException {
    final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofMinutes(2))
        .header("User-Agent", "minecraft-visual-renderer")
        .GET()
        .build();
    final HttpResponse<InputStream> response;
    try {
      response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while downloading " + url, e);
    }

    try (InputStream body = response.body()) {
      if (response.statusCode() != 200) {
        throw new IOException("HTTP " + response.statusCode() + " for " + url);
      }
      final byte[] data = body.readAllBytes();
      if (expectedSha1 != null && !expectedSha1.equalsIgnoreCase(sha1(data))) {
        throw new IOException("Checksum mismatch for " + url);
      }
      return data;
    }
  }

  private static String sha1(byte[] data) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
    }
    catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
