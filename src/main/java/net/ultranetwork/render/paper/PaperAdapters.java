package net.ultranetwork.render.paper;

import com.destroystokyo.paper.ClientOption;
import com.destroystokyo.paper.profile.PlayerProfile;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import net.ultranetwork.render.playerlist.PlayerListEntry;
import net.ultranetwork.render.playerlist.PlayerListRenderer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds render inputs from live Paper objects. Call from the server thread.
 */
public final class PaperAdapters {

  private PaperAdapters() {
  }

  public static @NotNull PlayerListEntry entry(@NotNull Player player) {
    final Team team = player.getScoreboard().getEntityTeam(player);
    return PlayerListEntry.builder(player.getName())
        .displayName(player.playerListName())
        .uuid(player.getUniqueId())
        .latency(player.getPing())
        .spectator(player.getGameMode() == GameMode.SPECTATOR)
        .order(player.getPlayerListOrder())
        .team(team != null ? team.getName() : null)
        .showHat(player.getClientOption(ClientOption.SKIN_PARTS).hasHatsEnabled())
        .skinUrl(skinUrl(player.getPlayerProfile()))
        .build();
  }

  /**
   * The tab list as {@code viewer} sees it: their header and footer and every online player they can see, with
   * heads shown as on an online-mode server.
   */
  public static @NotNull PlayerListRenderer.Builder tabList(@NotNull Player viewer) {
    final List<PlayerListEntry> entries = new ArrayList<>();
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.equals(viewer) || viewer.canSee(player)) {
        entries.add(entry(player));
      }
    }
    return PlayerListRenderer.builder()
        .header(viewer.playerListHeader())
        .footer(viewer.playerListFooter())
        .entries(entries)
        .showHeads(Bukkit.getOnlineMode());
  }

  private static @Nullable URI skinUrl(@NotNull PlayerProfile profile) {
    final URL url = profile.getTextures().getSkin();
    if (url == null) {
      return null;
    }
    try {
      return url.toURI();
    }
    catch (URISyntaxException e) {
      return null;
    }
  }
}
