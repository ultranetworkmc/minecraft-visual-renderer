package net.ultranetwork.render.playerlist;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.ultranetwork.render.util.HeadUtil;
import net.ultranetwork.render.util.RenderUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlayerListRenderer {
  private static final int DEFAULT_MAX_PLAYERS_PER_COLUMN = 20;
  private static final int DEFAULT_COLUMN_SPACING = 10;
  private static final int DEFAULT_PADDING = 5;
  private static final int FOOTER_EXTRA_SPACING = 6;
  private static final int HEADER_FOOTER_PADDING = 4;
  private static final int PLAYER_NAME_PING_SPACING = 4;
  private static final int PING_BAR_WIDTH = 2;
  private static final int PING_BAR_HEIGHT_INCREMENT = 2;
  private static final int PING_BAR_SPACING = 1;
  private static final int PLAYER_LINE_HEIGHT = 18;
  private static final int PLAYER_HEAD_SIZE = 16;
  private static final int PLAYER_HEAD_NAME_SPACING = 2; // Space between head and name

  // Colours specific to PlayerList
  private static final Color BACKGROUND_COLOUR = new Color(56, 70, 117);
  private static final Color TAB_TEXT_SHADOW_COLOUR = new Color(0, 0, 0, 80);

  // Ping thresholds and colours
  private static final Color PING_GOOD_COLOUR = new Color(0, 255, 0);
  private static final Color PING_MEDIUM_COLOUR = new Color(255, 255, 0);
  private static final Color PING_BAD_COLOUR = new Color(255, 85, 85);
  private static final Color PING_VERY_BAD_COLOUR = new Color(170, 0, 0);
  private static final Color PING_UNKNOWN_COLOUR = new Color(170, 170, 170);

  // todo this
  private static final int PING_GOOD_THRESHOLD = 150;
  private static final int PING_MEDIUM_THRESHOLD = 300;
  private static final int PING_BAD_THRESHOLD = 500;

  private final Component header;
  private final Component footer;
  private final List<PlayerListName> players;
  private final int maxPlayersPerColumn;
  private final int columnSpacing;
  private final int padding;
  private final boolean showHeads;
  private final Map<String, BufferedImage> playerHeads;

  private PlayerListRenderer(
      Builder builder,
      Map<String, BufferedImage> fetchedHeads)
  {
    this.header = builder.header != null ? builder.header : Component.empty();
    this.footer = builder.footer != null ? builder.footer : Component.empty();
    this.players = List.copyOf(builder.names);
    this.maxPlayersPerColumn = builder.maxPlayersPerColumn;
    this.columnSpacing = builder.columnSpacing;
    this.padding = builder.padding;
    this.showHeads = builder.showHeads;
    this.playerHeads = fetchedHeads;
  }

  private BufferedImage render() {
    final int playerCount = this.players.size();
    final int numColumns = (playerCount == 0) ? 0 :
        Math.min(4, (playerCount + this.maxPlayersPerColumn - 1) / this.maxPlayersPerColumn);

    final BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D tempG = tempImage.createGraphics();
    tempG.setFont(RenderUtil.MINECRAFT_FONT);

    final int headerHeight = !Component.empty().equals(this.header) ? PLAYER_LINE_HEIGHT + HEADER_FOOTER_PADDING * 2 : 0;
    final int footerHeight = !Component.empty().equals(this.footer) ? PLAYER_LINE_HEIGHT + HEADER_FOOTER_PADDING * 2 : 0;

    final FontMetrics metrics = tempG.getFontMetrics();
    final int headerWidth = RenderUtil.calculateComponentWidth(metrics, this.header);
    final int footerWidth = RenderUtil.calculateComponentWidth(metrics, this.footer);
    final int maxHeaderFooterWidth = Math.max(headerWidth, footerWidth);

    int maxPlayerNameWidth = 0;
    for (PlayerListName playerListName : this.players) {
      final Component nameToMeasure = playerListName.listNameComponent();
      maxPlayerNameWidth = Math.max(maxPlayerNameWidth, RenderUtil.calculateComponentWidth(metrics, nameToMeasure));
    }

    final int pingBarsWidth = (PING_BAR_WIDTH + PING_BAR_SPACING) * 5 - PING_BAR_SPACING;
    final int headAreaWidth = this.showHeads
        ? PLAYER_HEAD_SIZE + PLAYER_HEAD_NAME_SPACING
        : 0;

    // Width needed for one column's content (head + name + ping)
    final int singleColumnContentWidth = headAreaWidth + maxPlayerNameWidth + PLAYER_NAME_PING_SPACING + pingBarsWidth;

    // Total width for all content columns + spacing between them
    final int requiredContentWidth = numColumns * singleColumnContentWidth + Math.max(0, numColumns - 1) * this.columnSpacing;

    // Image width is max of content area or header/footer, plus padding
    final int imageWidth = Math.max(requiredContentWidth, maxHeaderFooterWidth) + this.padding * 2;

    // Calculate actual column width based on final image width
    final int actualContentWidth = imageWidth - this.padding * 2;
    final int columnWidth = (numColumns == 0) ? actualContentWidth
        : (actualContentWidth - Math.max(0, numColumns - 1) * this.columnSpacing) / numColumns;

    // Calculate height
    final int numRowsNeeded = (playerCount == 0) ? 0 : (int) Math.ceil((double) playerCount / numColumns);
    final int actualNumRowsDisplayed = Math.min(this.maxPlayersPerColumn, numRowsNeeded);
    final int playersHeight = actualNumRowsDisplayed * PLAYER_LINE_HEIGHT;
    final int extraSpacing = (footerHeight > 0 && playersHeight > 0) ? FOOTER_EXTRA_SPACING : 0;
    final int imageHeight = headerHeight + playersHeight + extraSpacing + footerHeight + this.padding * 2;

    tempG.dispose();

    // final image
    final BufferedImage finalImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D graphics = finalImage.createGraphics();
    graphics.setFont(RenderUtil.MINECRAFT_FONT);
    RenderUtil.applyMinecraftRenderingHints(graphics);

    // Draw background
    graphics.setColor(BACKGROUND_COLOUR);
    graphics.fillRect(0, 0, imageWidth, imageHeight);

    // Draw header
    int currentY = this.padding;
    if (headerHeight > 0) {
      final int headerTextWidth = RenderUtil.calculateComponentWidth(metrics, this.header); // Use actual metrics
      final int headerX = this.padding + Math.max(0, (actualContentWidth - headerTextWidth) / 2);
      final int headerBaselineY = currentY + HEADER_FOOTER_PADDING + metrics.getAscent();

      RenderUtil.drawAdventureComponent(graphics, this.header, headerX, headerBaselineY, RenderUtil.DEFAULT_TEXT_COLOUR, TAB_TEXT_SHADOW_COLOUR);
      currentY += headerHeight;
    }

    // Draw players
    final int playerGridY = currentY;
    int playerIndex = 0;
    for (int col = 0; col < numColumns; col++) {
      final int columnStartX = this.padding + col * (columnWidth + this.columnSpacing);

      for (int row = 0; row < actualNumRowsDisplayed; row++) {
        if (playerIndex >= this.players.size())
          break;

        final PlayerListName playerRenderData = this.players.get(playerIndex++);
        final int playerBaseY = playerGridY + row * PLAYER_LINE_HEIGHT;
        final int playerTextBaselineY = playerBaseY + metrics.getAscent();
        int currentDrawX = columnStartX; // Starting X for this player entry

        // Draw head
        if (this.showHeads) {
          final BufferedImage headImage = this.playerHeads.get(playerRenderData.plainListName());

          if (headImage != null) {
            final int headY = playerBaseY + (PLAYER_LINE_HEIGHT - PLAYER_HEAD_SIZE) / 2; // center
            graphics.drawImage(headImage, currentDrawX, headY, PLAYER_HEAD_SIZE, PLAYER_HEAD_SIZE, null);
          }

          // Always continue X even if head is missing t maintain alignment
          currentDrawX += PLAYER_HEAD_SIZE + PLAYER_HEAD_NAME_SPACING;
        }

        RenderUtil.drawAdventureComponent(graphics, playerRenderData.listNameComponent(),
            currentDrawX,
            playerTextBaselineY,
            RenderUtil.DEFAULT_TEXT_COLOUR,
            TAB_TEXT_SHADOW_COLOUR);

        // Calculate ping X relative to the right edge of the columns content area
        final int columnContentEndX = columnStartX + columnWidth;
        final int pingX = columnContentEndX - pingBarsWidth;

        // Calculate ping Y centered vertically within the line height
        final int totalPingHeight = PING_BAR_HEIGHT_INCREMENT * 5; // 5 = max height
        final int pingYOffset = (PLAYER_LINE_HEIGHT - totalPingHeight) / 2;
        final int pingDrawY = playerBaseY + pingYOffset;

        drawPingBars(graphics, playerRenderData.ping(), pingX, pingDrawY);
      }

      if (playerIndex >= this.players.size())
        break;
    }

    // Draw footer
    if (footerHeight > 0) {
      currentY = (playerGridY + playersHeight) + extraSpacing;

      final int footerTextWidth = RenderUtil.calculateComponentWidth(metrics, this.footer);
      final int footerX = this.padding + Math.max(0, (actualContentWidth - footerTextWidth) / 2);
      final int footerBaselineY = currentY + HEADER_FOOTER_PADDING + metrics.getAscent();

      RenderUtil.drawAdventureComponent(graphics, this.footer, footerX, footerBaselineY, RenderUtil.DEFAULT_TEXT_COLOUR, TAB_TEXT_SHADOW_COLOUR);
    }

    graphics.dispose();
    return finalImage;
  }

  private void drawPingBars(
      Graphics2D graphics,
      int ping,
      int x,
      int y)
  {
    final int barsToShow;
    final Color barColour;

    if (ping < 0) {
      barsToShow = 0;
      barColour = PING_UNKNOWN_COLOUR;
    }
    else if (ping < PING_GOOD_THRESHOLD) {
      barsToShow = 5;
      barColour = PING_GOOD_COLOUR;
    }
    else if (ping < PING_MEDIUM_THRESHOLD) {
      barsToShow = 4;
      barColour = PING_MEDIUM_COLOUR;
    }
    else if (ping < PING_BAD_THRESHOLD) {
      barsToShow = 3;
      barColour = PING_BAD_COLOUR;
    }
    else {
      barsToShow = 2;
      barColour = PING_VERY_BAD_COLOUR;
    }

    final int maxTotalBarHeight = PING_BAR_HEIGHT_INCREMENT * 5;
    int currentX = x;
    int currentBarHeight = PING_BAR_HEIGHT_INCREMENT;

    // Draw 5 bar positions/placeholders
    for (int i = 1; i <= 5; ++i) {
      final int barY = y + (maxTotalBarHeight - currentBarHeight); // Align bars at the bottom

      if (i <= barsToShow) {
        graphics.setColor(barColour);
        graphics.fillRect(currentX, barY, PING_BAR_WIDTH, currentBarHeight);
      }
      else if (ping >= 0) { // Only draw grey background bars if ping is known
        graphics.setColor(PING_UNKNOWN_COLOUR);
        graphics.fillRect(currentX, barY, PING_BAR_WIDTH, currentBarHeight);
      }
      currentX += PING_BAR_WIDTH + PING_BAR_SPACING;
      currentBarHeight += PING_BAR_HEIGHT_INCREMENT;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final List<PlayerListName> names = new ArrayList<>();
    private int maxPlayersPerColumn = DEFAULT_MAX_PLAYERS_PER_COLUMN;
    private int columnSpacing = DEFAULT_COLUMN_SPACING;
    private int padding = DEFAULT_PADDING;
    private Component header = null;
    private Component footer = Component.text("Players Online: ", NamedTextColor.GRAY).append(Component.text("0", NamedTextColor.WHITE));
    private Comparator<PlayerListName> customSorter = null;
    private boolean showHeads = false;

    private Builder() {}

    public Builder header(@Nullable Component header) {
      this.header = header;
      return this;
    }

    public Builder footer(@Nullable Component footer) {
      this.footer = footer;
      return this;
    }

    public Builder names(@NotNull Collection<PlayerListName> names) {
      this.names.addAll(names);
      return this;
    }

    public Builder name(@NotNull PlayerListName name) {
      this.names.add(name);
      return this;
    }

    public Builder maxPlayersPerColumn(int maxPlayersPerColumn) {
      this.maxPlayersPerColumn = Math.max(1, maxPlayersPerColumn);
      return this;
    }

    public Builder columnSpacing(int columnSpacing) {
      this.columnSpacing = Math.max(0, columnSpacing);
      return this;
    }

    public Builder padding(int padding) {
      this.padding = Math.max(0, padding);
      return this;
    }

    public Builder showNameHeads(boolean showHeads) {
      this.showHeads = showHeads;
      return this;
    }

    public Builder sortBy(@NotNull Comparator<PlayerListName> sorter) {
      this.customSorter = sorter;
      return this;
    }

    public CompletableFuture<BufferedImage> build() {
      this.names.sort(
          Objects.requireNonNullElseGet(this.customSorter, () -> Comparator
              .comparingInt(PlayerListName::priority)
              .reversed() // Higher priority first
              .thenComparing(PlayerListName::plainListName, String.CASE_INSENSITIVE_ORDER))
      );

      if (this.footer != null && this.footer.children().size() == 1
          && this.footer.equals(Component.text("Players Online: ", NamedTextColor.GRAY).append(Component.text("0", NamedTextColor.WHITE)))) {
        this.footer = Component.text("Players Online: ", NamedTextColor.GRAY)
            .append(Component.text(this.names.size(), NamedTextColor.WHITE));
      }

      CompletableFuture<Map<String, BufferedImage>> headsFuture;
      if (this.showHeads && !this.names.isEmpty()) {
        final List<CompletableFuture<Map.Entry<String, BufferedImage>>> fetchFutures = this.names.stream()
            .map(PlayerListName::plainListName)
            .distinct()
            .map(name -> HeadUtil.fetchPlayerHead(name)
                .thenApply(image -> Map.entry(name, image)))
            .toList();

        // Combine all fetch futures: wait for all to complete
        headsFuture = CompletableFuture.allOf(fetchFutures.toArray(new CompletableFuture[0]))
            .thenApply(v -> fetchFutures.stream()
                .map(CompletableFuture::join)
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
      }
      else {
        headsFuture = CompletableFuture.completedFuture(Collections.emptyMap());
      }

      final Builder builderSnapshot = this.copy();
      return headsFuture.thenApplyAsync(fetchedHeads -> {
        final PlayerListRenderer renderer = new PlayerListRenderer(builderSnapshot, fetchedHeads);
        return renderer.render(); // Perform the synchronous rendering part
      });
    }

    private Builder copy() {
      Builder copy = new Builder();
      copy.names.addAll(this.names);
      copy.maxPlayersPerColumn = this.maxPlayersPerColumn;
      copy.columnSpacing = this.columnSpacing;
      copy.padding = this.padding;
      copy.header = this.header;
      copy.footer = this.footer;
      copy.customSorter = this.customSorter;
      copy.showHeads = this.showHeads;
      return copy;
    }
  }
}