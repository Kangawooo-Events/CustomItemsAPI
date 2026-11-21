package arnett.customItemsAPI.items.modules.placeable.attachable;

import arnett.customItemsAPI.CustomItemsAPI;
import arnett.customItemsAPI.items.modules.callbacks.annotations.ItemEvent;
import arnett.customItemsAPI.items.modules.placeable.BasePlaceableItemModule;
import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.persistence.PersistentDataType;

public interface AttachmentPlacementModule extends BasePlaceableItemModule {

    /**
     * Called when the block instance at this custom item's placement is located at is updated by block physics
     * @param e BlockPhysicsEvent
     */
    @ItemEvent()
    default void attachmentBlockPhysicsUpdate(BlockPhysicsEvent e) {
        System.out.println("physicis!");
        System.out.println("breakable directionality! " + placementDirection(e.getBlock()));

        //is the block causing this check the one this is attached to
        if(placementDirection(e.getBlock()).getOppositeFace() == e.getBlock().getFace(e.getSourceBlock()))
        {
            //is attached block broken
            if(!e.getSourceBlock().isBuildable())
            {
                //block is no longer solid so break this
                naturalBlockBreak(e.getBlock(), true);
            }
        }
    }

    /**
     * Gets the placement direction of a placed custom block, mostly used for attached block in physics block updates
     * @param block custom block
     * @return direction placed
     */
    default BlockFace placementDirection(Block block)
    {
        Integer face = new CustomBlockData(block, CustomItemsAPI.singleton)
                .get(placementDirectionNamespace, PersistentDataType.INTEGER);

        if(face == null)
            return BlockFace.DOWN;

        return BlockFace.values()[face];
    }
}
