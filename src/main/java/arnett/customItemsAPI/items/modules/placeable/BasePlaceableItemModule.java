package arnett.customItemsAPI.items.modules.placeable;

import arnett.customItemsAPI.items.Directionality;
import arnett.customItemsAPI.CustomItemsAPI;
import arnett.customItemsAPI.items.modules.ItemModule;
import com.jeff_media.customblockdata.CustomBlockData;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

/**
 * Abstract interface intended to be defined by inheriting interfaces not extended by a class
 */
public interface BasePlaceableItemModule extends ItemModule {

    //region Abstract Properties

    /*=================================================================================================
                    -  Abstract Properties  -
    =================================================================================================*/


    /**
     * @return Namespaced Key which leads to the block display's resource in a resource pack
     */
    NamespacedKey displayModelKey();

    /**
     * @return The reaction this block has when pushed by a Piston
     */
    PistonMoveReaction pistonPushable();

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

    //endregion


    //region Optional Properties

    /*=================================================================================================
                    -  Optional Properties  -
    =================================================================================================*/

    /**
     * @return The material source for the particles which appear when broken (defaults to the base material)
     */
    default Material breakParticleMaterial()
    {
        return placementMaterial();
    }

    /**
     * @return Key to item resource to be used as item display model when placed on wall
     * (defaults to displayModelKey())
     */
    default NamespacedKey wallDisplayModelKey()
    {
        return displayModelKey();
    }

    /**
     * @return Key to item resource to be used as item display model when placed on wall
     * (defaults to displayModelKey())
     */
    default NamespacedKey roofDisplayModelKey()
    {
        return displayModelKey();
    }

    /**
     * Defines the offset to place the display model at. Default at .5, .5, .5
     * @return value
     */
    default Vector displayModelOffset() {
        return new Vector(.5, .5, .5);
    }

    /**
     * @return should this block resist explosions (defaults to false)
     */
    default boolean explosionResistant(){return false;}

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


    //todo move to custom Display module
    //region placement

    /*=================================================================================================
                        -  placement  -
    =================================================================================================*/

    /**
     * Call to try placing this block with the specified parameters
     * @param blockReplaced The block which was replaced with the placed block
     * @param faceClicked Face of the block which was clicked in the process of placing this
     * @param itemPlaced The item which placed
     * @param placementEvent generic cancelable placement event
     * @return was the placement a success
     */
    boolean tryPlace(Block blockReplaced, BlockFace faceClicked, ItemStack itemPlaced, Player placer, Cancellable placementEvent);

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
                rotation.mul(PlacementHelper.placeWallDirectional(player, against));
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
     * Called to run middle click pick item logic (currently only works for blocks)
     * @param e pick item event
     */
    //todo make copy for interaction types aswell later
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

    /**
     * Called whenever this item is dropped from a block breaking
     */
    void onPlaceableDropItem(Item droppedItem);

    /**
     * Called whenever this block is caught in a block physics update
     * @param e BlockPhysicsEvent
     */
    void onBlockPhysicsUpdate(BlockPhysicsEvent e);

    /**
     * Called whenever a placement of this block is successful
     * @param blockReplaced The block which was replaced with the placed block
     * @param itemPlaced The item which placed
     * @param placer Player placing the block
     */
    void onPlaced(Block blockReplaced, ItemStack itemPlaced, Player placer);


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


    /**
     * Call to drop an instance of this item at the given location
     */
    default void dropPlaceableItem(Location breakSpot)
    {
        //drop the item of the base material and call on BlockDrop item for consistency
        //because this is what it would normally look like if the player broke a block
        breakSpot.getWorld().dropItemNaturally(
                breakSpot,
                new ItemStack(getLibrary().getItem()),
                this::onPlaceableDropItem
        );
    }

    //endregion

}
