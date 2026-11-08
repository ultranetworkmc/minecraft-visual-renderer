package net.ultranetwork.render.tooltip;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.ultranetwork.render.Backdrop;
import net.ultranetwork.render.RenderEnvironment;
import net.ultranetwork.render.graphics.Canvas;
import net.ultranetwork.render.graphics.Sprite;
import net.ultranetwork.render.text.StyledText;
import net.ultranetwork.render.text.TextRenderer;
import net.ultranetwork.render.util.ImageUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Renders a tooltip exactly like the client's {@code GuiGraphics#renderTooltip} for text lines.
 */
public final class TooltipRenderer {
  private static final Key BACKGROUND = Key.key("tooltip/background");
  private static final Key FRAME = Key.key("tooltip/frame");
  private static final int LINE_HEIGHT = 10;
  private static final int FIRST_LINE_GAP = 2;
  // Tooltip sprites are drawn 12 GUI pixels around the text (3 padding + 9 margin).
  private static final int SPRITE_INSET = 12;

  private final List<Component> lines;
  private final @Nullable Key style;
  private final int scale;
  private final @Nullable Backdrop backdrop;
  private final RenderEnvironment environment;

  private TooltipRenderer(Builder builder) {
    this.lines = List.copyOf(builder.lines);
    this.style = builder.style;
    this.scale = builder.scale;
    this.backdrop = builder.backdrop;
    this.environment = builder.environment != null ? builder.environment : RenderEnvironment.defaultEnvironment();
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  private BufferedImage render() {
    final RenderEnvironment env = this.environment;
    final List<StyledText> texts = this.lines.stream().map(env::text).toList();

    int width = 0;
    for (StyledText text : texts) {
      width = Math.max(width, TextRenderer.width(env, text));
    }
    final int height = (texts.size() == 1 ? -FIRST_LINE_GAP : 0) + texts.size() * LINE_HEIGHT;

    final int spriteWidth = width + SPRITE_INSET * 2;
    final int spriteHeight = height + SPRITE_INSET * 2;
    final Canvas canvas = new Canvas(-SPRITE_INSET, -SPRITE_INSET, spriteWidth, spriteHeight, this.scale);
    canvas.blitSprite(this.sprite("_background", BACKGROUND), -SPRITE_INSET, -SPRITE_INSET, spriteWidth, spriteHeight);
    canvas.blitSprite(this.sprite("_frame", FRAME), -SPRITE_INSET, -SPRITE_INSET, spriteWidth, spriteHeight);

    final Random random = new Random();
    int y = 0;
    for (int i = 0; i < texts.size(); i++) {
      TextRenderer.draw(canvas, env, texts.get(i), 0, y, 0xFFFFFFFF, true, random);
      y += LINE_HEIGHT + (i == 0 ? FIRST_LINE_GAP : 0);
    }

    return ImageUtil.withBackdrop(ImageUtil.cropTransparent(canvas.toImage()), this.backdrop);
  }

  private Sprite sprite(String suffix, Key fallback) {
    Sprite sprite = null;
    if (this.style != null) {
      sprite = this.environment.sprites().sprite(Key.key(this.style.namespace(), "tooltip/" + this.style.value() + suffix));
    }
    if (sprite == null) {
      sprite = this.environment.sprites().sprite(fallback);
    }
    return Objects.requireNonNull(sprite, () -> "Missing tooltip sprite " + fallback);
  }

  public static final class Builder {
    private final List<Component> lines = new ArrayList<>();
    private @Nullable Key style;
    private int scale = 2;
    private @Nullable Backdrop backdrop;
    private @Nullable RenderEnvironment environment;

    private Builder() {
    }

    public @NotNull Builder line(@NotNull Component line) {
      this.lines.add(Objects.requireNonNull(line, "line"));
      return this;
    }

    public @NotNull Builder lines(@NotNull Collection<? extends Component> lines) {
      lines.forEach(this::line);
      return this;
    }

    /**
     * Uses a custom tooltip style, i.e. the {@code minecraft:tooltip_style} item component. The sprites
     * {@code <namespace>:tooltip/<path>_background} and {@code _frame} must exist in the environment's assets.
     */
    public @NotNull Builder style(@Nullable Key style) {
      this.style = style;
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

    /**
     * Fills the area behind the (partly translucent) tooltip. Transparent by default.
     */
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

    /**
     * Renders on the calling thread.
     */
    public @NotNull BufferedImage render() {
      if (this.lines.isEmpty()) {
        throw new IllegalStateException("A tooltip needs at least one line");
      }
      return new TooltipRenderer(this).render();
    }

    public @NotNull CompletableFuture<BufferedImage> build() {
      return this.build(ForkJoinPool.commonPool());
    }

    public @NotNull CompletableFuture<BufferedImage> build(@NotNull Executor executor) {
      if (this.lines.isEmpty()) {
        return CompletableFuture.failedFuture(new IllegalStateException("A tooltip needs at least one line"));
      }
      final TooltipRenderer renderer = new TooltipRenderer(this);
      return CompletableFuture.supplyAsync(renderer::render, executor);
    }
  }
}
