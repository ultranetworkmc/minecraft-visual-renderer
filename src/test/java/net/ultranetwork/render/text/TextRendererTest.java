package net.ultranetwork.render.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.ultranetwork.render.RenderEnvironment;
import org.junit.jupiter.api.Test;

class TextRendererTest {
  private final RenderEnvironment env = RenderEnvironment.fallback();

  private int width(Component component) {
    return TextRenderer.width(this.env, this.env.text(component));
  }

  @Test
  void measuresWithVanillaAdvances() {
    assertEquals(24, this.width(Component.text("Hello")));
    assertEquals(4, this.width(Component.text(" ")));
    assertEquals(2 + 2, this.width(Component.text("i!")));
  }

  @Test
  void boldAddsOnePixelPerCharacter() {
    assertEquals(6 + 1 + 2 + 1, this.width(Component.text("Hi").decorate(TextDecoration.BOLD)));
  }

  @Test
  void legacyCodesAreNotMeasuredButApplied() {
    assertEquals(this.width(Component.text("Hi").decorate(TextDecoration.BOLD)), this.width(Component.text("§lHi")));
    assertEquals(this.width(Component.text("Hi")), this.width(Component.text("§l§rHi")));
    assertEquals(this.width(Component.text("Hi")), this.width(Component.text("§l§cHi")));
  }

  @Test
  void childrenInheritParentStyle() {
    final Component nested = Component.text("H").decorate(TextDecoration.BOLD).append(Component.text("i"));
    assertEquals(this.width(Component.text("Hi").decorate(TextDecoration.BOLD)), this.width(nested));
  }

  @Test
  void shadowColourIsQuarterBrightness() {
    assertEquals(0xFF3F3F3F, TextRenderer.shadowColour(Style.empty(), 0xFFFFFFFF, true));
    assertEquals(0x90151515, TextRenderer.shadowColour(Style.empty(), 0x90555555, true));
    assertEquals(0, TextRenderer.shadowColour(Style.empty(), 0xFFFFFFFF, false));
    final Style custom = Style.style().shadowColor(ShadowColor.shadowColor(0xFF112233)).build();
    assertEquals(0xFF112233, TextRenderer.shadowColour(custom, 0xFFFFFFFF, true));
    assertEquals(0x80112233, TextRenderer.shadowColour(custom, 0x80FFFFFF, true));
    assertEquals(0x40112233, TextRenderer.shadowColour(
        Style.style().shadowColor(ShadowColor.shadowColor(0x80112233)).build(), 0x80FFFFFF, true));
  }

  @Test
  void textColourKeepsBaseAlpha() {
    assertEquals(0x90FF5555, TextRenderer.textColour(NamedTextColor.RED, 0x90FFFFFF));
  }

  @Test
  void splitsAtSpacesAndNewlines() {
    final List<StyledText> lines = TextRenderer.split(this.env, StyledText.plain("aaaa bbbb\ncc"), 30);
    assertEquals(List.of("aaaa", "bbbb", "cc"), lines.stream().map(StyledText::plainText).toList());
  }

  @Test
  void splitCarriesLegacyFormattingToNextLine() {
    final List<StyledText> lines = TextRenderer.split(this.env, StyledText.plain("§laaaa bbbb"), 30);
    assertEquals(2, lines.size());
    assertEquals(TextRenderer.width(this.env, StyledText.plain("§lbbbb")), TextRenderer.width(this.env, lines.get(1)));
  }

  @Test
  void translatesLikeVanilla() {
    final var flattener = VanillaFlattener.create(Map.of(
        "greeting", "Hello %s and %2$s",
        "percent", "100%%",
        "broken", "50% off")::get);
    final Component args = Component.translatable("greeting", Component.text("A"), Component.text("B"));
    assertEquals("Hello A and B", StyledText.of(args, flattener).plainText());
    assertEquals("100%", StyledText.of(Component.translatable("percent"), flattener).plainText());
    assertEquals("50% off", StyledText.of(Component.translatable("broken"), flattener).plainText());
    assertEquals("missing.key", StyledText.of(Component.translatable("missing.key"), flattener).plainText());
    assertEquals("Fallback", StyledText.of(Component.translatable("missing.key", "Fallback"), flattener).plainText());
    // A missing argument makes vanilla fall back to the raw format string.
    assertEquals("Hello %s and %2$s",
        StyledText.of(Component.translatable("greeting", Component.text("A")), flattener).plainText());
  }

  @Test
  void translationArgumentsInheritStyle() {
    final var flattener = VanillaFlattener.create(Map.of("k", "%s")::get);
    final StyledText text = StyledText.of(
        Component.translatable("k", NamedTextColor.RED, Component.text("x")), flattener);
    assertEquals(NamedTextColor.RED, text.runs().getFirst().style().color());
  }
}
