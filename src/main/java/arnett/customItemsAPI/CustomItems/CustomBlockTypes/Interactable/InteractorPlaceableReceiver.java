package arnett.customItemsAPI.CustomItems.CustomBlockTypes.Interactable;

import arnett.customItemsAPI.CustomItems.CustomBlockTypes.BlockState.CustomBlockStatePlaceableData;
import arnett.customItemsAPI.CustomItems.CustomBlockTypes.PlaceableReceiver;
import arnett.customItemsAPI.CustomItems.CustomItemManager;
import arnett.customItemsAPI.CustomItemsAPI;
import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class InteractorPlaceableReceiver extends PlaceableReceiver {

    protected CustomBlockStatePlaceableData itemData;

    public InteractorPlaceableReceiver(CustomBlockStatePlaceableData data) {
        itemData = data;
    }

    @Override
    public void onItemBlockPlaced(BlockPlaceEvent e) {

    }

    @Override
    public void onItemBlockBroken(CustomBlockDataRemoveEvent e) {

    }
}
