package net.ultranetwork.render.item;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.ultranetwork.render.util.RenderUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class ItemImageRenderer {
  private static final int DEFAULT_PADDING = 6;
  private static final int DEFAULT_LINE_SPACING = 3;
  private static final int FONT_SIZE_PX = 16;
  private static final int LINE_HEIGHT = FONT_SIZE_PX - 2; // minecrafts line height (i think)

  private static final Color DEFAULT_BACKGROUND_COLOUR = new Color(16, 0, 16, 240);
  private static final Color DEFAULT_TOOLTIP_SHADOW_COLOUR = new Color(5, 0, 5, 100);

  private final ItemStack item;
  private final int padding;
  private final int lineSpacing;
  private final Color backgroundColour;
  private final Color shadowColour;
  private final Color defaultTextColour;

  private ItemImageRenderer(Builder builder) {
    this.item = Objects.requireNonNull(builder.item, "ItemStack cannot be null");
    this.padding = builder.padding;
    this.lineSpacing = builder.lineSpacing;
    this.backgroundColour = builder.backgroundColour;
    this.shadowColour = builder.shadowColour;
    this.defaultTextColour = builder.defaultTextColour;
  }

  /**
   *
   * @param item The ItemStack
   * @return A list of Components representing the tooltip lines
   */
  @NotNull
  private static List<Component> getTooltipLines(
      @NotNull ItemStack item
  ) {
    final List<Component> lines = new ArrayList<>();
    final ItemMeta meta = item.getItemMeta();

    Component nameComponent = null;
    if (meta != null) {

      if (meta.hasDisplayName()) {
        nameComponent = meta.displayName();
      }
    }

    // Fallback if no custom display name
    if (nameComponent == null) {
      final String baseName = item.getType().name().replace('_', ' ').toLowerCase();
      final StringBuilder titleCaseName = new StringBuilder();
      boolean nextTitleCase = true;

      for (char c : baseName.toCharArray()) {
        if (Character.isSpaceChar(c)) {
          nextTitleCase = true;
        }
        else if (nextTitleCase) {
          c = Character.toTitleCase(c);
          nextTitleCase = false;
        }

        titleCaseName.append(c);
      }

      nameComponent = Component.text(titleCaseName.toString(), NamedTextColor.WHITE);
    }

    // Append amount if greater than 1
    if (item.getAmount() > 1) {
      nameComponent = nameComponent.append(Component.text(" x" + item.getAmount(), NamedTextColor.WHITE));
    }

    lines.add(nameComponent);

    if (meta != null && meta.hasLore()) {
      final List<Component> lore = meta.lore();

      if (lore != null) {
        lines.addAll(lore);
      }
    }

    return lines;
  }

  private BufferedImage render() {
    final List<Component> lines = getTooltipLines(this.item);
    final BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D tempG = tempImage.createGraphics();
    tempG.setFont(RenderUtil.MINECRAFT_FONT);

    final FontMetrics metrics = tempG.getFontMetrics();
    int maxTextWidth = 0;

    for (Component line : lines) {
      maxTextWidth = Math.max(maxTextWidth, RenderUtil.calculateComponentWidth(metrics, line));
    }

    tempG.dispose();

    // Calculate height
    final int totalLines = lines.size();
    final int contentHeight = (totalLines == 0) ? 0 : (totalLines * LINE_HEIGHT + Math.max(0, totalLines - 1) * this.lineSpacing);

    // Add padding and border size (1px border)
    int tooltipWidth = maxTextWidth + this.padding * 2 + 2; // +2 for 1px border on each side
    int tooltipHeight = contentHeight + this.padding * 2 + 2; // +2 for 1px border top/bottom

    // Ensure minimum size
    tooltipWidth = Math.max(20, tooltipWidth);
    tooltipHeight = Math.max(20, tooltipHeight);

    // Create final image
    final BufferedImage tooltipImage = new BufferedImage(tooltipWidth, tooltipHeight, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D graphics = tooltipImage.createGraphics();
    graphics.setFont(RenderUtil.MINECRAFT_FONT);
    RenderUtil.applyMinecraftRenderingHints(graphics);

    // Draw background fill
    graphics.setColor(this.backgroundColour);
    graphics.fillRect(1, 1, tooltipWidth - 2, tooltipHeight - 2);

    // Draw border top, bottom, left, right
    graphics.drawLine(0, 0, tooltipWidth - 1, 0);
    graphics.drawLine(0, tooltipHeight - 1, tooltipWidth - 1, tooltipHeight - 1);
    graphics.drawLine(0, 0, 0, tooltipHeight - 1);
    graphics.drawLine(tooltipWidth - 1, 0, tooltipWidth - 1, tooltipHeight - 1);

    // Draw text lines
    final int startX = this.padding + 1; // +1 to be inside the border
    int currentY = this.padding + 1 + metrics.getAscent(); // Start y baseline inside top border

    for (Component line : lines) {
      RenderUtil.drawAdventureComponent(graphics, line, startX, currentY, this.defaultTextColour, this.shadowColour);
      currentY += (LINE_HEIGHT + this.lineSpacing);
    }

    graphics.dispose();
    return tooltipImage;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private ItemStack item;
    private int padding = DEFAULT_PADDING;
    private int lineSpacing = DEFAULT_LINE_SPACING;
    private Color backgroundColour = DEFAULT_BACKGROUND_COLOUR;
    private Color shadowColour = DEFAULT_TOOLTIP_SHADOW_COLOUR;
    private Color defaultTextColour = RenderUtil.DEFAULT_TEXT_COLOUR;

    private Builder() {
    }

    @NotNull
    public Builder item(@NotNull ItemStack item) {
      this.item = item;
      return this;
    }

    public Builder padding(int padding) {
      this.padding = Math.max(0, padding);
      return this;
    }

    public Builder lineSpacing(int lineSpacing) {
      this.lineSpacing = Math.max(0, lineSpacing);
      return this;
    }

    public Builder backgroundColor(@NotNull Color backgroundColour) {
      this.backgroundColour = backgroundColour;
      return this;
    }

    public Builder shadowColor(@NotNull Color shadowColour) {
      this.shadowColour = shadowColour;
      return this;
    }

    public Builder defaultTextColor(@NotNull Color defaultTextColour) {
      this.defaultTextColour = defaultTextColour;
      return this;
    }

    public CompletableFuture<BufferedImage> build() {
      if (item == null) {
        return CompletableFuture.failedFuture(new IllegalStateException("ItemStack must be set before building"));
      }

      // Create immutable copy for async task
      final ItemImageRenderer renderer = new ItemImageRenderer(this);
      return CompletableFuture.supplyAsync(renderer::render); // Run render() asynchronously
    }
  }
}