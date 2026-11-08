package net.ultranetwork.render.assets;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class LayeredAssets implements MinecraftAssets {
  private final List<MinecraftAssets> sources;

  LayeredAssets(@NotNull List<MinecraftAssets> sources) {
    this.sources = sources;
  }

  @Override
  public @Nullable InputStream open(@NotNull String path) throws IOException {
    for (MinecraftAssets source : this.sources) {
      final InputStream in = source.open(path);
      if (in != null) {
        return in;
      }
    }
    return null;
  }

  @Override
  public void close() throws IOException {
    IOException failure = null;
    for (MinecraftAssets source : this.sources) {
      try {
        source.close();
      }
      catch (IOException e) {
        if (failure == null) {
          failure = e;
        }
        else {
          failure.addSuppressed(e);
        }
      }
    }
    if (failure != null) {
      throw failure;
    }
  }
}
