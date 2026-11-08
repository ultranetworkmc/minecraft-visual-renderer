package net.ultranetwork.render.text;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Fonts {

  /**
   * @param id the style's font, or {@code null} for {@code minecraft:default}
   */
  @NotNull MinecraftFont font(@Nullable Key id);
}
