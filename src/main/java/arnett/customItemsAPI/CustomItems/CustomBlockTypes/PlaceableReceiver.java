package arnett.customItemsAPI.CustomItems.CustomBlockTypes;

import arnett.customItemsAPI.CustomItems.CustomItemManager;
import arnett.customItemsAPI.CustomItemsAPI;
import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public abstract class PlaceableReceiver {
    public abstract void onItemBlockPlaced(BlockPlaceEvent e);

    public abstract void onItemBlockBroken(CustomBlockDataRemoveEvent e);
}
