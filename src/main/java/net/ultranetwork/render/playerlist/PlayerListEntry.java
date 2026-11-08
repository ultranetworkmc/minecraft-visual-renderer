package net.ultranetwork.render.playerlist;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One player in the tab list, mirroring the client's {@code PlayerInfo}.
 *
 * @param displayName shown name (vanilla: the tab list display name, or the team-formatted name)
 * @param name        profile name, used for sorting and skin lookups
 * @param uuid        profile id, used for skin lookups and picking the default skin
 * @param latency     ping in milliseconds, negative for unknown
 * @param spectator   spectators are listed last, italic and translucent
 * @param order       tab list order; higher values are listed first
 * @param team        scoreboard team name, used as a sort key
 * @param score       objective score shown on the right, or {@code null}
 * @param showHat     whether the skin's hat layer is drawn
 * @param skinUrl     skin texture URL (e.g. from the player's profile); avoids a Mojang API lookup
 */
public record PlayerListEntry(
    @NotNull Component displayName,
    @NotNull String name,
    @Nullable UUID uuid,
    int latency,
    boolean spectator,
    int order,
    @Nullable String team,
    @Nullable Component score,
    boolean showHat,
    @Nullable URI skinUrl
) {

  public PlayerListEntry {
    Objects.requireNonNull(displayName, "displayName");
    Objects.requireNonNull(name, "name");
  }

  public static @NotNull PlayerListEntry of(@NotNull String name) {
    return builder(name).build();
  }

  public static @NotNull Builder builder(@NotNull String name) {
    return new Builder(name);
  }

  public @NotNull Builder toBuilder() {
    return new Builder(this.name)
        .displayName(this.displayName)
        .uuid(this.uuid)
        .latency(this.latency)
        .spectator(this.spectator)
        .order(this.order)
        .team(this.team)
        .score(this.score)
        .showHat(this.showHat)
        .skinUrl(this.skinUrl);
  }

  public static final class Builder {
    private final String name;
    private @Nullable Component displayName;
    private @Nullable UUID uuid;
    private int latency;
    private boolean spectator;
    private int order;
    private @Nullable String team;
    private @Nullable Component score;
    private boolean showHat = true;
    private @Nullable URI skinUrl;

    private Builder(@NotNull String name) {
      this.name = Objects.requireNonNull(name, "name");
    }

    public @NotNull Builder displayName(@Nullable Component displayName) {
      this.displayName = displayName;
      return this;
    }

    public @NotNull Builder uuid(@Nullable UUID uuid) {
      this.uuid = uuid;
      return this;
    }

    public @NotNull Builder latency(int latency) {
      this.latency = latency;
      return this;
    }

    public @NotNull Builder spectator(boolean spectator) {
      this.spectator = spectator;
      return this;
    }

    public @NotNull Builder order(int order) {
      this.order = order;
      return this;
    }

    public @NotNull Builder team(@Nullable String team) {
      this.team = team;
      return this;
    }

    /**
     * Shows an integer objective score, formatted like vanilla's default (yellow).
     */
    public @NotNull Builder score(int score) {
      return this.score(Component.text(score, NamedTextColor.YELLOW));
    }

    public @NotNull Builder score(@Nullable Component score) {
      this.score = score;
      return this;
    }

    public @NotNull Builder showHat(boolean showHat) {
      this.showHat = showHat;
      return this;
    }

    public @NotNull Builder skinUrl(@Nullable URI skinUrl) {
      this.skinUrl = skinUrl;
      return this;
    }

    public @NotNull PlayerListEntry build() {
      return new PlayerListEntry(
          this.displayName != null ? this.displayName : Component.text(this.name),
          this.name, this.uuid, this.latency, this.spectator, this.order, this.team, this.score, this.showHat,
          this.skinUrl);
    }
  }
}
