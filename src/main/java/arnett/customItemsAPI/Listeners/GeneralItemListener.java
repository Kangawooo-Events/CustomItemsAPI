package arnett.customItemsAPI.Listeners;

import arnett.customItemsAPI.CustomItemManager;
import arnett.customItemsAPI.CustomItems.CustomBlockTypes.CustomPlaceableLibrary;
import arnett.customItemsAPI.CustomItems.CustomItemLibrary;
import arnett.customItemsAPI.CustomItems.Useable.CustomUsableLibrary;
import arnett.customItemsAPI.CustomItemsAPI;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class GeneralItemListener implements Listener {

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e)
    {
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
        //item used
        itemInteractCheck(e);

        //block clicked
        blockInteractCheck(e);
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

        //is this a usable item
        if(!(data instanceof CustomUsableLibrary usableData))
            return;

        //call its use function
        usableData.onItemUsed(e);
    }

    @EventHandler
    public void onBlockWithDataRemoved(CustomBlockDataRemoveEvent e)
    {
        CustomItemLibrary data = CustomItemManager.getLibrary(e.getCustomBlockData());

        //is this a custom item
        if(data == null)
            return;

        if(!(data instanceof CustomPlaceableLibrary placeableData))
            return;

        //call item's break function
        placeableData.onItemBlockBroken(e);

        //remove the display
        CustomPlaceableLibrary.removeLink(e.getCustomBlockData());
    }

    @EventHandler
    public void onCopy(PlayerPickItemEvent e)
    {
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
