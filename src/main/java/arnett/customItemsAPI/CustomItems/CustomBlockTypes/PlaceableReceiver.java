package arnett.customItemsAPI.CustomItems.CustomBlockTypes;

import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public abstract class PlaceableReceiver {
    public abstract void onItemBlockPlaced(BlockPlaceEvent e);

    public abstract void onItemBlockBroken(CustomBlockDataRemoveEvent e);
}
