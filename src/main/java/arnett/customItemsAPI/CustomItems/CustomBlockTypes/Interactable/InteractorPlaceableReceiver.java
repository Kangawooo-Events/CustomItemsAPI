package arnett.customItemsAPI.CustomItems.CustomBlockTypes.Interactable;

import arnett.customItemsAPI.CustomItems.CustomBlockTypes.BlockState.CustomBlockStatePlaceableData;
import arnett.customItemsAPI.CustomItems.CustomBlockTypes.PlaceableReceiver;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import org.bukkit.event.block.BlockPlaceEvent;

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
