package net.ultranetwork.render.paper;

import io.papermc.paper.text.PaperComponents;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public final class PaperSupport {

  private PaperSupport() {
  }

  /**
   * Paper's flattener resolves translatable components with the server's English translations.
   */
  public static @NotNull ComponentFlattener flattener() {
    return PaperComponents.flattener();
  }
}
