package net.ultranetwork.render.text;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.ultranetwork.render.graphics.Canvas;
import org.jetbrains.annotations.NotNull;

/**
 * Measures, wraps and draws {@link StyledText} following vanilla's {@code Font}, {@code StringSplitter} and glyph
 * rendering rules.
 */
public final class TextRenderer {
  public static final int LINE_HEIGHT = 9;

  private TextRenderer() {
  }

  private record GlyphDraw(Glyph glyph, float x, float y, int colour, int shadowColour, boolean bold, boolean italic) {
  }

  private record EffectDraw(float x0, float y0, float x1, float y1, int colour, int shadowColour, float shadowOffset) {
  }

  public static int width(@NotNull Fonts fonts, @NotNull StyledText text) {
    final float[] width = {0f};
    for (StyledText.Run run : text.runs()) {
      LegacyFormatting.iterate(run.text(), run.style(), (index, style, codepoint) -> {
        width[0] += fonts.font(style.font()).glyph(codepoint).advance(style.hasDecoration(TextDecoration.BOLD));
        return true;
      });
    }
    return (int) Math.ceil(width[0]);
  }

  /**
   * Draws a single line of text with its top-left corner at {@code (x, y)}.
   *
   * @param colour     default ARGB colour; nothing is drawn when its alpha is zero
   * @param dropShadow whether to draw the standard drop shadow
   */
  public static void draw(
      @NotNull Canvas canvas,
      @NotNull Fonts fonts,
      @NotNull StyledText text,
      float x,
      float y,
      int colour,
      boolean dropShadow,
      @NotNull Random random)
  {
    if ((colour >>> 24) == 0) {
      return;
    }

    final List<GlyphDraw> glyphs = new ArrayList<>();
    final List<EffectDraw> effects = new ArrayList<>();
    final float[] pen = {x};
    for (StyledText.Run run : text.runs()) {
      LegacyFormatting.iterate(run.text(), run.style(), (index, style, codepoint) -> {
        final MinecraftFont font = fonts.font(style.font());
        Glyph glyph = font.glyph(codepoint);
        if (style.hasDecoration(TextDecoration.OBFUSCATED) && codepoint != ' ') {
          glyph = font.randomGlyph(glyph, random);
        }

        final boolean bold = style.hasDecoration(TextDecoration.BOLD);
        final int textColour = textColour(style.color(), colour);
        final int shadowColour = shadowColour(style, textColour, dropShadow);
        final float advance = glyph.advance(bold);
        final float effectStart = index == 0 ? pen[0] - 1f : pen[0];

        glyphs.add(new GlyphDraw(glyph, pen[0], y, textColour, shadowColour, bold,
            style.hasDecoration(TextDecoration.ITALIC)));
        if (style.hasDecoration(TextDecoration.STRIKETHROUGH)) {
          effects.add(new EffectDraw(effectStart, y + 4.5f - 1f, pen[0] + advance, y + 4.5f,
              textColour, shadowColour, glyph.shadowOffset()));
        }
        if (style.hasDecoration(TextDecoration.UNDERLINED)) {
          effects.add(new EffectDraw(effectStart, y + 9f - 1f, pen[0] + advance, y + 9f,
              textColour, shadowColour, glyph.shadowOffset()));
        }
        pen[0] += advance;
        return true;
      });
    }

    for (GlyphDraw draw : glyphs) {
      final Glyph glyph = draw.glyph();
      if (!glyph.visible()) {
        continue;
      }
      final float boldOffset = draw.bold() ? glyph.boldOffset() : 0f;
      if (draw.shadowColour() != 0) {
        final float offset = glyph.shadowOffset();
        drawGlyph(canvas, glyph, draw.x() + offset, draw.y() + offset, draw.shadowColour(), draw.bold(), draw.italic());
        if (draw.bold()) {
          drawGlyph(canvas, glyph, draw.x() + boldOffset + offset, draw.y() + offset, draw.shadowColour(), true,
              draw.italic());
        }
      }
      drawGlyph(canvas, glyph, draw.x(), draw.y(), draw.colour(), draw.bold(), draw.italic());
      if (draw.bold()) {
        drawGlyph(canvas, glyph, draw.x() + boldOffset, draw.y(), draw.colour(), true, draw.italic());
      }
    }

    for (EffectDraw effect : effects) {
      if (effect.shadowColour() != 0) {
        final float offset = effect.shadowOffset();
        canvas.fill(effect.x0() + offset, effect.y0() + offset, effect.x1() + offset, effect.y1() + offset,
            effect.shadowColour());
      }
      canvas.fill(effect.x0(), effect.y0(), effect.x1(), effect.y1(), effect.colour());
    }
  }

  private static void drawGlyph(Canvas canvas, Glyph glyph, float x, float y, int colour, boolean bold,
                                boolean italic)
  {
    final float up = glyph.top();
    final float down = glyph.bottom();
    final float shearTop = italic ? 1f - 0.25f * up : 0f;
    final float shearBottom = italic ? 1f - 0.25f * down : 0f;
    final float thickness = bold ? 0.1f : 0f;
    canvas.texturedQuad(glyph.texture(),
        glyph.u(), glyph.v(), glyph.u() + glyph.texelWidth(), glyph.v() + glyph.texelHeight(),
        x + glyph.left() - thickness, y + up - thickness, x + glyph.right() + thickness, y + down + thickness,
        shearTop, shearBottom, colour);
  }

