package arnett.customItemsAPI.items.modules.placeable;

import arnett.customItemsAPI.items.Directionality;
import arnett.customItemsAPI.items.modules.ItemModule;
import arnett.customItemsAPI.items.modules.callbacks.ItemEventPriority;
import arnett.customItemsAPI.items.modules.callbacks.annotations.ItemEvent;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.List;

/**
 * Abstract interface intended to be defined by inheriting interfaces not extended by a class
 */
public interface BasePlaceableItemModule extends ItemModule {

    //region Abstract Properties

    /*=================================================================================================
                    -  Abstract Properties  -
    =================================================================================================*/


    /**
     * @return Placement directions this can be placed at
     */
    Directionality directionality();

    /**
     * @return Material type of placement
     */
    Material placementMaterial();

    /**
     * @return The sound played when breaking
     */
    Sound breakSound();

    /**
     * @return sound to play when placed
     */
    Sound placementSound();


    //endregion


    //region Optional Properties

    /*=================================================================================================
                    -  Optional Properties  -
    =================================================================================================*/

    /**
     * @return The reaction this block has when pushed by a Piston
     */
    default PistonMoveReaction pistonPushable(){return PistonMoveReaction.BLOCK;}

    /**
     * @return The material source for the particles which appear when broken (defaults to the base material)
     */
    default Material breakParticleMaterial()
    {
        return placementMaterial();
    }

    /**
     * Defines the offset to spawn display things at. Default at .5, .5, .5
     * @return value
     */
    default Vector displayOffset() {
        return new Vector(.5, .5, .5);
    }

    /**
     * @return should this block resist explosions (defaults to false)
     */
    default boolean explosionResistant(){return false;}

    /**
     * returns the dropped items when a custom placement is broken
     * @param fromBlock custom block where placement was broken
     * @return
     */
    default List<ItemStack> droppedItems(Block fromBlock)
    {
        return List.of(getLibrary().getItem());
    }

    //endregion


    //region Static Properties

    /*=================================================================================================
                    -  Static Properties  -
    =================================================================================================*/

    /**
     * Namespaced used by CustomBlockData & Interactors to store the UUID of their display Entity
     */
    NamespacedKey DisplayLinkNamespace = new NamespacedKey("customitems", "linkeddisplay");

    /**
     * Namespaced used by Explosive blocks to define their range
     */
    NamespacedKey explosiveRangeNamespace = new NamespacedKey("customitems", "explosiverange");

    /**
     * Namespaced used by Directional blocks to define the direction they are placed
     */
    NamespacedKey placementDirectionNamespace = new NamespacedKey("customitems", "placementdirection");

    //endregion


    //region placement

    /*=================================================================================================
                        -  placement  -
    =================================================================================================*/

    /**
     * Handles placement logic from an interaction event (non-placeable item used to place). Priority is LATE
     */
    @ItemEvent(value = "place", priority = ItemEventPriority.LATE)
    default void handleInteractionPlacement(PlayerInteractEvent e)
    {
        if (e.useItemInHand() == Event.Result.DENY)
            return;

        //cancel the interaction event since we handle it here
        e.setCancelled(true);

        //gets the block which is being set
        assert e.getClickedBlock() != null;
        Block placeAtBlock = e.getClickedBlock().isReplaceable() ?
                e.getClickedBlock() :
                e.getClickedBlock().getRelative(e.getBlockFace());

        //try to place
        if (place(placeAtBlock, placeAtBlock.getState(), e.getBlockFace(), e.getItem(), e.getPlayer(), e, false))
        {
            //if placement was successful then swing the hand and invoke onPlace
            e.getPlayer().swingHand(e.getHand());
            onPlaced(placeAtBlock, e.getItem(), e.getPlayer());
        }
    }

    /**
     * Handles placement logic from a place event (placeable item used to place). Priority is LATE
     */
    @ItemEvent(priority = ItemEventPriority.LATE)
    default void handleBlockPlacement(BlockPlaceEvent e)
    {
        if(e.isCancelled())
            return;

        Block placedBlock = e.getBlockPlaced();
        Block blockAgainst = e.getBlockAgainst();
        var replaced = e.getBlockReplacedState();

        place(placedBlock, replaced, blockAgainst.getFace(placedBlock), e.getItemInHand(), e.getPlayer(), e, true);
    }

