package arnett.customItemsAPI.CustomItems.Useable;

import arnett.customItemsAPI.CustomItems.CustomBlockTypes.BlockState.CustomBlockStatePlaceableData;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public abstract class UsableReceiver {
    public abstract void onItemUsed(PlayerInteractEvent e);
}
