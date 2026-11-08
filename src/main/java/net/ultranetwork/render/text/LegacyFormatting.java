package net.ultranetwork.render.text;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Applies legacy {@code §} formatting codes found inside text, following vanilla's {@code StringDecomposer}.
 */
final class LegacyFormatting {
  static final char SECTION = '§';
  private static final String COLOR_CODES = "0123456789abcdef";
  private static final NamedTextColor[] COLORS = {
      NamedTextColor.BLACK, NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GREEN, NamedTextColor.DARK_AQUA,
      NamedTextColor.DARK_RED, NamedTextColor.DARK_PURPLE, NamedTextColor.GOLD, NamedTextColor.GRAY,
      NamedTextColor.DARK_GRAY, NamedTextColor.BLUE, NamedTextColor.GREEN, NamedTextColor.AQUA,
      NamedTextColor.RED, NamedTextColor.LIGHT_PURPLE, NamedTextColor.YELLOW, NamedTextColor.WHITE
  };

  @FunctionalInterface
  interface CharSink {
    /**
     * @param index position of the character within its run's raw text
     * @return {@code false} to stop iterating
     */
    boolean accept(int index, @NotNull Style style, int codepoint);
  }

  private LegacyFormatting() {
  }

  /**
   * @return {@code false} if the sink stopped early
   */
  static boolean iterate(@NotNull String text, @NotNull Style base, @NotNull CharSink sink) {
    Style style = base;
    final int length = text.length();
    for (int i = 0; i < length; i++) {
      final char c = text.charAt(i);
      if (c == SECTION) {
        if (i + 1 >= length) {
          break;
        }
        style = apply(style, base, text.charAt(i + 1));
        i++;
      }
      else if (Character.isHighSurrogate(c)) {
        if (i + 1 >= length) {
          if (!sink.accept(i, style, 0xFFFD)) {
            return false;
          }
          break;
        }
        final char low = text.charAt(i + 1);
        if (Character.isLowSurrogate(low)) {
          if (!sink.accept(i, style, Character.toCodePoint(c, low))) {
            return false;
          }
          i++;
        }
        else if (!sink.accept(i, style, 0xFFFD)) {
          return false;
        }
      }
      else if (!sink.accept(i, style, Character.isSurrogate(c) ? 0xFFFD : c)) {
        return false;
      }
    }
    return true;
  }

  private static Style apply(Style style, Style reset, char code) {
    final char lower = Character.toLowerCase(code);
    final int color = COLOR_CODES.indexOf(lower);
    if (color >= 0) {
      return style.color(COLORS[color])
          .decoration(TextDecoration.OBFUSCATED, false)
          .decoration(TextDecoration.BOLD, false)
          .decoration(TextDecoration.STRIKETHROUGH, false)
          .decoration(TextDecoration.UNDERLINED, false)
          .decoration(TextDecoration.ITALIC, false);
    }
    final @Nullable TextDecoration decoration = switch (lower) {
      case 'k' -> TextDecoration.OBFUSCATED;
      case 'l' -> TextDecoration.BOLD;
      case 'm' -> TextDecoration.STRIKETHROUGH;
      case 'n' -> TextDecoration.UNDERLINED;
      case 'o' -> TextDecoration.ITALIC;
      default -> null;
    };
    if (decoration != null) {
      return style.decoration(decoration, true);
    }
    return lower == 'r' ? reset : style;
  }
}
