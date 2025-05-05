package net.ultranetwork.render.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;

public class HeadUtil {
  private static final int PLAYER_HEAD_SIZE = 16;

  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();


  /**
   * @param playerName The Minecraft username.
   * @return A CompletableFuture containing the BufferedImage, or null if fetch failed.
   */
  public static CompletableFuture<BufferedImage> fetchPlayerHead(String playerName) {
    final String urlString = "https://crafthead.net/helm/" + playerName + "/" + PLAYER_HEAD_SIZE + ".png";

    try {
      final HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(urlString))
          .timeout(Duration.ofSeconds(10))
          .GET()
          .build();

      return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
          .thenApply(response -> {
            if (response.statusCode() == 200) {
              try (InputStream is = response.body()) {
                return ImageIO.read(is);
              }
              catch (IOException e) {
                System.err.println("Failed to read image stream for player " + playerName + ": " + e.getMessage());
                return null;
              }
            }

            return null;
          })
          .exceptionally(ex -> {
            System.err.println("Exception fetching head for player " + playerName + ": " + ex.getMessage());
            return null;
          });
    }
    catch (IllegalArgumentException e) {
      System.err.println("Invalid url generated for player " + playerName + ": " + urlString);
      return CompletableFuture.completedFuture(null);
    }
  }
}
