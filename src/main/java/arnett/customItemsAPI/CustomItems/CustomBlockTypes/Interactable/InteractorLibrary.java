package arnett.customItemsAPI.CustomItems.CustomBlockTypes.Interactable;

import arnett.customItemsAPI.CustomItems.CustomBlockTypes.PlaceableLibrary;
import arnett.customItemsAPI.CustomItems.CustomBlockTypes.PlacementData;
import arnett.customItemsAPI.CustomItems.ItemLibrary;
import arnett.customItemsAPI.CustomItemsAPI;
import arnett.customItemsAPI.ItemManager;
import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.UUID;

public abstract class InteractorLibrary extends PlaceableLibrary {

    public static NamespacedKey InteractorLinkNamespace =  new NamespacedKey("customitems", "linkedinteractor");

    public Vector getHitboxOffset()
    {
        return new Vector(.5f, 0, .5f);
    }

    public abstract float getHeight();
    public abstract float getWidth();

    public abstract boolean isExplosionResistant();

    public Vector getHitboxWallOffset()
    {
        return new Vector(0, .5f, .5f);
    }
    public Sound getPlacementSound()
    {
        return null;
    }

    public float getWallHeight()
    {
        return getWidth();
    }
    public float getWallWidth()
    {
        return getHeight();
    }

    public boolean isItem(Entity entity)
    {
        //start with general entity type check
        if(!(entity instanceof Interaction interaction))
            return false;

        //then check if the entity has the pdc tag for this item
        return interaction.getPersistentDataContainer().has(getIdentifier());
    }

