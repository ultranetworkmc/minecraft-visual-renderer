package net.ultranetwork.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.ultranetwork.render.playerlist.PlayerListEntry;
import net.ultranetwork.render.playerlist.PlayerListRenderer;
import net.ultranetwork.render.skin.SkinProvider;
import net.ultranetwork.render.tooltip.TooltipRenderer;
import net.ultranetwork.render.util.ImageUtil;

/**
 * Sample renders shared by the snapshot and vanilla-asset tests.
 */
final class Samples {
  static final Path OUTPUT = Path.of("build", "test-output");

  private Samples() {
  }

  static BufferedImage tooltip(RenderEnvironment env) {
    return TooltipRenderer.builder()
        .environment(env)
        .line(Component.text("Diamond Sword", NamedTextColor.AQUA))
        .line(Component.text("Sharpness V", NamedTextColor.GRAY))
        .line(Component.empty())
        .line(Component.text("When in Main Hand:", NamedTextColor.GRAY))
        .line(Component.text(" 9 Attack Damage", NamedTextColor.DARK_GREEN))
        .line(Component.text("Bold", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
            .append(Component.text(" italic", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.ITALIC)))
        .line(Component.text("under", NamedTextColor.YELLOW).decorate(TextDecoration.UNDERLINED)
            .append(Component.text(" struck", NamedTextColor.RED).decorate(TextDecoration.STRIKETHROUGH)))
        .line(Component.text("§9Legacy §lcodes"))
        .render();
  }

  static BufferedImage tabList(RenderEnvironment env) {
    return PlayerListRenderer.builder()
        .environment(env)
        .skinProvider(SkinProvider.none())
        .showHeads(env.usesVanillaAssets())
        .header(Component.text("My Server", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
        .footer(Component.text("play.example.net", NamedTextColor.GRAY))
        .entry(PlayerListEntry.builder("Notch").uuid(new UUID(1, 2)).latency(40).score(12).build())
        .entry(PlayerListEntry.builder("jeb_").uuid(new UUID(3, 4)).latency(320).score(7)
            .displayName(Component.text("[Mod] ", NamedTextColor.GREEN).append(Component.text("jeb_"))).build())
        .entry(PlayerListEntry.builder("Dinnerbone").uuid(new UUID(5, 6)).latency(700).score(3).build())
        .entry(PlayerListEntry.builder("Watcher").uuid(new UUID(7, 8)).spectator(true).latency(-1).build())
        .entry(PlayerListEntry.builder("Laggy").uuid(new UUID(9, 10)).latency(1500).score(0).build())
        .build().join();
  }

  static void write(String name, BufferedImage image) throws IOException {
    ImageUtil.writePng(image, OUTPUT.resolve(name + ".png"));
  }
}
