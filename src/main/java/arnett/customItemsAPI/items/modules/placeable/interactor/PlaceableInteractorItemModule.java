package arnett.customItemsAPI.items.modules.placeable.interactor;

import arnett.customItemsAPI.items.modules.placeable.BasePlaceableItemModule;
import arnett.customItemsAPI.items.modules.placeable.PlacementData;
import arnett.customItemsAPI.items.ItemLibrary;
import arnett.customItemsAPI.CustomItemsAPI;
import arnett.customItemsAPI.ItemManager;
import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.UUID;

public interface PlaceableInteractorItemModule extends BasePlaceableItemModule {

    NamespacedKey InteractorLinkNamespace =  new NamespacedKey("customitems", "linkedinteractor");

    //region Properties

    /*=================================================================================================
                        -  Properties  -
    =================================================================================================*/

    float height();
    float width();

    //endregion


    //region Properites

    /*=================================================================================================
                        -  Properites  -
    =================================================================================================*/

    default Vector hitboxOffset()
    {
        return new Vector(.5f, 0, .5f);
    }

    default Vector hitboxWallOffset()
    {
        return new Vector(0, .5f, .5f);
    }
    default Sound placementSound()
    {
        return null;
    }

    default float wallHeight()
    {
        return width();
    }
    default float wallWidth()
    {
        return height();
    }


    //endregion


    //region Create Display

    /*=================================================================================================
                        -  Create Display  -
    =================================================================================================*/

    /**
     * Creates an itemDisplay with the given placement data for this item
     * @return UUID of created itemDisplay
     */
    default UUID createDisplay(PlacementData placementData)
    {
        //create the display entity at the offset
        ItemDisplay display = placementData.location().getWorld().spawn(
                placementData.location().clone().add(displayModelOffset()), ItemDisplay.class, (e) -> {
                    //set an item to display
                    ItemStack toDisplay = new ItemStack(Material.STICK);

                    ItemMeta meta = toDisplay.getItemMeta();

                    //change it to the display model
                    NamespacedKey displayModelKey = switch (placementData.faceOn())
                    {
                        case DOWN -> roofDisplayModelKey();
                        case NORTH, EAST, SOUTH, WEST -> wallDisplayModelKey();
                        default -> displayModelKey();
                    };

                    meta.setItemModel(displayModelKey);

                    toDisplay.setItemMeta(meta);

                    //set the display
                    e.setItemStack(toDisplay);

                    //only do rotation logic if we are preforming a rotation
                    //apply the rotation
                    e.setTransformation(
                            new Transformation(
                                    new Vector3f(0, 0, 0),
                                    new Quaternionf(),
                                    new Vector3f(1, 1, 1),
                                    placementData.rotation()
                            )
                    );
                }
        );


        return display.getUniqueId();
    }


    //region Overloads

    //----------= Overloads =-----------------------------------------------------------------------------


    /**
     * Creates an itemDisplay with the given placement data for this item
     * @return UUID of created itemDisplay
     */
    default UUID createDisplay(Location spot, BlockFace against)
    {
        return createDisplay(new PlacementData(spot, new Quaternionf(), against));
    }

    /**
     * Creates an itemDisplay with the given placement data for this item
     * @return UUID of created itemDisplay
     */
    default UUID createDisplay(Location spot, BlockPlaceEvent e)
    {
        BlockFace against = e.getBlockPlaced().getFace(e.getBlockAgainst());
        var displaySpot = getPlacementData(spot, e.getPlayer(), against);

        if(displaySpot == null)
            return null;

        return createDisplay(displaySpot);
    }

    /**
     * Creates an itemDisplay with the given placement data for this item
     * @return UUID of created itemDisplay
     */
    default UUID createDisplay(Location spot, PlayerInteractEvent e)
    {
        var displaySpot = getPlacementData(spot, e.getPlayer(), e.getBlockFace());

        if(displaySpot == null)
            return null;

        return createDisplay(displaySpot);
    }


    //endregion


    //endregion


    //region Helper

    /*=================================================================================================
                        -  Helper  -
    =================================================================================================*/

    /**
     * Gets the custom interaction entity at the block location if present
     * @param block block to check
     * @return The interaction entity, or null if not present or block is overriding it (like if it is a solid block)
     */
    default Interaction getInteraction(Block block)
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

    /**
     * @param entity Entity to check
     * @return whether the entity provided is an interactor of this item type
     */
    default boolean isPlacedItem(Entity entity)
    {
        //start with general entity type check
        if(!(entity instanceof Interaction interaction))
            return false;

        //then check if the entity has the pdc tag for this item
        return interaction.getPersistentDataContainer().has(getLibrary().identifier());
    }

