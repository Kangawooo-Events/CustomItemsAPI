package arnett.customItemsAPI.Helpers;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.*;

public class BlockHelper {

    /**
     * Returns wether a block is usable
     * (defined as it having some mechanic of when you right-click it,
     * you could interact with it rather than use the item in your hand)
     * @param block
     * @return
     */
    public static boolean isUsable(Block block)
    {
        BlockData blockData = block.getBlockData();
        Material type = block.getType();
        return  block != null && (
                blockData instanceof Switch ||
                        blockData instanceof Shelf ||
                        blockData instanceof Container ||
                        blockData instanceof Gate ||
                        blockData instanceof Door ||
                        blockData instanceof TrapDoor ||
                        blockData instanceof CommandBlock ||
                        blockData instanceof Sign ||
                        blockData instanceof WallHangingSign ||
                        blockData instanceof CopperGolemStatue ||
                        blockData instanceof StructureBlock ||
                        (blockData instanceof Jukebox box && !box.hasRecord()) ||
                        type == Material.CRAFTER ||
                        type == Material.CRAFTING_TABLE ||
                        type == Material.ANVIL ||
                        type == Material.BEACON ||
                        type == Material.LECTERN ||
                        type == Material.TRAPPED_CHEST ||
                        type == Material.ENCHANTING_TABLE ||
                        type == Material.CAKE ||
                        type == Material.NOTE_BLOCK ||
                        type == Material.SWEET_BERRY_BUSH ||
                        type == Material.GLOW_BERRIES ||
                        type == Material.DRAGON_EGG ||
                        type == Material.DECORATED_POT ||
                        type == Material.DAYLIGHT_DETECTOR ||
                        type == Material.REPEATER ||
                        type == Material.COMPARATOR

        );
    }
}
