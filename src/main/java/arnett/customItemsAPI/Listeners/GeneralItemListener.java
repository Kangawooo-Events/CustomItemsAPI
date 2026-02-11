package arnett.customItemsAPI.Listeners;

import arnett.customItemsAPI.CustomItemManager;
import arnett.customItemsAPI.CustomItems.CustomBlockTypes.CustomPlaceableData;
import arnett.customItemsAPI.CustomItems.CustomItemData;
import arnett.customItemsAPI.CustomItems.Useable.CustomUsableData;
import arnett.customItemsAPI.CustomItemsAPI;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class GeneralItemListener implements Listener {

    CustomItemManager manager;

    public GeneralItemListener(CustomItemManager manager)
    {
        this.manager = manager;
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e)
    {
        CustomItemData data = manager.getData(e.getItemInHand());

        //is this a custom item
        if(data == null)
            return;

        if(!(data instanceof CustomPlaceableData placeableData))
            return;

        placeableData.onItemBlockPlaced(e);
    }

    @EventHandler
    public void onItemInteract(PlayerInteractEvent e)
    {
        CustomItemData data = manager.getData(e.getItem());

        //is this a custom item
        if(data == null)
            return;

        //is this a usable item
        if(!(data instanceof CustomUsableData usableData))
            return;

        //call its use function
        usableData.onItemUsed(e);
    }

    @EventHandler
    public void onBlockWithDataRemoved(CustomBlockDataRemoveEvent e)
    {
        CustomItemData data = manager.getData(e.getCustomBlockData());

        //is this a custom item
        if(data == null)
            return;

        if(!(data instanceof CustomPlaceableData placeableData))
            return;

        //call item's break function
        placeableData.onItemBlockBroken(e);

        //remove the display
        CustomPlaceableData.removeLink(e.getCustomBlockData());
    }
}
