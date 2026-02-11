package arnett.customItemsAPI.CustomItems.CustomBlockTypes.BlockState;

import arnett.customItemsAPI.CustomItemManager;
import arnett.customItemsAPI.CustomItems.CustomBlockType;
import arnett.customItemsAPI.CustomItems.CustomBlockTypes.CustomPlaceableData;
import arnett.customItemsAPI.CustomItems.CustomItemData;
import arnett.customItemsAPI.CustomItems.Directionality;
import arnett.customItemsAPI.CustomItemsAPI;
import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public abstract class CustomBlockStatePlaceableData extends CustomPlaceableData {

    protected abstract BlockData getOverrideBlockData();

    private UUID displayEntityID;

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

    public UUID createDisplay(Location spot, double rollRot)
    {
        //create the display entity at the offset
        ItemDisplay display = spot.getWorld().spawn(spot.clone().add(getDisplayModelOffset()), ItemDisplay.class, (e) -> {
            //set an item to display
            ItemStack toDisplay = new ItemStack(Material.STICK);

            ItemMeta meta = toDisplay.getItemMeta();

            //change it to the safe
            meta.setItemModel(getDisplayModelKey());

            toDisplay.setItemMeta(meta);

            //set the display
            e.setItemStack(toDisplay);

        });

        displayEntityID = display.getUniqueId();

        return  displayEntityID;
    }

    public UUID createDisplay(Location spot, BlockPlaceEvent e, Directionality directionality)
    {
        //set the rotation according to player direction
        Location rotatedSpot = spot.clone();

        BlockFace face = e.getBlockPlaced().getFace(e.getBlockAgainst());
        boolean againstWall = !(face == BlockFace.DOWN || face == BlockFace.UP || face == BlockFace.SELF);

        //used to track the item display's roll (used like if it needs to be flipped)
        double zRot = 0f;

        switch (directionality){
            case Directionality.NESW ->
            {
                switch (face)
                {
                    //base it off wall placement
                    case NORTH -> rotatedSpot.setYaw(0f);
                    case EAST -> rotatedSpot.setYaw(90f);
                    case SOUTH -> rotatedSpot.setYaw(180f);
                    case WEST -> rotatedSpot.setYaw(270f);

                    //placed on floor or roof
                    //base it off player direction
                    default -> {
                        switch (e.getPlayer().getFacing())
                        {
                            case NORTH -> rotatedSpot.setYaw(0f);
                            case EAST -> rotatedSpot.setYaw(90f);
                            case SOUTH -> rotatedSpot.setYaw(180f);
                            case WEST -> rotatedSpot.setYaw(270f);
                        }
                    }
                }
            }
            case Directionality.UD ->
            {
                switch (face)
                {
                    case UP -> zRot = 180f;
                    case DOWN -> zRot = 0f;

                    //placed on a wall so base of placed direction
                    default -> {
                        //if the player is looking up flip it
                        if(e.getPlayer().getFacing() == BlockFace.UP)
                        {
                            zRot = 180;
                        }
                    }
                }
            }
            case Directionality.NESWUD ->
            {
                //if the player is looking up flip it
                if(e.getPlayer().getFacing() == BlockFace.UP)
                {
                    zRot = 180;
                }

                switch (face)
                {
                    //base it off wall placement
                    case NORTH -> rotatedSpot.setYaw(0f);
                    case EAST -> rotatedSpot.setYaw(90f);
                    case SOUTH -> rotatedSpot.setYaw(180f);
                    case WEST -> rotatedSpot.setYaw(270f);

                    //placed on floor or roof
                    //base it off player direction
                    case DOWN, UP -> {
                        switch (e.getPlayer().getFacing())
                        {
                            case NORTH -> rotatedSpot.setYaw(0f);
                            case EAST -> rotatedSpot.setYaw(90f);
                            case SOUTH -> rotatedSpot.setYaw(180f);
                            case WEST -> rotatedSpot.setYaw(270f);
                        }

                        if(face == BlockFace.UP)
                            zRot = 180f;
                    }
                }
            }
        }

        return createDisplay(rotatedSpot, zRot);
    }

    public Entity getDisplayEntity()
    {
        return Bukkit.getEntity(displayEntityID);
    }

    public UUID getDisplayEntityID()
    {
        return displayEntityID;
    }


    @Override
    public void onItemBlockPlaced(BlockPlaceEvent e)
    {

        //the item was placed
        //replace the actual block with the kind we want
        e.getBlock().setBlockData(getOverrideBlockData());

        //spawn the item's Display
        Location placeSpot = e.getBlock().getLocation();

        //create the block's pdc
        PersistentDataContainer customBlockData = new CustomBlockData(e.getBlock(), CustomItemsAPI.singleton);

        //set the generic tag to tell it is a custom block
        customBlockData.set(CustomItemData.customItemTag, PersistentDataType.STRING, getIdentifier().toString());

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
    public void onItemBlockBroken(CustomBlockDataRemoveEvent e)
    {
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

    }
}
