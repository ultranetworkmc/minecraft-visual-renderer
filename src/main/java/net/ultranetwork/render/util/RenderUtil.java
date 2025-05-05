package net.ultranetwork.render.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RenderUtil {
  public static final Color DEFAULT_TEXT_COLOUR = Color.WHITE;
  public static final Font MINECRAFT_FONT;

  private static final Set<Character> NO_BOLD_OFFSET_CHARS = Set.of( // todo maybe just calculate how fat the character is in font instead?
      ':', '.', ',', ';', '\'', '`', '!', '|'
  );

  static {
    Font loadedFont;

    try (InputStream inputStream = RenderUtil.class.getResourceAsStream("/__minecraft_font.ttf")) {
      if (inputStream == null) {
        throw new IOException("Could not find minecraft_font.ttf in resources");
      }

      loadedFont = Font.createFont(Font.TRUETYPE_FONT, inputStream).deriveFont(16f);
      GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(loadedFont);
    }
    catch (IOException | FontFormatException e) {
      System.err.println("no font using default monoscaped");
      e.printStackTrace();
      loadedFont = new Font(Font.MONOSPACED, Font.PLAIN, 16);
    }

    MINECRAFT_FONT = loadedFont;
  }

  public static void applyMinecraftRenderingHints(Graphics2D graphics) {
    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
  }

  /**
   * @param textColour
   * @param defaultColour The default AWT colour to use if textColour is nul
   * @return The corresponding AWT colour
   */
  @NotNull
  public static Color getAwtColour(
      @Nullable TextColor textColour,
      @NotNull Color defaultColour
  ) {
    return (textColour != null) ? new Color(textColour.value()) : defaultColour;
  }

  /**
   * @param metrics   The FontMetrics for the font being used
   * @param component The Component to measure
   * @return The width of the component in pixels
   */
  public static int calculateComponentWidth(
      @NotNull FontMetrics metrics,
      @Nullable Component component
  ) {
    if (component == null || Component.empty().equals(component)) {
      return 0;
    }

    int width = 0;
    if (component instanceof TextComponent textComponent) {
      final String text = textComponent.content();

      if (!(text.isEmpty())) {
        width += metrics.stringWidth(text);
      }
    }

    for (Component child : component.children()) {
      width += calculateComponentWidth(metrics, child);
    }

    return width;
  }

  /**
   * @param graphics      The Graphics2D context.
   * @param component     The Component to draw.
   * @param x             The starting X coordinate.
   * @param y             The baseline Y coordinate for the text.
   * @param defaultColour The default text colour if the component has no colour specified.
   * @param shadowColour  The colour to use for the text shadow.
   * @return The total pixel width drawn
   */
  public static int drawAdventureComponent(
      @NotNull Graphics2D graphics,
      @Nullable Component component,
      int x,
      int y,
      @NotNull Color defaultColour,
      @NotNull Color shadowColour
  ) {
    if (component == null) {
      return 0;
    }

    final FontMetrics fm = graphics.getFontMetrics();
    final Style style = component.style();
    final Color colour = getAwtColour(style.color(), defaultColour);
    final boolean isBold = style.hasDecoration(TextDecoration.BOLD);
    int currentX = x;
    int actualWidthDrawn = 0;

    if (component instanceof TextComponent textComponent) {
      final String text = textComponent.content();

      for (char character : text.toCharArray()) {
        final String charStr = String.valueOf(character);
        final int charWidth = fm.stringWidth(charStr);

        // Determine if bold offset should be skipped for this character
        final boolean skipBoldOffset = NO_BOLD_OFFSET_CHARS.contains(character);

        // Draw shadow first
        graphics.setColor(shadowColour);
        graphics.drawString(charStr, currentX + 1, y + 1);

        // Apply extra shadow offset only if bold AND not skipping offset
        if (isBold && !skipBoldOffset) {
          graphics.drawString(charStr, currentX + 2, y + 1);
        }

        // Draw text
        graphics.setColor(colour);
        graphics.drawString(charStr, currentX, y);

        // Apply bold offset draw only if bold AND not skipping offset
        if (isBold && !skipBoldOffset) {
          // Draw again slightly offset for visual bold effect
          graphics.drawString(charStr, currentX + 1, y);
        }

        currentX += charWidth; // Advance X ONLY by the characters metric width
        actualWidthDrawn += charWidth;
      }
    }

    // Recursively draw children, updating currentX
    for (Component child : component.children()) {
      final Color childDefaultColour = child.style().color() == null ? colour : getAwtColour(child.style().color(), Color.WHITE);
      final Style childStyle = child.style();
      final boolean childBoldExplicit = childStyle.decoration(TextDecoration.BOLD) != TextDecoration.State.NOT_SET;
      final boolean childShouldBeBold = childBoldExplicit ? childStyle.hasDecoration(TextDecoration.BOLD) : isBold;
      final Component effectiveChild = child.style(s -> s.decoration(TextDecoration.BOLD, childShouldBeBold));
      final int childWidth = drawAdventureComponent(graphics, effectiveChild, currentX, y, childDefaultColour, shadowColour);

      currentX += childWidth;
      actualWidthDrawn += childWidth;
    }

    // Return the actual calculated width based on metrics, not the final currentX which might include visual bold offsets
    return actualWidthDrawn;
  }
}