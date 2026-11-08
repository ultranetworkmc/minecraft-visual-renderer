package net.ultranetwork.render.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.ScoreComponent;
import net.kyori.adventure.text.SelectorComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a {@link ComponentFlattener} that resolves translatable and keybind components the way the vanilla client
 * does, given a translation lookup (normally a language file such as {@code en_us.json}).
 */
public final class VanillaFlattener {
  private static final Pattern FORMAT = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

  // Default key bindings; letter keys display as the upper-case key, others via their translation key.
  private static final Map<String, String> DEFAULT_KEYS = Map.ofEntries(
      Map.entry("key.attack", "key.mouse.left"),
      Map.entry("key.use", "key.mouse.right"),
      Map.entry("key.pickItem", "key.mouse.middle"),
      Map.entry("key.forward", "W"),
      Map.entry("key.left", "A"),
      Map.entry("key.back", "S"),
      Map.entry("key.right", "D"),
      Map.entry("key.jump", "key.keyboard.space"),
      Map.entry("key.sneak", "key.keyboard.left.shift"),
      Map.entry("key.sprint", "key.keyboard.left.control"),
      Map.entry("key.drop", "Q"),
      Map.entry("key.inventory", "E"),
      Map.entry("key.chat", "T"),
      Map.entry("key.playerlist", "key.keyboard.tab"),
      Map.entry("key.command", "/"),
      Map.entry("key.socialInteractions", "P"),
      Map.entry("key.screenshot", "key.keyboard.f2"),
      Map.entry("key.togglePerspective", "key.keyboard.f5"),
      Map.entry("key.smoothCamera", "key.keyboard.unknown"),
      Map.entry("key.fullscreen", "key.keyboard.f11"),
      Map.entry("key.spectatorOutlines", "key.keyboard.unknown"),
      Map.entry("key.swapOffhand", "F"),
      Map.entry("key.saveToolbarActivator", "C"),
      Map.entry("key.loadToolbarActivator", "X"),
      Map.entry("key.advancements", "L"),
      Map.entry("key.hotbar.1", "1"),
      Map.entry("key.hotbar.2", "2"),
      Map.entry("key.hotbar.3", "3"),
      Map.entry("key.hotbar.4", "4"),
      Map.entry("key.hotbar.5", "5"),
      Map.entry("key.hotbar.6", "6"),
      Map.entry("key.hotbar.7", "7"),
      Map.entry("key.hotbar.8", "8"),
      Map.entry("key.hotbar.9", "9")
  );

  private VanillaFlattener() {
  }

  /**
   * @param translations translation key to format string, returning {@code null} for unknown keys
   */
  public static @NotNull ComponentFlattener create(@NotNull Function<String, @Nullable String> translations) {
    return ComponentFlattener.builder()
        .mapper(TextComponent.class, TextComponent::content)
        .complexMapper(TranslatableComponent.class, (component, out) -> translate(component, translations, out))
        .mapper(KeybindComponent.class, keybind -> keybindName(keybind.keybind(), translations))
        .mapper(ScoreComponent.class, score -> "")
        .mapper(SelectorComponent.class, SelectorComponent::pattern)
        .complexMapper(ObjectComponent.class, (object, out) -> {
          if (object.fallback() != null) {
            out.accept(object.fallback());
          }
        })
        .unknownMapper(component -> "")
        .build();
  }

  private static String keybindName(String keybind, Function<String, String> translations) {
    final String key = DEFAULT_KEYS.get(keybind);
    if (key == null) {
      final String translated = translations.apply(keybind);
      return translated != null ? translated : keybind;
    }
    if (!key.startsWith("key.")) {
      return key.toUpperCase(Locale.ROOT);
    }
    final String translated = translations.apply(key);
    return translated != null ? translated : key;
  }

  private static void translate(TranslatableComponent component, Function<String, String> translations,
                                Consumer<Component> out)
  {
    String format = translations.apply(component.key());
    if (format == null) {
      format = component.fallback() != null ? component.fallback() : component.key();
    }

    List<Component> parts = new ArrayList<>();
    try {
      final Matcher matcher = FORMAT.matcher(format);
      int nextArgument = 0;
      int position = 0;
      while (matcher.find(position)) {
        final int start = matcher.start();
        final int end = matcher.end();
        if (start > position) {
          parts.add(literal(format.substring(position, start)));
        }
        final String type = matcher.group(2);
        if ("%".equals(type) && "%%".equals(format.substring(start, end))) {
          parts.add(Component.text("%"));
        }
        else {
          if (!"s".equals(type)) {
            throw new IllegalArgumentException("Unsupported format " + format.substring(start, end));
          }
          final String index = matcher.group(1);
          parts.add(argument(component, index != null ? Integer.parseInt(index) - 1 : nextArgument++));
        }
        position = end;
      }
      if (position < format.length()) {
        parts.add(literal(format.substring(position)));
      }
    }
    catch (IllegalArgumentException e) {
      parts = List.of(Component.text(format));
    }
    parts.forEach(out);
  }

  private static Component literal(String text) {
    if (text.indexOf('%') != -1) {
      throw new IllegalArgumentException("Stray % in " + text);
    }
    return Component.text(text);
  }

  private static Component argument(TranslatableComponent component, int index) {
    final List<TranslationArgument> arguments = component.arguments();
    if (index < 0 || index >= arguments.size()) {
      throw new IllegalArgumentException("Missing argument " + index);
    }
    final Object value = arguments.get(index).value();
    if (value instanceof Component argument) {
      return argument;
    }
    return Component.text(String.valueOf(value));
  }
}
