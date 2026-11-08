package net.ultranetwork.render.assets;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A source of Minecraft client assets (fonts, GUI sprites, skins, language files), laid out like a resource pack:
 * paths look like {@code assets/minecraft/textures/font/ascii.png}.
 *
 * <p>Providing the vanilla assets is what makes renders pixel-identical to the game. Use
 * {@link #download(String, Path)} to fetch them from Mojang, or point at a client jar / resource pack you already
 * have.</p>
 */
public interface MinecraftAssets extends Closeable {

  /**
   * @return a stream for the asset, or {@code null} if this source does not contain it
   */
  @Nullable InputStream open(@NotNull String path) throws IOException;

  @Override
  default void close() throws IOException {
  }

  /**
   * Assets inside a zip file, such as a Minecraft client jar or a zipped resource pack.
   */
  static @NotNull MinecraftAssets zip(@NotNull Path file) throws IOException {
    return new ZipAssets(file);
  }

  /**
   * Assets inside an unpacked resource pack directory.
   */
  static @NotNull MinecraftAssets directory(@NotNull Path root) {
    return new DirectoryAssets(root);
  }

  /**
   * Stacks sources like the resource pack list in game: earlier sources override later ones.
   */
  static @NotNull MinecraftAssets layered(@NotNull List<? extends MinecraftAssets> sources) {
    return new LayeredAssets(List.copyOf(sources));
  }

  static @NotNull MinecraftAssets layered(@NotNull MinecraftAssets... sources) {
    return layered(List.of(sources));
  }

  /**
   * Downloads (or reuses from {@code cacheDirectory}) the official client jar and the font files that are only
   * shipped through the asset index, for the given Minecraft version (e.g. {@code "26.3"}).
   *
   * <p>This blocks while downloading; call it off the main thread.</p>
   */
  static @NotNull MinecraftAssets download(@NotNull String version, @NotNull Path cacheDirectory) throws IOException {
    return AssetDownloader.download(version, cacheDirectory);
  }
}