    /**
     * Call to try placing this block with the specified parameters. THIS IS THE PLACEMENT LOGIC FUNCTION
     * @param blockReplaced The block which was replaced with the placed block
     * @param faceClicked Face of the block which was clicked in the process of placing this
     * @param itemPlaced The item which placed
     * @param placementEvent generic cancelable placement event
     * @return was the placement a success
     */
    boolean place(Block blockReplaced, BlockState oldBlockState, BlockFace faceClicked, ItemStack itemPlaced, Player placer, Cancellable placementEvent, boolean alreadyDroppedItem);



    /**
     * Gets the placement data based on the placer, location and face placed aginst
     * @param spot spot the item is being placed
     * @param player player who placed the item (used for location and direction)
     * @param against face the block was placed aginst
     * @return placement data for these parameters
     */
    default PlacementData getPlacementData(Location spot, Player player, BlockFace against)
    {
        //set the rotation according to player direction
        Location placedSpot = spot.clone();

        //used to track the item display's roll (used like if it needs to be flipped)
        Quaternionf rotation = null;

        switch (directionality()){

            case Directionality.NESW -> rotation = PlacementHelper.placeNESW(player, against);

            case Directionality.UD -> rotation = PlacementHelper.placeUD(player, against);

            case Directionality.NESWUD ->
            {
                rotation = PlacementHelper.placeNESW(player, against);
                rotation.mul(PlacementHelper.placeUD(player, against));
            }

            case Directionality.Wall ->
            {
                rotation = PlacementHelper.placeNESW(player, against);
                rotation.mul(new Quaternionf().rotateX(-(float)Math.PI/2));
                rotation.mul(PlacementHelper.placeWallDirectional(player, against));
            }

            case WallD -> {
                rotation = PlacementHelper.placeNESW(player, against);
                rotation.mul(PlacementHelper.placeWallD(against));
            }

            case WallUD -> {
                rotation = PlacementHelper.placeNESW(player, against);
                rotation.mul(PlacementHelper.placeWallUD(player, against));
            }

            case WallBlock -> {
                var data = PlacementHelper.placeNESWBlock(spot, player, against);

                if(data == null || data.getLeft() == null)
                    return null;

                rotation = data.getLeft();
                against = data.getRight();

                rotation.mul(new Quaternionf().rotateX(-(float)Math.PI/2));
                rotation.mul(PlacementHelper.placeWallDirectional(player, against));
            }

            case WallDBlock -> {
                //first check if that block exists, if so just place according to NESW
                if(against != BlockFace.DOWN && spot.getBlock().getRelative(against.getOppositeFace()).isSolid())
                {
                    rotation = PlacementHelper.placeNESW(player, against);
                }
                else
                {
                    //the block does not exist so we have to find one ourselves or return if none are found
                    //also pass failed as down so it doesn't get checked
                    var data = PlacementHelper.findBlockRot(spot, BlockFace.DOWN, false);

                    if(data == null || data.getLeft() == null)
                        return null;

                    rotation = data.getLeft();
                    against = data.getRight();
                }

                rotation.mul(PlacementHelper.placeWallUD(player, against));
                rotation.mul(PlacementHelper.placeWallDirectional(player, against));
            }

            case WallUDBlock -> {
                //first check if that block exists, if so just place according to NESW
                if(spot.getBlock().getRelative(against.getOppositeFace()).isSolid())
                {
                    rotation = PlacementHelper.placeNESW(player, against);
                }
                else
                {
                    //the block does not exist so we have to find one ourselves or return if none are found
                    //also pass failed as down so it doesn't get checked
                    var data = PlacementHelper.findBlockRot(spot, against, false);

                    if(data == null || data.getLeft() == null)
                        return null;

                    rotation = data.getLeft();
                    against = data.getRight();
                }

                rotation.mul(PlacementHelper.placeWallUD(player, against));
                rotation.mul(PlacementHelper.placeWallDirectional(player, against));
            }
        }

        //if no rotation spot was found
        if(rotation == null)
        {
            return null;
        }

        return new PlacementData(placedSpot, rotation, against);
    }


    //endregion


    //region Helpers

    /*=================================================================================================
                        -  Helpers  -
    =================================================================================================*/

    default boolean isWallFace(BlockFace face)
    {
        return !(face == BlockFace.DOWN || face == BlockFace.UP || face == BlockFace.SELF);
    }

    //endregion


    //region Events

    /*=================================================================================================
                        -  Events  -
    =================================================================================================*/


    /**
     * Called before this item is middle click picked and the pickItem() function has run on LATE Priotiry.
     */
    @ItemEvent
    default void onPickItem(PlayerPickItemEvent e){}

