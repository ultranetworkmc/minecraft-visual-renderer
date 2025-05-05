package net.ultranetwork.render;

import java.io.File;
import net.ultranetwork.render.item.ItemImageRenderer;
import net.ultranetwork.render.util.ImageUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Main {
  public static void main(String[] args) {
    ItemImageRenderer.builder()
        .item(new ItemStack(Material.DIAMOND_PICKAXE))
        .build()
        .thenAccept(image -> {
          ImageUtil.saveImageToFile(image, new File("resources/output/asd.png"));
        });
  }
}