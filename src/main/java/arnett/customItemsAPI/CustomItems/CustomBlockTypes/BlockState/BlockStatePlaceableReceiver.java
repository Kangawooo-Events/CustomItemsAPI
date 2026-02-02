package arnett.customItemsAPI.CustomItems.CustomBlockTypes.BlockState;

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

public abstract class BlockStatePlaceableReceiver extends PlaceableReceiver {

    protected CustomBlockStatePlaceableData itemData;

    public BlockStatePlaceableReceiver(CustomBlockStatePlaceableData data) {
        itemData = data;
    }

    @Override
    public void onItemBlockPlaced(BlockPlaceEvent e)
    {

        //the item was placed
        //replace the actual block with the kind we want
        e.getBlock().setBlockData(itemData.getOverrideBlockData());

        //spawn the item's Display
        Location placeSpot = e.getBlock().getLocation();

        //create the block's pdc
        PersistentDataContainer customBlockData = new CustomBlockData(e.getBlock(), CustomItemsAPI.singleton);

        //set this as the item (boolean and true don't really matter here we just check that it has this later)
        customBlockData.set(itemData.getItemIdentifierKey(), PersistentDataType.BOOLEAN, true);

        //Link this block to the display
        customBlockData.set(
                CustomItemManager.DisplayLinkNamespace,
                PersistentDataType.STRING,
                itemData.createDisplay(placeSpot, e, itemData.getDirectionality()).toString()
        );
    }

    @Override
    public void onItemBlockBroken(CustomBlockDataRemoveEvent e)
    {
        //the item was Broken
        Location offsetLocation = e.getBlock().getLocation().clone().add(itemData.getDisplayModelOffset());

        //show particles
        offsetLocation.getWorld().spawnParticle(
                Particle.BLOCK,
                offsetLocation,
                30,
                0.3,
                0.3,
                0.3,
                0.1f,
                itemData.getBreakParticleMaterial().createBlockData()
        );

        //play the break sound
        offsetLocation.getWorld().playSound(
                offsetLocation,
                itemData.getBreakSound(),
                1f,
                1f
        );

    }
}
