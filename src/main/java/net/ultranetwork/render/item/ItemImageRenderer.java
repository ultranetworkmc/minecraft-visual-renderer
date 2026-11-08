package net.ultranetwork.render.item;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.inventory.tooltip.TooltipContext;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import net.kyori.adventure.text.Component;
import net.ultranetwork.render.Backdrop;
import net.ultranetwork.render.RenderEnvironment;
import net.ultranetwork.render.tooltip.TooltipRenderer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Renders an item's hover tooltip. The lines come from the server's own tooltip logic
 * ({@link ItemStack#computeTooltipLines}), so names, rarity colours, enchantments, attributes, lore and hidden
 * components all match what a player sees. Requires a running Paper server.
 */
public final class ItemImageRenderer {

  private ItemImageRenderer() {
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private @Nullable ItemStack item;
    private @Nullable Player player;
    private boolean advanced;
    private boolean creative;
    private int scale = 2;
    private @Nullable Backdrop backdrop;
    private @Nullable RenderEnvironment environment;

    private Builder() {
    }

    public @NotNull Builder item(@NotNull ItemStack item) {
      this.item = Objects.requireNonNull(item, "item");
      return this;
    }

    /**
     * The player the tooltip is computed for; affects context-dependent lines. Optional.
     */
    public @NotNull Builder player(@Nullable Player player) {
      this.player = player;
      return this;
    }

    /**
     * Advanced tooltips (F3+H): durability, item id and component count.
     */
    public @NotNull Builder advanced(boolean advanced) {
      this.advanced = advanced;
      return this;
    }

    public @NotNull Builder creative(boolean creative) {
      this.creative = creative;
      return this;
    }

    /**
     * @param scale output pixels per GUI pixel (the game's GUI scale), default 2
     */
    public @NotNull Builder scale(int scale) {
      if (scale < 1) {
        throw new IllegalArgumentException("Scale must be at least 1");
      }
      this.scale = scale;
      return this;
    }

    public @NotNull Builder backdrop(@Nullable Color backdrop) {
      return this.backdrop(backdrop == null ? null : Backdrop.solid(backdrop));
    }

    /**
     * Paints behind the tooltip, e.g. {@link Backdrop#sky()}. Transparent by default.
     */
    public @NotNull Builder backdrop(@Nullable Backdrop backdrop) {
      this.backdrop = backdrop;
      return this;
    }

    public @NotNull Builder environment(@Nullable RenderEnvironment environment) {
      this.environment = environment;
      return this;
    }

    private TooltipRenderer.Builder prepare() {
      if (this.item == null) {
        throw new IllegalStateException("ItemStack must be set before building");
      }
      final List<Component> lines = this.item.computeTooltipLines(
          TooltipContext.create(this.advanced, this.creative), this.player);
      if (lines.isEmpty()) {
        throw new IllegalStateException("Item has no visible tooltip");
      }
      return TooltipRenderer.builder()
          .lines(lines)
          .style(this.item.getData(DataComponentTypes.TOOLTIP_STYLE))
          .scale(this.scale)
          .backdrop(this.backdrop)
          .environment(this.environment);
    }

    /**
     * Computes the tooltip and renders it on the calling thread (should be the server thread).
     */
    public @NotNull BufferedImage render() {
      return this.prepare().render();
    }

    /**
     * Computes the tooltip lines on the calling thread (should be the server thread) and renders asynchronously.
     */
    public @NotNull CompletableFuture<BufferedImage> build() {
      return this.build(ForkJoinPool.commonPool());
    }

    public @NotNull CompletableFuture<BufferedImage> build(@NotNull Executor executor) {
      final TooltipRenderer.Builder tooltip;
      try {
        tooltip = this.prepare();
      }
      catch (RuntimeException e) {
        return CompletableFuture.failedFuture(e);
      }
      return tooltip.build(executor);
    }
  }
}
