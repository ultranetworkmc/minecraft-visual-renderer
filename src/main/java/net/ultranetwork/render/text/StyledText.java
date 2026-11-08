package net.ultranetwork.render.text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.flattener.FlattenerListener;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;

/**
 * A component flattened into plain-text runs with their fully inherited styles. Legacy {@code §} codes inside a run
 * are kept and applied while rendering, as vanilla does.
 */
public record StyledText(@NotNull List<Run> runs) {
  public static final StyledText EMPTY = new StyledText(List.of());

  public record Run(@NotNull String text, @NotNull Style style) {
  }

  public StyledText {
    runs = List.copyOf(runs);
  }

  public static @NotNull StyledText of(@NotNull Component component, @NotNull ComponentFlattener flattener) {
    final List<Run> runs = new ArrayList<>();
    final Deque<Style> styles = new ArrayDeque<>();
    styles.push(Style.empty());
    flattener.flatten(component, new FlattenerListener() {
      @Override
      public void pushStyle(@NotNull Style style) {
        styles.push(style.merge(styles.peek(), Style.Merge.Strategy.IF_ABSENT_ON_TARGET));
      }

      @Override
      public void component(@NotNull String text) {
        if (!text.isEmpty()) {
          runs.add(new Run(text, styles.peek()));
        }
      }

      @Override
      public void popStyle(@NotNull Style style) {
        if (styles.size() > 1) {
          styles.pop();
        }
      }
    });
    return new StyledText(runs);
  }

  public static @NotNull StyledText plain(@NotNull String text) {
    return text.isEmpty() ? EMPTY : new StyledText(List.of(new Run(text, Style.empty())));
  }

  public boolean isEmpty() {
    return this.runs.isEmpty();
  }

  /**
   * @return the concatenated raw text, like vanilla's {@code Component#getString()}
   */
  public @NotNull String plainText() {
    final StringBuilder builder = new StringBuilder();
    for (Run run : this.runs) {
      builder.append(run.text());
    }
    return builder.toString();
  }
}
