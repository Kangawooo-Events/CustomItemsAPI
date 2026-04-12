package arnett.customItemsAPI.CustomItems.CustomBlockTypes;

import arnett.customItemsAPI.CustomItems.ItemLibrary;
import arnett.customItemsAPI.CustomItems.Directionality;
import arnett.customItemsAPI.CustomItemsAPI;
import com.jeff_media.customblockdata.CustomBlockData;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public abstract class PlaceableLibrary extends ItemLibrary {

    public static NamespacedKey explosiveRangeNamespace = new NamespacedKey("customitems", "explosiverange");
    public static NamespacedKey placementDirectionNamespace = new NamespacedKey("customitems", "placementdirection");

    public abstract NamespacedKey getDisplayModelKey();
    public abstract PistonMoveReaction getPistonPushable();

    public NamespacedKey getWallDisplayModelKey()
    {
        return getDisplayModelKey();
    }

    public NamespacedKey getRoofDisplayModelKey()
    {
        return getDisplayModelKey();
    }

    public List<NamespacedKey> getAttachmentModelKeys()
    {
        return List.of();
    }

    /**
     * Defines the offset to place the display model at. Default at .5, .5, .5
     * @return value
     */
    public Vector getDisplayModelOffset() {
        return new Vector(.5, .5, .5);
    }

    public abstract Directionality getDirectionality();

    public abstract Material getBreakParticleMaterial();

    public abstract Sound getBreakSound();


    public UUID createDisplay(Location spot, BlockFace against)
    {
        return createDisplay(new PlacementData(spot, new Quaternionf(), against));
    }

    public UUID createDisplay(Location spot, BlockPlaceEvent e)
    {
        BlockFace against = e.getBlockPlaced().getFace(e.getBlockAgainst());
        var displaySpot = getDisplaySpot(spot, e.getPlayer(), against);

        if(displaySpot == null)
            return null;

        return createDisplay(displaySpot);
    }

    public UUID createDisplay(Location spot, PlayerInteractEvent e)
    {
        var displaySpot = getDisplaySpot(spot, e.getPlayer(), e.getBlockFace());

        if(displaySpot == null)
            return null;

        return createDisplay(displaySpot);
    }

    public UUID createDisplay(PlacementData placementData)
    {
        //create the display entity at the offset
        ItemDisplay display = placementData.location().getWorld().spawn(
                placementData.location().clone().add(getDisplayModelOffset()), ItemDisplay.class, (e) -> {
                        //set an item to display
                        ItemStack toDisplay = new ItemStack(Material.STICK);

                        ItemMeta meta = toDisplay.getItemMeta();

                        meta.displayName(Component.text(getName(), NamedTextColor.GOLD));

                        //change it to the display model
                        NamespacedKey displayModelKey = switch (placementData.faceOn())
                        {
                            case DOWN -> getRoofDisplayModelKey();
                            case NORTH, EAST, SOUTH, WEST -> getWallDisplayModelKey();
                            default -> getDisplayModelKey();
                        };

                        meta.setItemModel(displayModelKey);

                        toDisplay.setItemMeta(meta);

                        //set the display
                        e.setItemStack(toDisplay);

                        //only do rotation logic if we are preforming a rotation
                        //apply the rotation
                        e.setTransformation(
                                new Transformation(
                                        new Vector3f(0, 0, 0),
                                        new Quaternionf(),
                                        new Vector3f(1, 1, 1),
                                        placementData.rotation()
                                )
                        );
                }
        );


        return display.getUniqueId();
    }

    protected boolean isWallFace(BlockFace face)
    {
        return !(face == BlockFace.DOWN || face == BlockFace.UP || face == BlockFace.SELF);
    }

    public PlacementData getDisplaySpot(Location spot, Player player, BlockFace against)
    {
        //set the rotation according to player direction
        Location placedSpot = spot.clone();

        //used to track the item display's roll (used like if it needs to be flipped)
        Quaternionf rotation = null;

        switch (getDirectionality()){

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

    public PlacementData getDisplaySpot(Location spot, PlayerInteractEvent e)
    {
        return getDisplaySpot(spot, e.getPlayer(), e.getBlockFace());
    }

    public PlacementData getDisplaySpot(Location spot, BlockPlaceEvent e)
    {
        return getDisplaySpot(spot, e.getPlayer(), e.getBlockPlaced().getFace(e.getBlockAgainst()));
    }

    /**
     *
     * @param spot Where to place the block
     * @param template Itemstack required to create place event and used in overriding for blocks that need more data from the place item
     * @param placer who to get credit for placing the block, required to fire BlockPlaceEvent
     *               Places the block against the block down from it
     */
    public void placeBlock(Location spot, ItemStack template, Player placer)
    {
        //this is used to place a block somewhere in the world through code
        //the template is only there for usage with blocks that need custom data from an itemstack
        //but for default this just places the block

        Block placeAtBlock = spot.getBlock();
        BlockState previousState = placeAtBlock.getState();
        Block placedAgainst = placeAtBlock.getRelative(BlockFace.DOWN);

        BlockPlaceEvent placeEvent = new BlockPlaceEvent(
                placeAtBlock,
                previousState,
                placedAgainst,
                template,
                placer,
                true,
                EquipmentSlot.HAND
        );

        //call the place event
        //this does the plugin code stuff
        Bukkit.getPluginManager().callEvent(placeEvent);

        //if the event was NOT canceled
        if(!placeEvent.isCancelled())
        {
            //set the block type
            placeAtBlock.setType(getBaseMaterial());
        }
    }

    @Override
    public String toString()
    {
        return getName();
    }

    public void onCopy(PlayerPickItemEvent e, Entity targetEntity, Block targetBlock)
    {
        if(e.isCancelled())
            return;

        //cancel the event since we have to override it ourselves
        e.setCancelled(true);

        //if this item is already in the inventory then swap to it
        PlayerInventory playerInventory = e.getPlayer().getInventory();
        int containingSlot = findInInventory(playerInventory);

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

        e.getPlayer().getInventory().setItemInMainHand(getItem());
    }

    private void dropPlaceableItem(Location breakSpot)
    {
        //drop the item of the base material and call on BlockDrop item for consistency
        //because this is what it would normally look like if the player broke a block
        breakSpot.getWorld().dropItemNaturally(
                breakSpot,
                new ItemStack(getBaseMaterial()),
                this::onPlaceableDropItem
        );
    }

    public void onPlaceableDropItem(Item baseMaterialItem)
    {
        baseMaterialItem.setItemStack(getItem());
    }

    public void onBlockPhysicsUpdate(BlockPhysicsEvent e)
    {
        return;
    }

    public abstract void naturalBlockBreak(Block block, boolean dropItem);

    public BlockFace getPlacementDirection(Block block)
    {
        Integer face = new CustomBlockData(block, CustomItemsAPI.singleton)
                .get(placementDirectionNamespace, PersistentDataType.INTEGER);

        if(face == null)
            return BlockFace.DOWN;

        return BlockFace.values()[face];
    }
}
