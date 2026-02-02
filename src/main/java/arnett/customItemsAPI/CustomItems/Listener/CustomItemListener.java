package arnett.customItemsAPI.CustomItems.Listener;

import arnett.customItemsAPI.CustomItems.CustomItemManager;
import arnett.customItemsAPI.CustomItems.CustomBlockTypes.CustomPlaceableData;
import arnett.customItemsAPI.CustomItemsAPI;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class CustomItemListener implements Listener {

    CustomItemManager manager;

    public CustomItemListener(CustomItemManager manager)
    {
        this.manager = manager;
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e)
    {
        PersistentDataContainerView pdc = e.getItemInHand().getPersistentDataContainer();

        manager.getItems().forEach(item -> {
            if(item instanceof CustomPlaceableData placeable)
            {
                if(pdc.has(item.getItemIdentifierKey()))
                    placeable.getPlaceableReceiver().onItemBlockPlaced(e);
            }
        });
    }


    @EventHandler
    public void onBlockWithDataRemoved(CustomBlockDataRemoveEvent e)
    {
        PersistentDataContainer pdc = e.getCustomBlockData();

        if(!pdc.has(CustomItemManager.DisplayLinkNamespace))
            return;

        manager.getItems().forEach(item -> {
            if(item instanceof CustomPlaceableData placeable)
            {
                if(pdc.has(item.getItemIdentifierKey()))
                {
                    placeable.getPlaceableReceiver().onItemBlockBroken(e);
                }
            }
        });

        //get the attached entity
        String id = e.getCustomBlockData().get(CustomItemManager.DisplayLinkNamespace, PersistentDataType.STRING);

        //get the id
        UUID entityId = UUID.fromString(id);

        //remove the entity
        Bukkit.getEntity(entityId).remove();
    }
}
