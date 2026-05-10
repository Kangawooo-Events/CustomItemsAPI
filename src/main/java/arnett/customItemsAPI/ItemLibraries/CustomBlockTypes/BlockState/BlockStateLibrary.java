package arnett.customItemsAPI.ItemLibraries.CustomBlockTypes.BlockState;

import arnett.customItemsAPI.ItemLibraries.CustomBlockTypes.PlaceableLibrary;
import arnett.customItemsAPI.ItemLibraries.ItemLibrary;
import arnett.customItemsAPI.CustomItemsAPI;
import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public abstract class BlockStateLibrary extends PlaceableLibrary {

    protected abstract BlockData getOverrideBlockData();

    public abstract boolean getExplosionResistant();

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

    public void onBlockPlaced(BlockPlaceEvent e)
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
        customBlockData.set(ItemLibrary.customItemTag, PersistentDataType.STRING, getIdentifier().toString());

        //set this as the item (boolean and true don't really matter here we just check that it has this later)
        customBlockData.set(getIdentifier(), PersistentDataType.BOOLEAN, true);

        var placementInfo = getDisplaySpot(placeSpot, e);

        //set the placement direction
        customBlockData.set(placementDirectionNamespace, PersistentDataType.INTEGER, placementInfo.faceOn().ordinal());

        UUID id = createDisplay(placementInfo);

        if(id == null)
        {
            e.setCancelled(true);
            return;
        }

        //Link this block to the display
        new CustomBlockData(e.getBlock(), CustomItemsAPI.singleton).set(
                PlaceableLibrary.DisplayLinkNamespace,
                PersistentDataType.STRING,
                id.toString()
        );
    }

    public void onBlockBroken(BlockBreakEvent e)
    {
        if(e.isCancelled())
            return;

        //stop the item drop and handle it ourselves
        e.setDropItems(false);
        naturalBlockBreak(e.getBlock(), e.getPlayer().getGameMode() != GameMode.CREATIVE);

        //we handle the breaking ourselves so cancel the event from this point
        e.setCancelled(true);
    }

    public void onBlockInteracted(PlayerInteractEvent e, Event.Result canUseBlock)
    {
        return;
    }

    public ItemDisplay getDisplayEntity(Block block)
    {
        return (ItemDisplay) Bukkit.getEntity(getDisplayEntityID(block));
    }

    public UUID getDisplayEntityID(Block block)
    {
        if(!CustomBlockData.hasCustomBlockData(block, CustomItemsAPI.singleton))
        {
            throw new IllegalArgumentException("No Display Entity Attached to Block");
        }

        return UUID.fromString(
                new CustomBlockData(block, CustomItemsAPI.singleton)
                        .get(PlaceableLibrary.DisplayLinkNamespace, PersistentDataType.STRING)
        );
    }

    /**
     * Called once an entity explodes with this in the radius
     * @param e the Explosion Bukkit event
     * @param customBlock The custom block which is set to be exploded
     * @return whether to remove it from the explosion
     * list, true = resist, false = explode
     */
    public boolean onEntityExplode(EntityExplodeEvent e, Block customBlock)
    {
        onExplode(customBlock);
        return true;
    }

    /**
     * Called once a block explodes with this in the radius, by default just calls onExplode
     * @param e the Explosion Bukkit event
     * @param customBlock The custom block which is set to be exploded
     * @return whether to remove it from the explosion
     * list, true = resist, false = explode.
     * Always returns true by default because if it does get exploded the block breakage is handled
     * in the onExplode function to drop the custom item.
     */
    public boolean onBlockExplode(BlockExplodeEvent e, Block customBlock)
    {
        onExplode(customBlock);
        return true;
    }

    /**
     * Called by default when an explosion reaches this block.
     * Calls the naturalBreakBlock function if we are not explosion resistant
     */
    public void onExplode(Block explodedBlock)
    {
        if(!getExplosionResistant())
        {
            //break it ourselves
            naturalBlockBreak(explodedBlock, true);
        }
    }

    /**
     * Naturally removes the custom block and drops it's items
     * @param brokenBlock the custom block being broken
     */
    @Override
    public void naturalBlockBreak(Block brokenBlock, boolean dropItem)
    {
        if(isItem(brokenBlock))
            //first delete the block
            deleteBlock(brokenBlock);

        Location offsetLocation = brokenBlock.getLocation().clone().add(getDisplayModelOffset());

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

        //remove the display
        removeLink(brokenBlock);

        //then drop the block item if set to
        if(dropItem)
            dropPlaceableItem(brokenBlock);
    }

    private void dropPlaceableItem(Block breakBlock) {
        //drop the item of the base material and call on BlockDrop item for consistency
        //because this is what it would normally look like if the player broke a block
        breakBlock.getWorld().dropItemNaturally(
                breakBlock.getLocation().add(.5f, .5f, .5f),
                new ItemStack(getBaseMaterial()),
                item -> onPlaceableDropItem(breakBlock, item)
        );
    }

    public void onPlaceableDropItem(Block brokenBlock, Item baseMaterialItem)
    {
        //call the base function
        onPlaceableDropItem(baseMaterialItem);
    }

    public void deleteBlock(Block block)
    {
        CustomBlockData cbd = new CustomBlockData(block, CustomItemsAPI.singleton);
        removeLink(cbd);
        cbd.clear();
        block.setType(Material.AIR);
    }

    public static void removeLink(CustomBlockData blockData)
    {
        //get the attached entity
        String id = blockData.get(PlaceableLibrary.DisplayLinkNamespace, PersistentDataType.STRING);

        //get the id
        UUID entityId = UUID.fromString(id);

        //remove the entity
        Entity entity = Bukkit.getEntity(entityId);

        if(entity == null)
            return;

        entity.remove();
    }

    public static void removeLink(Block block)
    {
        if(!CustomBlockData.hasCustomBlockData(block, CustomItemsAPI.singleton))
            return;

        //get the attached entity
        removeLink(new CustomBlockData(block, CustomItemsAPI.singleton));
    }

    @Override
    public void onBlockPhysicsUpdate(BlockPhysicsEvent e) {

        switch (getDirectionality())
        {
            case WallBlock, WallUDBlock, WallDBlock: break;
            default : return;
        }

        if(e.getBlock().getType() != Material.AIR)
            naturalBlockBreak(e.getBlock(), true);
        if(getPlacementDirection(e.getBlock()).getOppositeFace() == e.getBlock().getFace(e.getSourceBlock()))
        {
            //attached block broken
            if(!e.getSourceBlock().isBuildable())
            {
                //block is no longer solid so break this
                naturalBlockBreak(e.getBlock(), true);
            }
        }
    }
}
