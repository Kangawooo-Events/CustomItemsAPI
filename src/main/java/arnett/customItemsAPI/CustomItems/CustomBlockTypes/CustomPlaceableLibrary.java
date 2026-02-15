package arnett.customItemsAPI.CustomItems.CustomBlockTypes;

import arnett.customItemsAPI.CustomItemManager;
import arnett.customItemsAPI.CustomItems.CustomItemLibrary;
import arnett.customItemsAPI.CustomItems.Directionality;
import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

public abstract class CustomPlaceableLibrary extends CustomItemLibrary {

    public static void removeLink(CustomBlockData blockData)
    {
        //get the attached entity
        String id = blockData.get(CustomItemManager.DisplayLinkNamespace, PersistentDataType.STRING);

        //get the id
        UUID entityId = UUID.fromString(id);

        //remove the entity
        Bukkit.getEntity(entityId).remove();
    }

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

    public abstract UUID createDisplay(Location spot, double rollRot);

    public abstract UUID createDisplay(Location spot, BlockPlaceEvent e, Directionality directionality);

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

    public void onItemBlockBroken(CustomBlockDataRemoveEvent e)
    {
        return;
    }

    public void onCopy(PlayerPickItemEvent e, Entity targetEntity, Block targetBlock)
    {
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
}
