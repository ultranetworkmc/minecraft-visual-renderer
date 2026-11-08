package net.ultranetwork.render.playerlist;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.ultranetwork.render.RenderEnvironment;
import org.junit.jupiter.api.Test;

class PlayerListRendererTest {

  private static PlayerListRenderer.Builder tab() {
    return PlayerListRenderer.builder().environment(RenderEnvironment.fallback()).scale(1);
  }

  @Test
  void singlePlayerLayout() {
    // name 28px + 13px for the ping column = 41px column; the background adds 1px each side.
    final BufferedImage image = tab().entry(PlayerListEntry.of("Steve")).build().join();
    assertEquals(42, image.getWidth());
    assertEquals(10, image.getHeight());
    assertEquals(0x80000000, image.getRGB(0, 0), "black background row above the grid");
  }

  @Test
  void pingIconIsDrawnAtTheRightEdge() {
    final BufferedImage image = tab().entry(PlayerListEntry.builder("Steve").latency(20).build()).build().join();
    // Column spans x=1..41; the icon starts 11px before its end. The tallest bar is at icon (8, 1).
    final int iconLeft = 1 + 41 - 11;
    assertEquals(0x00FF21, image.getRGB(iconLeft + 8, 1 + 1) & 0xFFFFFF);
  }

  @Test
  void headerAndFooterAddRows() {
    final BufferedImage image = tab()
        .header(Component.text("Header"))
        .footer(Component.text("Footer\nSecond line"))
        .entry(PlayerListEntry.of("Steve"))
        .build().join();
    // 1 header line (9 + 1), 1 player row (9), 2 footer lines (1 + 18), plus the top border row.
    assertEquals(1 + 10 + 9 + 19, image.getHeight());
  }

  @Test
  void emptyHeaderIsIgnored() {
    final BufferedImage withEmpty = tab().header(Component.empty()).entry(PlayerListEntry.of("Steve")).build().join();
    assertEquals(10, withEmpty.getHeight());
  }

  @Test
  void capsAtEightyPlayersInFourColumns() {
    final List<PlayerListEntry> entries = new ArrayList<>();
    for (int i = 0; i < 85; i++) {
      entries.add(PlayerListEntry.of("P" + i));
    }
    final BufferedImage image = tab().entries(entries).build().join();
    assertEquals(1 + 20 * 9, image.getHeight());
  }

  @Test
  void builderIsReusable() {
    final PlayerListRenderer.Builder builder = tab().entry(PlayerListEntry.of("Steve"));
    final BufferedImage first = builder.build().join();
    builder.entry(PlayerListEntry.of("Alex"));
    final BufferedImage second = builder.build().join();
    assertEquals(10, first.getHeight());
    assertEquals(19, second.getHeight());
  }

  @Test
  void vanillaSortOrder() {
    final List<PlayerListEntry> entries = new ArrayList<>(List.of(
        PlayerListEntry.builder("zed").build(),
        PlayerListEntry.builder("Spec").spectator(true).build(),
        PlayerListEntry.builder("Alpha").build(),
        PlayerListEntry.builder("Owner").order(10).build(),
        PlayerListEntry.builder("beta").team("a").build()
    ));
    entries.sort(PlayerListRenderer.VANILLA_ORDER);
    assertEquals(List.of("Owner", "Alpha", "zed", "beta", "Spec"),
        entries.stream().map(PlayerListEntry::name).toList());
  }
}