    /**
     * @return Item Display entity linked to the provided interaction entity (null if none)
     */
    default ItemDisplay getDisplayEntity(Entity Interactor)
    {
        return (ItemDisplay) Bukkit.getEntity(getDisplayEntityID(Interactor));
    }

    /**
     * @return UUID of the Item Display entity linked to the provided interaction entity (null if none)
     */
    default UUID getDisplayEntityID(Entity Interactor)
    {
        if(!Interactor.getPersistentDataContainer().has(BasePlaceableItemModule.DisplayLinkNamespace))
            return null;

        return UUID.fromString(
                Interactor.getPersistentDataContainer()
                        .get(BasePlaceableItemModule.DisplayLinkNamespace, PersistentDataType.STRING)
        );
    }

    //endregion


    //region Placement

    /*=================================================================================================
                        -  Placement  -
    =================================================================================================*/


    @Override
    default boolean tryPlace(Block blockReplaced, BlockFace faceClicked, ItemStack itemPlaced, Player placer, Cancellable placementEvent) {

        //get placement location
        Location placeSpot = blockReplaced.getLocation();

        //check the block for nearby entities or blocks
        if((blockReplaced.getType() != Material.AIR &&
                !blockReplaced.isReplaceable()) ||
                placeSpot.getWorld().getNearbyEntities(BoundingBox.of(blockReplaced)).stream().anyMatch(
                        entity -> {
                            return entity instanceof Interaction interaction &&
                                    ItemManager.getLibrary(interaction) != null;
                        }
                )
        )
        {
            return false;
        }

        //remove the existing block if it was replaced
        blockReplaced.breakNaturally();

        //get the display spot now since we're going to need it to create the hitbox
        PlacementData displayInfo = getPlacementData(placeSpot, placer, faceClicked);

        if(displayInfo == null)
        {
            placementEvent.setCancelled(true);
            return false;
        }

        boolean isOnWall = switch (directionality())
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
            offset = hitboxWallOffset();

            offset.add(new Vector(0, -wallHeight()/2, -wallWidth()/2));

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
            offset.add(hitboxOffset());
        }
        else
        {
            offset = hitboxOffset();
            //if it is on the roof move the hitbox up
            if(displayInfo.faceOn().equals(BlockFace.DOWN))
            {
                offset.add(new Vector(0, 1 - height(), 0));
            }
        }

        hitboxCenter.add(offset);

        //create the hitbox
        Interaction interaction = blockReplaced.getWorld().spawn(
                hitboxCenter, Interaction.class
        );

        //set size of interaction
        interaction.setInteractionHeight(isOnWall ? wallHeight() : height());
        interaction.setInteractionWidth(isOnWall ? wallWidth() : width());

        PersistentDataContainer pdc = interaction.getPersistentDataContainer();

        NamespacedKey identifier = getLibrary().identifier();

        //set the generic tag to tell it is a custom block
        pdc.set(ItemLibrary.customItemTag, PersistentDataType.STRING, identifier.toString());

        //set this as the item (boolean and true don't really matter here we just check that it has this later)
        pdc.set(identifier, PersistentDataType.BOOLEAN, true);

        //tag this interactor with its display's UUID for access
        //also creates the item display
        pdc.set(
                BasePlaceableItemModule.DisplayLinkNamespace,
                PersistentDataType.STRING,
                createDisplay(displayInfo).toString()
        );

        //tag the block's pdc as a custom item

        //create the block's pdc
        PersistentDataContainer customBlockData = new CustomBlockData(blockReplaced, CustomItemsAPI.singleton);

        //set the generic tag to tell it is a custom block
        customBlockData.set(ItemLibrary.customItemTag, PersistentDataType.STRING, identifier.toString());

        //set this as the item (boolean and true don't really matter here we just check that it has this later)
        customBlockData.set(identifier, PersistentDataType.BOOLEAN, true);

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

        //remove the item if not in creative
        if(placer.getGameMode() != GameMode.CREATIVE)
            itemPlaced.setAmount(itemPlaced.getAmount()-1);

        Sound placeSound = placementSound();

        if(placeSound != null)
            placeSpot.getWorld().playSound(placeSpot, placeSound, 1, 1);

        //call anything set to happen when the block is placed
        onPlaced(blockReplaced, itemPlaced, placer);