  static int textColour(TextColor color, int base) {
    return color == null ? base : (base & 0xFF000000) | (color.value() & 0xFFFFFF);
  }

  static int shadowColour(Style style, int textColour, boolean dropShadow) {
    final ShadowColor shadow = style.shadowColor();
    if (shadow != null) {
      final float textAlpha = (textColour >>> 24) / 255f;
      if (textAlpha != 1f) {
        final int alpha = (int) Math.floor(textAlpha * (shadow.alpha() / 255f) * 255f);
        return alpha << 24 | (shadow.value() & 0xFFFFFF);
      }
      return shadow.value();
    }
    if (!dropShadow) {
      return 0;
    }
    return (textColour & 0xFF000000)
        | (int) ((textColour >> 16 & 0xFF) * 0.25f) << 16
        | (int) ((textColour >> 8 & 0xFF) * 0.25f) << 8
        | (int) ((textColour & 0xFF) * 0.25f);
  }

  /**
   * Splits text into lines no wider than {@code maxWidth}, breaking at {@code \n} and at the last space before the
   * limit, like vanilla's {@code Font#split}.
   */
  public static @NotNull List<StyledText> split(@NotNull Fonts fonts, @NotNull StyledText text, int maxWidth) {
    final List<StyledText.Run> parts = new ArrayList<>();
    for (StyledText.Run run : text.runs()) {
      if (!run.text().isEmpty()) {
        parts.add(run);
      }
    }

    final List<StyledText> lines = new ArrayList<>();
    boolean endedWithNewline = false;
    boolean again = true;
    while (again) {
      again = false;
      final LineBreakFinder finder = new LineBreakFinder(fonts, maxWidth);
      for (StyledText.Run part : parts) {
        if (!LegacyFormatting.iterate(part.text(), part.style(), finder)) {
          final int position = finder.lineBreak;
          final char c = charAt(parts, position);
          final boolean newline = c == '\n';
          endedWithNewline = newline;
          lines.add(splitAt(parts, position, newline || c == ' ' ? 1 : 0, finder.lineBreakStyle));
          again = true;
          break;
        }
        finder.offset += part.text().length();
      }
    }

    if (!parts.isEmpty()) {
      lines.add(new StyledText(parts));
    }
    else if (endedWithNewline) {
      lines.add(StyledText.EMPTY);
    }
    return lines;
  }

  private static char charAt(List<StyledText.Run> parts, int position) {
    int remaining = position;
    for (StyledText.Run part : parts) {
      if (remaining < part.text().length()) {
        return part.text().charAt(remaining);
      }
      remaining -= part.text().length();
    }
    return '\0';
  }

  private static StyledText splitAt(List<StyledText.Run> parts, int position, int skip, Style splitStyle) {
    final List<StyledText.Run> line = new ArrayList<>();
    final ListIterator<StyledText.Run> iterator = parts.listIterator();
    int remaining = position;
    boolean found = false;
    while (iterator.hasNext()) {
      final StyledText.Run part = iterator.next();
      final String text = part.text();
      final int length = text.length();
      if (!found) {
        if (remaining > length) {
          line.add(part);
          iterator.remove();
          remaining -= length;
        }
        else {
          final String head = text.substring(0, remaining);
          if (!head.isEmpty()) {
            line.add(new StyledText.Run(head, part.style()));
          }
          remaining += skip;
          found = true;
        }
      }
      if (found) {
        if (remaining <= length) {
          final String tail = text.substring(remaining);
          if (tail.isEmpty()) {
            iterator.remove();
          }
          else {
            iterator.set(new StyledText.Run(tail, splitStyle));
          }
          break;
        }
        iterator.remove();
        remaining -= length;
      }
    }
    return new StyledText(line);
  }

  private static final class LineBreakFinder implements LegacyFormatting.CharSink {
    private final Fonts fonts;
    private final float maxWidth;
    int lineBreak = -1;
    Style lineBreakStyle = Style.empty();
    int offset;
    private boolean hadNonZeroWidthChar;
    private float width;
    private int lastSpace = -1;
    private Style lastSpaceStyle = Style.empty();

    LineBreakFinder(Fonts fonts, float maxWidth) {
      this.fonts = fonts;
      this.maxWidth = maxWidth;
    }

    @Override
    public boolean accept(int index, @NotNull Style style, int codepoint) {
      final int position = index + this.offset;
      if (codepoint == '\n') {
        return this.finish(position, style);
      }
      if (codepoint == ' ') {
        this.lastSpace = position;
        this.lastSpaceStyle = style;
      }
      final float advance = this.fonts.font(style.font()).glyph(codepoint)
          .advance(style.hasDecoration(TextDecoration.BOLD));
      this.width += advance;
      if (this.hadNonZeroWidthChar && this.width > this.maxWidth) {
        return this.lastSpace != -1 ? this.finish(this.lastSpace, this.lastSpaceStyle) : this.finish(position, style);
      }
      this.hadNonZeroWidthChar |= advance != 0f;
      return true;
    }

    private boolean finish(int position, Style style) {
      this.lineBreak = position;
      this.lineBreakStyle = style;
      return false;
    }
  }
}
