package net.ultranetwork.render.playerlist;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.ultranetwork.render.Backdrop;
import net.ultranetwork.render.RenderEnvironment;
import net.ultranetwork.render.graphics.Canvas;
import net.ultranetwork.render.graphics.Sprite;
import net.ultranetwork.render.graphics.Texture;
import net.ultranetwork.render.skin.SkinProvider;
import net.ultranetwork.render.skin.Skins;
import net.ultranetwork.render.text.StyledText;
import net.ultranetwork.render.text.TextRenderer;
import net.ultranetwork.render.util.ImageUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Renders the player list (tab list) exactly like the client's {@code PlayerTabOverlay}.
 */
public final class PlayerListRenderer {
  /**
   * The client's sort order: list order (descending), non-spectators first, team name, then profile name.
   */
  public static final Comparator<PlayerListEntry> VANILLA_ORDER = Comparator
      .comparingInt((PlayerListEntry entry) -> -entry.order())
      .thenComparingInt(entry -> entry.spectator() ? 1 : 0)
      .thenComparing(entry -> Objects.requireNonNullElse(entry.team(), ""))
      .thenComparing(PlayerListEntry::name, String::compareToIgnoreCase);

  private static final int MAX_PLAYERS = 80;
  private static final int MAX_ROWS_PER_COLUMN = 20;
  private static final int COLUMN_GAP = 5;
  private static final int SCREEN_MARGIN = 50;
  private static final int BACKGROUND = 0x80000000;
  private static final int ROW_BACKGROUND = 0x20FFFFFF;
  private static final int SPECTATOR_COLOUR = 0x90FFFFFF;
  private static final Pattern UPSIDE_DOWN_NAMES = Pattern.compile("Dinnerbone|Grumm");

  private final RenderEnvironment environment;
  private final @Nullable Component header;
  private final @Nullable Component footer;
  private final List<PlayerListEntry> entries;
  private final List<@Nullable Texture> skins;
  private final boolean showHeads;
  private final int screenWidth;
  private final int scale;
  private final @Nullable Backdrop backdrop;
  private final boolean transparentBackground;