        return true;
    }

    /**
     * Handles the placement of the item when a placement action is made this custom item
     * @param e
     * @param canUseItem
     */
    default void handleItemPlacementInteraction(PlayerInteractEvent e, Event.Result canUseItem) {
        if (canUseItem == Event.Result.DENY)
            return;

        //cancel the interaction event since we handle it here
        e.setCancelled(true);

        //gets the block which is being set
        assert e.getClickedBlock() != null;
        Block placeAtBlock = e.getClickedBlock().isReplaceable() ?
                e.getClickedBlock() :
                e.getClickedBlock().getRelative(e.getBlockFace());

        //try to place
        if (tryPlace(placeAtBlock, e.getBlockFace(), e.getItem(), e.getPlayer(), e))
        {
            //if placement was successful then swing the hand
            e.getPlayer().swingHand(e.getHand());
        }
    }

    //endregion


    //region Events

    /*=================================================================================================
                        -  Events  -
    =================================================================================================*/

    /**
     * Called when the interaction entity is broken (damaged by other entity).
     * The other entity is NOT always a player
     */
    @OverridingMethodsMustInvokeSuper
    default void onInteractorBroken(EntityDamageByEntityEvent e, Interaction interactionEntity)
    {
        if (e.isCancelled())
            return;

        naturalBlockBreak(interactionEntity,
                !(e.getDamager() instanceof Player player) || player.getGameMode() != GameMode.CREATIVE);
    }

    /**
     * Called when the interaction entity is interacted with by a player
     */
    default void onInteractorInteracted(PlayerInteractEntityEvent e, Interaction interactionEntity)
    {
        return;
    }

    /**
     * Called once an entity explodes with this in the (approximate) radius
     * @param e the Explosion Bukkit event
     * @param customInteraction The custom block which is set to be exploded
     * @return whether to remove it from the explosion list (if applicable), true = resist, false = explode
     */
    @OverridingMethodsMustInvokeSuper
    default void onEntityExplode(EntityExplodeEvent e, Interaction customInteraction)
    {
        onExplode(customInteraction);
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
    @OverridingMethodsMustInvokeSuper
    default void onBlockExplode(BlockExplodeEvent e, Interaction customInteraction)
    {
        onExplode(customInteraction);
    }

    /**
     * Called by default when an explosion reaches this block (power x 2).
     * Calls the naturalBreakBlock function if we are not explosion resistant
     */
    @OverridingMethodsMustInvokeSuper
    default void onExplode(Interaction explodedInteraction)
    {
        if(!explosionResistant())
        {
            //break it ourselves
            naturalBlockBreak(explodedInteraction, true);
        }
    }

    /**
     * Called when the block an instance of this custom item's placment is located at is updated by block physics
     * @param e BlockPhysicsEvent
     */
    @Override
    @OverridingMethodsMustInvokeSuper
    default void onBlockPhysicsUpdate(BlockPhysicsEvent e) {
        switch (directionality())
        {
            case WallBlock, WallUDBlock, WallDBlock: break;
            default : return;
        }

        if(placementDirection(e.getBlock()).getOppositeFace() == e.getBlock().getFace(e.getSourceBlock()))
        {
            //attached block broken
            if(!e.getSourceBlock().isBuildable())
            {
                //block is no longer solid so break this
                naturalBlockBreak(getInteraction(e.getBlock()), true);
            }
        }
    }


    //endregion


    //region Breaking

    /*=================================================================================================
                        -  Breaking  -
    =================================================================================================*/


    /**
     * Naturally removes the custom block and drops it's items
     * @param blockInteraction the custom block being broken
     */
    default void naturalBlockBreak(Interaction blockInteraction, boolean dropItem)
    {
        if(blockInteraction == null)
            return;

        if(isPlacedItem(blockInteraction))
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
                breakParticleMaterial().createBlockData()
        );

        //play the break sound
        offsetLocation.getWorld().playSound(
                offsetLocation,
                breakSound(),
                1f,
                1f
        );

        //then drop the block item if set to
        if(dropItem)
            dropPlaceableItem(blockInteraction.getLocation());
    }

    /**
     * Naturally removes the custom block from the given block and drops it's items
     * @param interactionBlock the custom block being broken
     */
    @Override
    default void naturalBlockBreak(Block interactionBlock, boolean dropItem)
    {
        naturalBlockBreak(getInteraction(interactionBlock), dropItem);
    }

    /**
     * Deletes the interaction placement and the display
     */
    default void delete(Interaction interaction)
    {
        //clear the custom data
        new CustomBlockData(interaction.getLocation().getBlock(), CustomItemsAPI.singleton).clear();
        removeLink(interaction);
        interaction.remove();
    }

    /**
     * Deletes display of the provided interaction entity
     */
    default void removeLink(Interaction interaction)
    {
        //get the attached entity
        String id = interaction.getPersistentDataContainer().get(BasePlaceableItemModule.DisplayLinkNamespace, PersistentDataType.STRING);

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

    //endregion


}
