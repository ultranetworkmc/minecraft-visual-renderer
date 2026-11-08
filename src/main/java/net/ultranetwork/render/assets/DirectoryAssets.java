package net.ultranetwork.render.assets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class DirectoryAssets implements MinecraftAssets {
  private final Path root;

  DirectoryAssets(@NotNull Path root) {
    this.root = root.toAbsolutePath().normalize();
  }

  @Override
  public @Nullable InputStream open(@NotNull String path) throws IOException {
    final Path file = this.root.resolve(path).normalize();
    if (!file.startsWith(this.root) || !Files.isRegularFile(file)) {
      return null;
    }
    return Files.newInputStream(file);
  }
}
