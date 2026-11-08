package net.ultranetwork.render.assets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ZipAssets implements MinecraftAssets {
  private final ZipFile zip;

  ZipAssets(@NotNull Path file) throws IOException {
    this.zip = new ZipFile(file.toFile());
  }

  @Override
  public @Nullable InputStream open(@NotNull String path) throws IOException {
    final ZipEntry entry = this.zip.getEntry(path);
    return entry == null || entry.isDirectory() ? null : this.zip.getInputStream(entry);
  }

  @Override
  public void close() throws IOException {
    this.zip.close();
  }
}