  private PlayerListRenderer(Snapshot snapshot, List<@Nullable Texture> skins) {
    this.environment = snapshot.environment;
    this.header = snapshot.header;
    this.footer = snapshot.footer;
    this.entries = snapshot.entries;
    this.skins = skins;
    this.showHeads = snapshot.showHeads;
    this.screenWidth = snapshot.screenWidth;
    this.scale = snapshot.scale;
    this.backdrop = snapshot.backdrop;
    this.transparentBackground = snapshot.transparentBackground;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  private BufferedImage render() {
    final RenderEnvironment env = this.environment;
    final int screen = this.screenWidth;
    final int spaceWidth = TextRenderer.width(env, StyledText.plain(" "));

    final int count = this.entries.size();
    final List<StyledText> names = new ArrayList<>(count);
    final List<@Nullable StyledText> scores = new ArrayList<>(count);
    final int[] scoreWidths = new int[count];
    boolean hasObjective = false;
    int nameWidth = 0;
    int scoreColumnWidth = 0;
    for (int i = 0; i < count; i++) {
      final PlayerListEntry entry = this.entries.get(i);
      final Component name = entry.spectator()
          ? entry.displayName().decoration(TextDecoration.ITALIC, true)
          : entry.displayName();
      final StyledText styledName = env.text(name);
      names.add(styledName);
      nameWidth = Math.max(nameWidth, TextRenderer.width(env, styledName));

      final StyledText score = entry.score() != null ? env.text(entry.score()) : null;
      scores.add(score);
      if (score != null) {
        hasObjective = true;
        scoreWidths[i] = TextRenderer.width(env, score);
        scoreColumnWidth = Math.max(scoreColumnWidth, scoreWidths[i] > 0 ? spaceWidth + scoreWidths[i] : 0);
      }
    }

    int columns = 1;
    int rows = count;
    while (rows > MAX_ROWS_PER_COLUMN) {
      columns++;
      rows = (count + columns - 1) / columns;
    }

    final int scoreWidth = hasObjective ? scoreColumnWidth : 0;
    final int columnWidth = Math.min(columns * ((this.showHeads ? 9 : 0) + nameWidth + scoreWidth + 13),
        screen - SCREEN_MARGIN) / columns;
    final int gridLeft = screen / 2 - (columnWidth * columns + (columns - 1) * COLUMN_GAP) / 2;
    int totalWidth = columnWidth * columns + (columns - 1) * COLUMN_GAP;

    final List<StyledText> headerLines = this.lines(this.header);
    final List<StyledText> footerLines = this.lines(this.footer);
    for (StyledText line : headerLines) {
      totalWidth = Math.max(totalWidth, TextRenderer.width(env, line));
    }
    for (StyledText line : footerLines) {
      totalWidth = Math.max(totalWidth, TextRenderer.width(env, line));
    }

    final int boxLeft = screen / 2 - totalWidth / 2 - 1;
    final int boxRight = screen / 2 + totalWidth / 2 + 1;
    final int top = 9;
    int bottom = 10 + (headerLines.isEmpty() ? 0 : headerLines.size() * TextRenderer.LINE_HEIGHT + 1)
        + rows * TextRenderer.LINE_HEIGHT;
    if (!footerLines.isEmpty()) {
      bottom += 1 + footerLines.size() * TextRenderer.LINE_HEIGHT;
    }

    final Canvas canvas = new Canvas(boxLeft, top, boxRight - boxLeft, bottom - top, this.scale);
    final Random random = new Random();
    int y = 10;

    if (!headerLines.isEmpty()) {
      this.fillBackground(canvas, boxLeft, y - 1, boxRight, y + headerLines.size() * TextRenderer.LINE_HEIGHT, BACKGROUND);
      for (StyledText line : headerLines) {
        TextRenderer.draw(canvas, env, line, screen / 2 - TextRenderer.width(env, line) / 2, y, 0xFFFFFFFF, true,
            random);
        y += TextRenderer.LINE_HEIGHT;
      }
      y++;
    }

    this.fillBackground(canvas, boxLeft, y - 1, boxRight, y + rows * TextRenderer.LINE_HEIGHT, BACKGROUND);
    for (int i = 0; i < count; i++) {
      final int column = i / rows;
      final int row = i % rows;
      final int columnLeft = gridLeft + column * columnWidth + column * COLUMN_GAP;
      final int rowTop = y + row * TextRenderer.LINE_HEIGHT;
      this.fillBackground(canvas, columnLeft, rowTop, columnLeft + columnWidth, rowTop + 8, ROW_BACKGROUND);

      final PlayerListEntry entry = this.entries.get(i);
      int x = columnLeft;
      if (this.showHeads) {
        this.drawFace(canvas, entry, this.skins.get(i), x, rowTop);
        x += 9;
      }

      TextRenderer.draw(canvas, env, names.get(i), x, rowTop, entry.spectator() ? SPECTATOR_COLOUR : 0xFFFFFFFF,
          true, random);

      final StyledText score = scores.get(i);
      if (hasObjective && !entry.spectator()) {
        final int scoreLeft = x + nameWidth + 1;
        final int scoreRight = scoreLeft + scoreWidth;
        if (scoreRight - scoreLeft > 5 && score != null) {
          TextRenderer.draw(canvas, env, score, scoreRight - scoreWidths[i], rowTop, 0xFFFFFFFF, true, random);
        }
      }

      this.drawPing(canvas, entry.latency(), columnLeft + columnWidth - 11, rowTop);
    }

    if (!footerLines.isEmpty()) {
      y += rows * TextRenderer.LINE_HEIGHT + 1;
      this.fillBackground(canvas, boxLeft, y - 1, boxRight, y + footerLines.size() * TextRenderer.LINE_HEIGHT, BACKGROUND);
      for (StyledText line : footerLines) {
        TextRenderer.draw(canvas, env, line, screen / 2 - TextRenderer.width(env, line) / 2, y, 0xFFFFFFFF, true,
            random);
        y += TextRenderer.LINE_HEIGHT;
      }
    }

    return ImageUtil.withBackdrop(canvas.toImage(), this.backdrop);
  }

  private void fillBackground(Canvas canvas, int x0, int y0, int x1, int y1, int argb) {
    if (!this.transparentBackground) {
      canvas.fill(x0, y0, x1, y1, argb);
    }
  }

  private List<StyledText> lines(@Nullable Component component) {
    if (component == null) {
      return List.of();
    }
    final StyledText text = this.environment.text(component);
    if (text.plainText().isEmpty()) {
      return List.of();
    }
    return TextRenderer.split(this.environment, text, this.screenWidth - SCREEN_MARGIN);
  }

  private void drawFace(Canvas canvas, PlayerListEntry entry, @Nullable Texture skin, int x, int y) {
    Texture texture = skin;
    if (texture == null) {
      texture = this.environment.sprites().texture(Skins.defaultSkin(entry.uuid()));
    }
    if (texture == null) {
      return;
    }
    final boolean upsideDown = UPSIDE_DOWN_NAMES.matcher(entry.name()).matches();
    final float v = upsideDown ? 16 : 8;
    final float height = upsideDown ? -8 : 8;
    final float texelsPerPixel = texture.width() / 64f;
    canvas.blit(texture, 8 * texelsPerPixel, v * texelsPerPixel, 8 * texelsPerPixel, height * texelsPerPixel,
        x, y, 8, 8, -1);
    if (entry.showHat()) {
      canvas.blit(texture, 40 * texelsPerPixel, v * texelsPerPixel, 8 * texelsPerPixel, height * texelsPerPixel,
          x, y, 8, 8, -1);
    }
  }

  private void drawPing(Canvas canvas, int latency, int x, int y) {
    final String icon;
    if (latency < 0) {
      icon = "icon/ping_unknown";
    }
    else if (latency < 150) {
      icon = "icon/ping_5";
    }
    else if (latency < 300) {
      icon = "icon/ping_4";
    }
    else if (latency < 600) {
      icon = "icon/ping_3";
    }
    else if (latency < 1000) {
      icon = "icon/ping_2";
    }
    else {
      icon = "icon/ping_1";
    }
    final Sprite sprite = this.environment.sprites().sprite(Key.key(icon));
    if (sprite != null) {
      canvas.blitSprite(sprite, x, y, 10, 8);
    }
  }

  private record Snapshot(
      RenderEnvironment environment,
      @Nullable Component header,
      @Nullable Component footer,
      List<PlayerListEntry> entries,
      boolean showHeads,
      SkinProvider skinProvider,
      int screenWidth,
      int scale,
      @Nullable Backdrop backdrop,
      boolean transparentBackground
  ) {
  }

  public static final class Builder {
    private final List<PlayerListEntry> entries = new ArrayList<>();
    private @Nullable Component header;
    private @Nullable Component footer;
    private @Nullable Comparator<PlayerListEntry> sorter;
    private boolean showHeads;
    private SkinProvider skinProvider = SkinProvider.mojang();
    private int screenWidth = 480;
    private int scale = 2;
    private @Nullable Backdrop backdrop;
    private boolean transparentBackground;
    private @Nullable RenderEnvironment environment;

    private Builder() {
    }

    public @NotNull Builder header(@Nullable Component header) {
      this.header = header;
      return this;
    }

    public @NotNull Builder footer(@Nullable Component footer) {
      this.footer = footer;
      return this;
    }

    public @NotNull Builder entry(@NotNull PlayerListEntry entry) {
      this.entries.add(Objects.requireNonNull(entry, "entry"));
      return this;
    }

    public @NotNull Builder entries(@NotNull Collection<PlayerListEntry> entries) {
      entries.forEach(this::entry);
      return this;
    }

    /**
     * Overrides {@link #VANILLA_ORDER}.
     */
    public @NotNull Builder sortBy(@Nullable Comparator<PlayerListEntry> sorter) {
      this.sorter = sorter;
      return this;
    }

    /**
     * Draws player faces, as vanilla does on online-mode servers. Skins are fetched with the
     * {@link #skinProvider(SkinProvider) skin provider}. Off by default.
     */
    public @NotNull Builder showHeads(boolean showHeads) {
      this.showHeads = showHeads;
      return this;
    }

    public @NotNull Builder skinProvider(@NotNull SkinProvider skinProvider) {
      this.skinProvider = Objects.requireNonNull(skinProvider, "skinProvider");
      return this;
    }

    /**
     * The scaled screen width the overlay is laid out for. It caps column widths and wraps the header and footer
     * at {@code screenWidth - 50}. Default 480 (1080p at auto GUI scale).
     */
    public @NotNull Builder screenWidth(int screenWidth) {
      if (screenWidth <= SCREEN_MARGIN) {
        throw new IllegalArgumentException("Screen width must be greater than " + SCREEN_MARGIN);
      }
      this.screenWidth = screenWidth;
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
     * Fills the area behind the translucent overlay (in game this is the world). Transparent by default.
     */
    public @NotNull Builder backdrop(@Nullable Color backdrop) {
      return this.backdrop(backdrop == null ? null : Backdrop.solid(backdrop));
    }

    /**
     * Paints behind the overlay, e.g. {@link Backdrop#sky()} for the look of the tab list over a daytime sky.
     * Transparent by default.
     */
    public @NotNull Builder backdrop(@Nullable Backdrop backdrop) {
      this.backdrop = backdrop;
      return this;
    }

    /**
     * Skips the translucent black panel and row highlights, so only text, heads, scores and signal bars are drawn
     * on a fully transparent image. Useful when the image is shown on a dark background such as Discord. The layout
     * and image size are unchanged. Off by default.
     */
    public @NotNull Builder transparentBackground(boolean transparentBackground) {
      this.transparentBackground = transparentBackground;
      return this;
    }

    public @NotNull Builder environment(@Nullable RenderEnvironment environment) {
      this.environment = environment;
      return this;
    }

    private Snapshot snapshot() {
      final List<PlayerListEntry> sorted = new ArrayList<>(this.entries);
      sorted.sort(this.sorter != null ? this.sorter : VANILLA_ORDER);
      return new Snapshot(
          this.environment != null ? this.environment : RenderEnvironment.defaultEnvironment(),
          this.header, this.footer,
          List.copyOf(sorted.subList(0, Math.min(MAX_PLAYERS, sorted.size()))),
          this.showHeads, this.skinProvider, this.screenWidth, this.scale, this.backdrop, this.transparentBackground);
    }

    public @NotNull CompletableFuture<BufferedImage> build() {
      return this.build(ForkJoinPool.commonPool());
    }

    /**
     * Fetches skins (if heads are shown) and renders on {@code executor}. The builder can be reused.
     */
    public @NotNull CompletableFuture<BufferedImage> build(@NotNull Executor executor) {
      final Snapshot snapshot = this.snapshot();
      if (!snapshot.showHeads()) {
        final List<Texture> noSkins = new ArrayList<>();
        snapshot.entries().forEach(entry -> noSkins.add(null));
        return CompletableFuture.supplyAsync(() -> new PlayerListRenderer(snapshot, noSkins).render(), executor);
      }

      final List<CompletableFuture<Texture>> skins = snapshot.entries().stream()
          .map(entry -> snapshot.skinProvider().fetchSkin(entry)
              .handle((image, error) -> error == null ? Skins.process(image) : null))
          .toList();
      return CompletableFuture.allOf(skins.toArray(CompletableFuture[]::new))
          .thenApplyAsync(ignored -> {
            final List<Texture> textures = new ArrayList<>(skins.size());
            skins.forEach(skin -> textures.add(skin.join()));
            return new PlayerListRenderer(snapshot, textures).render();
          }, executor);
    }
  }
}
