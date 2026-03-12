package arnett.customItemsAPI.CustomItems.CustomBlockTypes.BlockState;

import arnett.customItemsAPI.CustomItemManager;
import arnett.customItemsAPI.CustomItems.CustomBlockTypes.CustomPlaceableLibrary;
import arnett.customItemsAPI.CustomItems.CustomItemLibrary;
import arnett.customItemsAPI.CustomItems.Directionality;
import arnett.customItemsAPI.CustomItemsAPI;
import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public abstract class CustomBlockStatePlaceableLibrary extends CustomPlaceableLibrary {

    protected abstract BlockData getOverrideBlockData();

    public boolean isItem(Block block)
    {
        //start with general material check
        if(!block.getType().equals(getOverrideBlockData().getMaterial()))
            return false;

        //then check if the block has a pdc
        if(!CustomBlockData.hasCustomBlockData(block, CustomItemsAPI.singleton))
        {
            return false;
        }

        //only then check if it has the specific namespace
        return new CustomBlockData(block, CustomItemsAPI.singleton).has(getIdentifier());
    }

    @Override
    public void onItemBlockPlaced(BlockPlaceEvent e)
    {
        if(e.isCancelled())
            return;

        //the item was placed
        //replace the actual block with the kind we want
        e.getBlock().setBlockData(getOverrideBlockData());

        //spawn the item's Display
        Location placeSpot = e.getBlock().getLocation();

        //create the block's pdc
        PersistentDataContainer customBlockData = new CustomBlockData(e.getBlock(), CustomItemsAPI.singleton);

        //set the generic tag to tell it is a custom block
        customBlockData.set(CustomItemLibrary.customItemTag, PersistentDataType.STRING, getIdentifier().toString());

        //set this as the item (boolean and true don't really matter here we just check that it has this later)
        customBlockData.set(getIdentifier(), PersistentDataType.BOOLEAN, true);

        //Link this block to the display
        customBlockData.set(
                CustomItemManager.DisplayLinkNamespace,
                PersistentDataType.STRING,
                createDisplay(placeSpot, e, getDirectionality()).toString()
        );
    }

    @Override
    public void onItemBlockBroken(BlockBreakEvent e)
    {
        if(e.isCancelled())
            return;

        //the item was Broken
        Location offsetLocation = e.getBlock().getLocation().clone().add(getDisplayModelOffset());

        //show particles
        offsetLocation.getWorld().spawnParticle(
                Particle.BLOCK,
                offsetLocation,
                30,
                0.3,
                0.3,
                0.3,
                0.1f,
                getBreakParticleMaterial().createBlockData()
        );

        //play the break sound
        offsetLocation.getWorld().playSound(
                offsetLocation,
                getBreakSound(),
                1f,
                1f
        );

        super.onItemBlockBroken(e);
    }

}
