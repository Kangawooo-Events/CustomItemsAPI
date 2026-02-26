package arnett.customItemsAPI.Listeners;

import arnett.customItemsAPI.CustomItemManager;
import arnett.customItemsAPI.CustomItems.CustomBlockTypes.CustomPlaceableLibrary;
import arnett.customItemsAPI.CustomItems.CustomItemLibrary;
import arnett.customItemsAPI.CustomItemsAPI;
import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class GeneralItemListener implements Listener {

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e)
    {
        if(e.isCancelled())
            return;

        CustomItemLibrary data = CustomItemManager.getLibrary(e.getItemInHand());

        //is this a custom item
        if(data == null)
            return;

        if(!(data instanceof CustomPlaceableLibrary placeableData))
            return;

        placeableData.onItemBlockPlaced(e);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e)
    {
        if(e.useItemInHand() != Event.Result.DENY)
            //item used
            itemInteractCheck(e);

        if(e.useInteractedBlock() != Event.Result.DENY)
            //block clicked
            blockInteractCheck(e);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e)
    {
        //get item used
        ItemStack used = e.getPlayer().getInventory().getItem(e.getHand());

        CustomItemLibrary data = CustomItemManager.getLibrary(used);

        //is this a custom item
        if(data == null)
            return;

        //call its use function
        data.onItemUsedOnEntity(e);
    }

    void blockInteractCheck(PlayerInteractEvent e)
    {
        CustomItemLibrary data = CustomItemManager.getLibrary(e.getClickedBlock());

        //is this a custom item
        if(data == null)
            return;

        //get placeable data
        if(!(data instanceof CustomPlaceableLibrary placeableData))
            return;

        //call item's break function
        placeableData.onBlockInteraction(e);

    }

    void itemInteractCheck(PlayerInteractEvent e)
    {
        //check the item used
        CustomItemLibrary data = CustomItemManager.getLibrary(e.getItem());

        //is this a custom item
        if(data == null)
            return;

        //call its use function
        data.onItemUsed(e);
    }

    @EventHandler
    public void onBlockRemovedByPlayer(BlockBreakEvent e)
    {
        if(e.isCancelled())
            return;

        //get custom data (getLibrary has a quick exit)
        CustomItemLibrary data = CustomItemManager.getLibrary(e.getBlock());

        //is this a custom item
        if(data == null)
            return;

        if(!(data instanceof CustomPlaceableLibrary placeableData))
            return;

        //call item's break function
        placeableData.onItemBlockBroken(e);
    }

    @EventHandler
    public void onCopy(PlayerPickItemEvent e)
    {
        if(e.isCancelled())
            return;

        Player player = e.getPlayer();

        //get what was selected
        var results = e.getPlayer().getWorld().rayTrace(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                (int)player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE).getValue(),
                FluidCollisionMode.NEVER,
                false,
                .1,
                entity -> entity != player
        );

        if(results.getHitBlock() != null)
        {
            CustomItemLibrary data = CustomItemManager.getLibrary(results.getHitBlock());

            //is this a custom item
            if(data == null)
                return;

            if(!(data instanceof CustomPlaceableLibrary placeableData))
                return;

            //call item's copy function
            placeableData.onCopy(e, results.getHitEntity(), results.getHitBlock());
        }
    }
}