    public Interaction onItemPlacementInteraction(PlayerInteractEvent e, Event.Result canUseItem)
    {
        if(canUseItem == Event.Result.DENY)
            return null;

        //the item was placed
        Block placeAtBlock = e.getClickedBlock().isReplaceable() ?
                e.getClickedBlock() :
                e.getClickedBlock().getRelative(e.getBlockFace());

        Location placeSpot = placeAtBlock.getLocation();

        //check the block for nearby entities or blocks
        if((placeAtBlock.getType() != Material.AIR &&
                !placeAtBlock.isReplaceable()) ||
                placeSpot.getWorld().getNearbyEntities(BoundingBox.of(placeAtBlock)).stream().anyMatch(
                        entity -> {
                            return entity instanceof Interaction interaction &&
                                            ItemManager.getLibrary(interaction) != null;
                        }
                )
        )
        {
            e.setCancelled(true);
            return null;
        }

        //get the display spot now since we're going to need it to create the hitbox
        PlacementData displayInfo = getDisplaySpot(placeSpot, e);

        if(displayInfo == null)
        {
            e.setCancelled(true);
            return null;
        }

        //remove the block if it was replaced
        placeAtBlock.breakNaturally();


        boolean isOnWall = switch (getDirectionality())
        {
            case Wall, WallBlock, WallD, WallUD, WallDBlock, WallUDBlock -> isWallFace(displayInfo.faceOn());
            default -> false;
        };

        Location hitboxCenter = displayInfo.location().clone();

        //offset
        Vector offset;
        if(isOnWall)
        {
            //add relative to the forward position of the location
            offset = getHitboxWallOffset();

            offset.add(new Vector(0, -getWallHeight()/2, -getWallWidth()/2));

            //rotate the offset based on the yaw
            switch (displayInfo.faceOn())
            {
                case NORTH -> offset.rotateAroundY(0);
                case EAST -> offset.rotateAroundY(-Math.PI/2);
                case SOUTH -> offset.rotateAroundY(Math.PI);
                case WEST -> offset.rotateAroundY(Math.PI/2);
            }

            offset.rotateAroundY(Math.toRadians(hitboxCenter.getYaw()));

            //then add the regular offset to place it in the center of the block
            offset.add(getHitboxOffset());
        }
        else
        {
            offset = getHitboxOffset();
            //if it is on the roof move the hitbox up
            if(displayInfo.faceOn().equals(BlockFace.DOWN))
            {
                offset.add(new Vector(0, 1 - getHeight(), 0));
            }
        }

        hitboxCenter.add(offset);

        //create the hitbox
        Interaction interaction = e.getClickedBlock().getWorld().spawn(
                hitboxCenter, Interaction.class
        );

        //set size of interaction
        interaction.setInteractionHeight(isOnWall ? getWallHeight() : getHeight());
        interaction.setInteractionWidth(isOnWall ? getWallWidth() : getWidth());

        PersistentDataContainer pdc = interaction.getPersistentDataContainer();

        //set the generic tag to tell it is a custom block
        pdc.set(ItemLibrary.customItemTag, PersistentDataType.STRING, getIdentifier().toString());

        //set this as the item (boolean and true don't really matter here we just check that it has this later)
        pdc.set(getIdentifier(), PersistentDataType.BOOLEAN, true);

        //tag this interactor with its display's UUID for access
        //also creates the item display
        pdc.set(
                ItemManager.DisplayLinkNamespace,
                PersistentDataType.STRING,
                createDisplay(displayInfo).toString()
        );

        //tag the block's pdc as a custom item

        //create the block's pdc
        PersistentDataContainer customBlockData = new CustomBlockData(placeAtBlock, CustomItemsAPI.singleton);

        //set the generic tag to tell it is a custom block
        customBlockData.set(ItemLibrary.customItemTag, PersistentDataType.STRING, getIdentifier().toString());

        //set this as the item (boolean and true don't really matter here we just check that it has this later)
        customBlockData.set(getIdentifier(), PersistentDataType.BOOLEAN, true);

        //tag this block with its interactor's UUID for access
        customBlockData.set(
                InteractorLinkNamespace,
                PersistentDataType.STRING,
                interaction.getUniqueId().toString()
        );

        customBlockData.set(
                placementDirectionNamespace,
                PersistentDataType.INTEGER,
                displayInfo.faceOn().ordinal()
        );

        //don't have the player actually place a block here or interact with it since we are placing a block now
        e.setUseItemInHand(Event.Result.DENY);
        e.setUseInteractedBlock(Event.Result.DENY);

        //remove the item if not in creative
        if(e.getPlayer().getGameMode() != GameMode.CREATIVE)
            e.getItem().setAmount(e.getItem().getAmount()-1);

        //play the swing animation to emulate placing
        e.getPlayer().swingHand(e.getHand());

        Sound placeSound = getPlacementSound();

        if(placeSound != null)
            placeSpot.getWorld().playSound(placeSpot, placeSound, 1, 1);

        return interaction;
    }


    public void onInteractorBroken(EntityDamageByEntityEvent e, Interaction interaction)
    {
        if (e.isCancelled())
            return;

        naturalBlockBreak(interaction,
                !(e.getDamager() instanceof Player player) || player.getGameMode() != GameMode.CREATIVE);
    }

    public void onInteractorInteracted(PlayerInteractEntityEvent e, Interaction interaction)
    {
        return;
    }

    public ItemDisplay getDisplayEntity(Entity Interactor)
    {
        return (ItemDisplay) Bukkit.getEntity(getDisplayEntityID(Interactor));
    }

    public UUID getDisplayEntityID(Entity Interactor)
    {
        if(!Interactor.getPersistentDataContainer().has(ItemManager.DisplayLinkNamespace))
            return null;

        return UUID.fromString(
                Interactor.getPersistentDataContainer()
                        .get(ItemManager.DisplayLinkNamespace, PersistentDataType.STRING)
        );
    }


    /**
     * Called once an entity explodes with this in the radius (power x 2), by default just calls onExplode
     * @param e the Explosion Bukkit event
     * @param customInteraction The custom block which is set to be exploded
     * @return whether to remove it from the explosion
     * list, true = resist, false = explode
     */
    public boolean onEntityExplode(EntityExplodeEvent e, Interaction customInteraction)
    {
        onExplode(customInteraction);
        return true;
    }

