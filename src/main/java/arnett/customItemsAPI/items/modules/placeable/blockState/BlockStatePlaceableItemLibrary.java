package arnett.customItemsAPI.items.modules.placeable.blockState;

import arnett.customItemsAPI.items.modules.callbacks.annotations.ItemEvent;
import arnett.customItemsAPI.items.modules.placeable.BasePlaceableItemModule;
import arnett.customItemsAPI.items.ItemLibrary;
import arnett.customItemsAPI.CustomItemsAPI;
import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public abstract class BlockStatePlaceableItemLibrary extends ItemLibrary implements BasePlaceableItemModule {


    //region Properties

    /*=================================================================================================
                        -  Properties  -
    =================================================================================================*/

    public abstract BlockData overridedBlockData();

    @Override
    public Sound placementSound() {
        return null;
    }

    //endregion


    //region Optional Properties

    /*=================================================================================================
                        -  Optional Properties  -
    =================================================================================================*/


    /**
     * @return should this placed block track placed data
     */
    public boolean doPlacedDataTracking(){return true;}


    //endregion


    //region Placement

    /*=================================================================================================
                        -  Placement  -
    =================================================================================================*/


    @Override
    public boolean place(Block toBeReplaced, BlockState oldBlockState, BlockFace faceClicked, ItemStack itemPlaced, Player placer, Cancellable placementEvent, boolean alreadyDroppedItem) {

        //the item was placed
        //replace the actual block with the kind we want
        toBeReplaced.setBlockData(overridedBlockData());

        //spawn the item's Display
        Location placeSpot = toBeReplaced.getLocation();

        if(doPlacedDataTracking())
        {
            //create the block's pdc
            PersistentDataContainer customBlockData = new CustomBlockData(toBeReplaced, CustomItemsAPI.singleton);

            //set the generic tag to tell it is a custom block
            customBlockData.set(ItemLibrary.customItemTag, PersistentDataType.STRING, identifier().toString());

            //set this as the item (boolean and true don't really matter here we just check that it has this later)
            customBlockData.set(identifier(), PersistentDataType.BOOLEAN, true);

            var placementInfo = getPlacementData(placeSpot, placer, faceClicked);

            //set the placement direction
            customBlockData.set(placementDirectionNamespace, PersistentDataType.INTEGER, placementInfo.faceOn().ordinal());

            //call modify placed data
            if(doPlacedDataTracking())
                modifyNewPlacementItemData(customBlockData, itemPlaced, placer);
        }


        //play place sound
        var placementSound = placementSound();
        if (placementSound != null)
            placeSpot.getWorld().playSound(placeSpot, placementSound, 1, 1);



        return true;
    }


    //endregion


    //region Helper

    /*=================================================================================================
                        -  Helper  -
    =================================================================================================*/

    public boolean isItem(Block block)
    {
        //start with general material check
        if(!block.getType().equals(overridedBlockData().getMaterial()))
            return false;

        //then check if the block has a pdc
        if(doPlacedDataTracking())
        {
            if(!CustomBlockData.hasCustomBlockData(block, CustomItemsAPI.singleton))
            {
                return false;
            }

            //only then check if it has the specific namespace
            return new CustomBlockData(block, CustomItemsAPI.singleton).has(identifier());
        }
        else
        {
            return true;
        }
    }

    //endregion


    //region Events

    /*=================================================================================================
                        -  Events  -
    =================================================================================================*/

    @ItemEvent
    public void onBlockBroken(BlockBreakEvent e)
    {}

    @ItemEvent
    public void onBlockInteracted(PlayerInteractEvent e)
    {}

    /**
     * Called once an entity explodes with this in the radius
     * @param e the Explosion Bukkit event
     */
    @ItemEvent
    public void onEntityExplode(EntityExplodeEvent e)
    {}

    /**
     * Called once a block explodes with this in the radius, by public just calls onExplode
     * @param e the Explosion Bukkit event
     */
    @ItemEvent
    public void onBlockExplode(BlockExplodeEvent e)
    {

    }

    /**
     * Called once an entity explodes with this in the radius
     * @param e the Explosion Bukkit event
     * @param customBlock The custom block which is set to be exploded
     */
    @ItemEvent("block")
    public void onEntityExplode(EntityExplodeEvent e, Block customBlock) {}

    /**
     * Called once a block explodes with this in the radius, by public just calls onExplode
     * @param e the Explosion Bukkit event
     * @param customBlock The custom block which is set to be exploded
     */
    @ItemEvent("block")
    public void onBlockExplode(BlockExplodeEvent e, Block customBlock) {}


    //endregion



    //region Logic by Event

    /*=================================================================================================
                        -  Logic by Event  -
    =================================================================================================*/

    @ItemEvent
    public void broken(BlockBreakEvent e)
    {
        if(e.isCancelled())
            return;

        //stop the item drop and handle it ourselves
        e.setDropItems(false);
        naturalBlockBreak(e.getBlock(), e.getPlayer().getGameMode() != GameMode.CREATIVE);

        //we handle the breaking ourselves so cancel the event from this point
        e.setCancelled(true);
    }


    //endregion


    //region Breaking

    /*=================================================================================================
                        -  Breaking  -
    =================================================================================================*/

    /**
     * Naturally removes the custom block and drops it's items. This does not call break Events
     * @param brokenBlock the custom block being broken
     */
    @Override
    public void naturalBlockBreak(Block brokenBlock, boolean dropItem)
    {
        if(isItem(brokenBlock))
            //first delete the block
            deleteBlock(brokenBlock);

        Location offsetLocation = brokenBlock.getLocation().clone().add(displayOffset());

        //show particles
        offsetLocation.getWorld().spawnParticle(
                Particle.BLOCK,
                offsetLocation,
                30,
                0.3,
                0.3,
                0.3,
                0.1f,
                breakParticleMaterial().createBlockData()
        );

        //play the break sound
        offsetLocation.getWorld().playSound(
                offsetLocation,
                breakSound(),
                1f,
                1f
        );

        //then drop the block item if set to
        if(dropItem)
        {
            var world = brokenBlock.getWorld();

            //generic drops
            for (ItemStack droppedItem : droppedItems(brokenBlock)) {
                world.dropItemNaturally(offsetLocation, droppedItem);
            }
        }
    }

    public void deleteBlock(Block block)
    {
        if(doPlacedDataTracking())
        {
            CustomBlockData cbd = new CustomBlockData(block, CustomItemsAPI.singleton);
            cbd.clear();
        }
        block.setType(Material.AIR);
    }


    //endregion


    //region Placement Data

    /*=================================================================================================
                        -  Placement Data  -
    =================================================================================================*/

    @Override
    public PersistentDataContainer getPlacedItemData(Block placedItem) {
        if(!doPlacedDataTracking())
            return null;

        return new CustomBlockData(placedItem, CustomItemsAPI.singleton);
    }

    //endregion
}
