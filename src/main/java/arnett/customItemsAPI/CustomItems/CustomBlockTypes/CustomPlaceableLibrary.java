package arnett.customItemsAPI.CustomItems.CustomBlockTypes;

import arnett.customItemsAPI.CustomItemManager;
import arnett.customItemsAPI.CustomItems.CustomItemLibrary;
import arnett.customItemsAPI.CustomItems.Directionality;
import arnett.customItemsAPI.CustomItemsAPI;
import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

public abstract class CustomPlaceableLibrary extends CustomItemLibrary {

    public abstract NamespacedKey getDisplayModelKey();

    public List<NamespacedKey> getAttachmentModelKeys()
    {
        return List.of();
    }

    public Vector getDisplayModelOffset() {
        return new Vector(.5, .5, .5);
    }

    public abstract Directionality getDirectionality();

    public abstract Material getBreakParticleMaterial();

    public abstract Sound getBreakSound();

    public UUID createDisplay(Location spot, double rollRot)
    {
        //create the display entity at the offset
        ItemDisplay display = spot.getWorld().spawn(spot.clone().add(getDisplayModelOffset()), ItemDisplay.class, (e) -> {
            //set an item to display
            ItemStack toDisplay = new ItemStack(Material.STICK);

            ItemMeta meta = toDisplay.getItemMeta();

            meta.displayName(Component.text(getName(), NamedTextColor.GOLD));

            //change it to the safe
            meta.setItemModel(getDisplayModelKey());

            toDisplay.setItemMeta(meta);

            //set the display
            e.setItemStack(toDisplay);

        });

        System.out.println(display == null);
        System.out.println(display.getUniqueId());

        displayEntityID = display.getUniqueId();

        return displayEntityID;
    }

    public UUID createDisplay(Location spot, BlockPlaceEvent e, Directionality directionality)
    {
        //set the rotation according to player direction
        Location rotatedSpot = spot.clone();

        BlockFace face = e.getBlockPlaced().getFace(e.getBlockAgainst());
        boolean againstWall = !(face == BlockFace.DOWN || face == BlockFace.UP || face == BlockFace.SELF);

        //used to track the item display's roll (used like if it needs to be flipped)
        double zRot = 0f;

        switch (directionality){
            case Directionality.NESW ->
            {
                switch (face)
                {
                    //base it off wall placement
                    case NORTH -> rotatedSpot.setYaw(0f);
                    case EAST -> rotatedSpot.setYaw(90f);
                    case SOUTH -> rotatedSpot.setYaw(180f);
                    case WEST -> rotatedSpot.setYaw(270f);

                    //placed on floor or roof
                    //base it off player direction
                    default -> {
                        switch (e.getPlayer().getFacing())
                        {
                            case NORTH -> rotatedSpot.setYaw(0f);
                            case EAST -> rotatedSpot.setYaw(90f);
                            case SOUTH -> rotatedSpot.setYaw(180f);
                            case WEST -> rotatedSpot.setYaw(270f);
                        }
                    }
                }
            }
            case Directionality.UD ->
            {
                switch (face)
                {
                    case UP -> zRot = 180f;
                    case DOWN -> zRot = 0f;

                    //placed on a wall so base of placed direction
                    default -> {
                        //if the player is looking up flip it
                        if(e.getPlayer().getFacing() == BlockFace.UP)
                        {
                            zRot = 180;
                        }
                    }
                }
            }
            case Directionality.NESWUD ->
            {
                //if the player is looking up flip it
                if(e.getPlayer().getFacing() == BlockFace.UP)
                {
                    zRot = 180;
                }

                switch (face)
                {
                    //base it off wall placement
                    case NORTH -> rotatedSpot.setYaw(0f);
                    case EAST -> rotatedSpot.setYaw(90f);
                    case SOUTH -> rotatedSpot.setYaw(180f);
                    case WEST -> rotatedSpot.setYaw(270f);

                    //placed on floor or roof
                    //base it off player direction
                    case DOWN, UP -> {
                        switch (e.getPlayer().getFacing())
                        {
                            case NORTH -> rotatedSpot.setYaw(0f);
                            case EAST -> rotatedSpot.setYaw(90f);
                            case SOUTH -> rotatedSpot.setYaw(180f);
                            case WEST -> rotatedSpot.setYaw(270f);
                        }

                        if(face == BlockFace.UP)
                            zRot = 180f;
                    }
                }
            }
        }

        return createDisplay(rotatedSpot, zRot);
    }

    protected UUID displayEntityID;

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

    public void deleteBlock(Block block)
    {
        CustomBlockData cbd = new CustomBlockData(block, CustomItemsAPI.singleton);
        removeLink(cbd);
        cbd.clear();
        block.setType(Material.AIR);
    }

    public static void removeLink(CustomBlockData blockData)
    {
        //get the attached entity
        String id = blockData.get(CustomItemManager.DisplayLinkNamespace, PersistentDataType.STRING);

        //get the id
        UUID entityId = UUID.fromString(id);

        //remove the entity
        Bukkit.getEntity(entityId).remove();
    }

    public static void removeLink(Block block)
    {
        if(!CustomBlockData.hasCustomBlockData(block, CustomItemsAPI.singleton))
            return;

        //get the attached entity
        removeLink(new CustomBlockData(block, CustomItemsAPI.singleton));
    }

    @Override
    public String toString()
    {
        return getName();
    }

    public void onItemBlockPlaced(BlockPlaceEvent e)
    {
        return;
    }

    public void onBlockInteraction(PlayerInteractEvent e)
    {
        return;
    }

    public void onItemBlockBroken(BlockBreakEvent e)
    {
        if(e.isCancelled())
            return;

        //remove the display
        CustomPlaceableLibrary.removeLink(e.getBlock());
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

    public ItemDisplay getDisplayEntity()
    {
        return (ItemDisplay) Bukkit.getEntity(displayEntityID);
    }

    public UUID getDisplayEntityID()
    {
        return displayEntityID;
    }
}