    /**
     * Called once a block explodes with this in the radius (power x 2), by default just calls onExplode
     * @param e the Explosion Bukkit event
     * @param customInteraction The custom block which is set to be exploded
     * @return whether to remove it from the explosion
     * list, true = resist, false = explode.
     * Always returns true by default because if it does get exploded the block breakage is handled
     * in the onExplode function to drop the custom item.
     */
    public boolean onBlockExplode(BlockExplodeEvent e, Interaction customInteraction)
    {
        onExplode(customInteraction);
        return true;
    }

    /**
     * Called by default when an explosion reaches this block (power x 2).
     * Calls the naturalBreakBlock function if we are not explosion resistant
     */
    public void onExplode(Interaction explodedInteraction)
    {
        if(!isExplosionResistant())
        {
            //break it ourselves
            naturalBlockBreak(explodedInteraction, true);
        }
    }

    /**
     * Naturally removes the custom block and drops it's items
     * @param blockInteraction the custom block being broken
     */
    public void naturalBlockBreak(Interaction blockInteraction, boolean dropItem)
    {
        if(blockInteraction == null)
            return;

        if(isItem(blockInteraction))
            //first delete the block
            delete(blockInteraction);

        Location offsetLocation = blockInteraction.getLocation().clone().add(0, .5, 0);

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

        //then drop the block item if set to
        if(dropItem)
            dropPlaceableItem(blockInteraction);
    }

    /**
     * Naturally removes the custom block and drops it's items
     * @param interactionBlock the custom block being broken
     */
    @Override
    public void naturalBlockBreak(Block interactionBlock, boolean dropItem)
    {
        naturalBlockBreak(getInteraction(interactionBlock), dropItem);
    }

    private void dropPlaceableItem(Interaction breakInteraction) {
        //drop the item of the base material and call on BlockDrop item for consistency
        //because this is what it would normally look like if the player broke a block
        breakInteraction.getWorld().dropItemNaturally(
                breakInteraction.getLocation(),
                new ItemStack(getBaseMaterial()),
                item -> onPlaceableDropItem(breakInteraction, item)
        );
    }

    public void onPlaceableDropItem(Interaction brokenInteraction, Item baseMaterialItem)
    {
        //call the base function
        onPlaceableDropItem(baseMaterialItem);
    }

    public void delete(Interaction interaction)
    {
        //clear the custom data
        new CustomBlockData(interaction.getLocation().getBlock(), CustomItemsAPI.singleton).clear();
        removeLink(interaction);
        interaction.remove();
    }

    public static void removeLink(Interaction interaction)
    {
        //get the attached entity
        String id = interaction.getPersistentDataContainer().get(ItemManager.DisplayLinkNamespace, PersistentDataType.STRING);

        if(id == null)
            return;

        //get the id
        UUID entityId = UUID.fromString(id);

        //remove the entity
        Entity entity = Bukkit.getEntity(entityId);

        if(entity == null)
            return;

        entity.remove();
    }

    public static Interaction getInteraction(Block block)
    {
        if(block.getType().isSolid())
            return null;

        if(!CustomBlockData.hasCustomBlockData(block, CustomItemsAPI.singleton))
            return null;

        String idString = new CustomBlockData(block, CustomItemsAPI.singleton)
                .get(InteractorLinkNamespace, PersistentDataType.STRING);

        if(idString == null)
            return null;

        UUID id = UUID.fromString(idString);

        if(!(Bukkit.getEntity(id) instanceof Interaction interaction))
            return null;

        return interaction;
    }

    @Override
    public void onBlockPhysicsUpdate(BlockPhysicsEvent e) {
        switch (getDirectionality())
        {
            case WallBlock, WallUDBlock, WallDBlock: break;
            default : return;
        }

        if(getPlacementDirection(e.getBlock()).getOppositeFace() == e.getBlock().getFace(e.getSourceBlock()))
        {
            //attached block broken
            if(!e.getSourceBlock().isBuildable())
            {
                //block is no longer solid so break this
                naturalBlockBreak(getInteraction(e.getBlock()), true);
            }
        }
    }
}