    /**
     * Called whenever this block is caught in a block physics update
     * @param e BlockPhysicsEvent
     */
    @ItemEvent
    default void onBlockPhysicsUpdate(BlockPhysicsEvent e){}


    /**
     * Called whenever a placement interaction is made with an instance of this item. runs before any placement
     * logic is done (logic is done on LATE priority, this is NORMAL)
     * @param e PlayerInteractEvent
     */
    @ItemEvent("place")
    default void onPlacementInteraction(PlayerInteractEvent e){}

    /**
     * Called whenever the placed block is broken, called before break logic is run
     * @param placedBlock placed block which was broken
     * @param breaker player who broke the block
     */
    default void onPlacementBroken(Block placedBlock, Entity breaker, Cancellable event)
    {}


    /**
     * Called whenever a placement of this block is successful (Not an Item Event, this has no event attached)
     * @param blockReplaced The block which was replaced with the placed block
     * @param itemPlaced The item which placed
     * @param placer Player placing the block
     */
    default void onPlaced(Block blockReplaced, ItemStack itemPlaced, Player placer){}


    //endregion


    //region Logic by Events

    /*=================================================================================================
                        -  Logic by Events  -
    =================================================================================================*/


    /**
     * Called by default when an explosion reaches this block.
     * Calls the naturalBreakBlock function if we are not explosion resistant
     */
    default void explodedLogic(Cancellable e, Block explodedBlock)
    {
        if(e.isCancelled())
            return;

        if(!explosionResistant())
        {
            //break it ourselves
            naturalBlockBreak(explodedBlock, true);
        }
    }


    //endregion


    //region Breaking

    /*=================================================================================================
                        -  Breaking  -
    =================================================================================================*/

    /**
     * Call to break this custom block naturally at the given block location
     * @param block block location of the custom placement
     * @param dropItem should this drop an item
     */
    void naturalBlockBreak(Block block, boolean dropItem);

    //endregion


    //region Picking

    /*=================================================================================================
                        -  Picking  -
    =================================================================================================*/


    /**
     * Called to run middle click pick item logic on LATE priority (currently only works for blocks)
     * @param e pick item event
     */
    //todo make copy for interaction types as well
    @ItemEvent(priority = ItemEventPriority.LATE)
    default void pickItem(PlayerPickItemEvent e)
    {
        if(e.isCancelled())
            return;

        //grab the library since we need it a few times
        var library = getLibrary();

        //cancel the event since we have to override it ourselves
        e.setCancelled(true);

        //if this item is already in the inventory then swap to it
        PlayerInventory playerInventory = e.getPlayer().getInventory();
        int containingSlot = library.findInInventory(playerInventory);

        if(containingSlot != -1)
        {
            //are we in one of the hotbar slots?
            if(containingSlot <= 8)
            {
                //we are so switch the held hand to where the item is
                playerInventory.setHeldItemSlot(containingSlot);
            }
            else
            {
                //it's somewhere else in the inventory
                ItemStack temp = playerInventory.getItem(containingSlot);
                playerInventory.setItem(containingSlot, playerInventory.getItemInMainHand());
                playerInventory.setItem(playerInventory.getHeldItemSlot(), temp);
            }

            return;
        }

        //item is not withing inventory already
        //so now give it only if they are in creative

        if(!e.getPlayer().getGameMode().equals(GameMode.CREATIVE))
            return;

        //find an empty hotbar slot
        int firstEmpty;
        for(firstEmpty = 0; firstEmpty < 9; firstEmpty ++)
        {
            ItemStack slotItem = playerInventory.getItem(firstEmpty);
            if(slotItem == null || slotItem.isEmpty())
                break;
        }

        //if we found an empty hotbar slot
        if(firstEmpty < 9)
        {
            playerInventory.setHeldItemSlot(firstEmpty);
        }

        e.getPlayer().getInventory().setItemInMainHand(library.getItem());
    }


    //endregion


    //region Placement data

    /*=================================================================================================
                        -  Placement data  -
    =================================================================================================*/


    /**
     * Gets the data container of the placed block, may return null if placed data tracking if fasle
     */
    PersistentDataContainer getPlacedItemData(Block placedItem);


    /**
     * Called to modify the placed item data
     * @param pdc placed Item data
     * @param placedItem item used to place
     */
    default void modifyNewPlacementItemData(PersistentDataContainer pdc, ItemStack placedItem, Player placer){}


    //endregion

}
