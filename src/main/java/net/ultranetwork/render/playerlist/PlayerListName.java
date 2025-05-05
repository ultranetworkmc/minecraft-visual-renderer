package net.ultranetwork.render.playerlist;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @param listNameComponent The formatted Component for the player's name/prefix
 * @param ping              The player's ping (-1 for unknown)
 * @param plainListName     The player's unformatted name (for sorting)
 * @param priority          Sorting priority (higher values appear first)
 */
public record PlayerListName(
    @NotNull Component listNameComponent,
    int ping,
    @NotNull String plainListName,
    int priority
) {

  public static PlayerListName of(
      @NotNull String plainName,
      int priority
  ) {
    return new PlayerListName(Component.text(plainName), -1, plainName, priority);
  }

  public static PlayerListName of(
      @NotNull Component listNameComponent,
      int ping,
      @NotNull String plainName,
      int priority
  ) {
    return new PlayerListName(listNameComponent, ping, plainName, priority);
  }
}