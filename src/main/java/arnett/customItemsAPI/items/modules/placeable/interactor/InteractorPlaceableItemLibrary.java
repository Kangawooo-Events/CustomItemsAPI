package arnett.customItemsAPI.items.modules.placeable.interactor;

import arnett.customItemsAPI.items.modules.callbacks.ItemCallbackManager;
import arnett.customItemsAPI.items.modules.callbacks.ItemEventPriority;
import arnett.customItemsAPI.items.modules.callbacks.annotations.ItemEvent;
import arnett.customItemsAPI.items.modules.callbacks.annotations.OnBlockDropItems;
import arnett.customItemsAPI.items.modules.callbacks.annotations.OnInteractionDropItems;
import arnett.customItemsAPI.items.modules.callbacks.annotations.OnInteractionPlaced;
import arnett.customItemsAPI.items.modules.placeable.BasePlaceableItemModule;
import arnett.customItemsAPI.items.modules.placeable.PlacementData;
import arnett.customItemsAPI.items.ItemLibrary;
import arnett.customItemsAPI.CustomItemsAPI;
import arnett.customItemsAPI.ItemManager;
import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class InteractorPlaceableItemLibrary extends ItemLibrary implements BasePlaceableItemModule {

    NamespacedKey InteractorLinkNamespace =  new NamespacedKey("customitems", "linkedinteractor");


    //region Properties

    /*=================================================================================================
                        -  Properties  -
    =================================================================================================*/


    /**
     * @return Namespaced Key which leads to the block display's resource in a resource pack
     */
    public abstract NamespacedKey displayModelKey();

    /**
     * @return Width of hitbox when placed on wall (Minecraft forces this size on both x and z axis)
     */
    public abstract float hitboxHeight();


    /**
     * @return Height of hitbox
     */
    public abstract float hitboxWidth();

    //endregion


    //region Optional Properties

    /*=================================================================================================
                        -  Optional Properties  -
    =================================================================================================*/

    /**
     * @return Key to item resource to be used as item display model when placed on wall
     * (defaults to displayModelKey())
     */
    public NamespacedKey wallDisplayModelKey()
    {
        return displayModelKey();
    }

    /**
     * @return Key to item resource to be used as item display model when placed on wall
     * (defaults to displayModelKey())
     */
    public NamespacedKey roofDisplayModelKey()
    {
        return displayModelKey();
    }

    /**
     * @return Width of hitbox when placed on wall (Minecraft forces this size on both x and z axis)
     */
    public float wallHeight()
    {
        return hitboxWidth();
    }

    /**
     * @return Height of hitbox when placed on wall
     */
    public float wallWidth()
    {
        return hitboxHeight();
    }

    /**
     * @return Offset of hitbox (on regular axis)
     */
    public Vector hitboxOffset()
    {
        return new Vector(.5f, 0, .5f);
    }

    /**
     * @return Offset of hitbox when placed on a wall (on regular axis)
     */
    public Vector hitboxWallOffset()
    {
        return new Vector(0, .5f, .5f);
    }

    /**
     * returns the dropped items when a custom placement is broken, this combines with the regular block version
     * @param interaction custom interaction which was broken
     * @return List of items that will be dropped on breaking
     */
    abstract List<ItemStack> basicDroppedItems(Interaction interaction);

    @Override
    public List<ItemStack> basicDroppedItems(Block fromBlock) {
        return List.of();
    }

    @Override
    public Material placementMaterial() {
        return Material.AIR;
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
    public UUID createDisplay(PlacementData placementData)
    {
        //create the display entity at the offset
        ItemDisplay display = placementData.location().getWorld().spawn(
                placementData.location().clone().add(displayOffset()), ItemDisplay.class, (e) -> {
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
    public UUID createDisplay(Location spot, BlockFace against)
    {
        return createDisplay(new PlacementData(spot, new Quaternionf(), against));
    }

    /**
     * Creates an itemDisplay with the given placement data for this item
     * @return UUID of created itemDisplay
     */
    public UUID createDisplay(Location spot, BlockPlaceEvent e)
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
    public UUID createDisplay(Location spot, PlayerInteractEvent e)
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
    public Interaction getInteraction(Block block)
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
    public boolean isPlacedItem(Entity entity)
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
    public ItemDisplay getDisplayEntity(Entity Interactor)
    {
        return (ItemDisplay) Bukkit.getEntity(getDisplayEntityID(Interactor));
    }

    /**
     * @return UUID of the Item Display entity linked to the provided interaction entity (null if none)
     */
    public UUID getDisplayEntityID(Entity Interactor)
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


    /**
     * Handles placement logic
     * @param blockReplaced The block which was replaced with the placed block
     * @param faceClicked Face of the block which was clicked in the process of placing this
     * @param itemPlaced The item which placed
     * @param placer
     * @param placementEvent generic cancelable placement event
     * @return was the placement a success
     */
    @Override
    public boolean place(Block blockReplaced, BlockState oldBlockState, BlockFace faceClicked, ItemStack itemPlaced, Player placer, Cancellable placementEvent, boolean alreadyDroppedItem) {

        //get placement location
        Location placeSpot = blockReplaced.getLocation();

        //check the block for nearby entities or blocks
        if((oldBlockState.getType() != Material.AIR &&
                !oldBlockState.getBlockData().isReplaceable()) ||
                placeSpot.getWorld().getNearbyEntities(BoundingBox.of(blockReplaced)).stream().anyMatch(
                        entity -> entity instanceof Interaction interaction &&
                                ItemManager.getLibrary(interaction) != null
                )
        )
        {
            return false;
        }

        //remove the existing block if it was replaced
        if(!alreadyDroppedItem)
        {
            blockReplaced.breakNaturally();
        }

        //set air just to catch any placement
        blockReplaced.setType(Material.AIR);

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
                offset.add(new Vector(0, 1 - hitboxHeight(), 0));
            }
        }

        hitboxCenter.add(offset);

        //create the hitbox
        Interaction interaction = blockReplaced.getWorld().spawn(
                hitboxCenter, Interaction.class
        );

        //set size of interaction
        interaction.setInteractionHeight(isOnWall ? wallHeight() : hitboxHeight());
        interaction.setInteractionWidth(isOnWall ? wallWidth() : hitboxWidth());

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

        //call other modifications
        modifyNewPlacementItemData(customBlockData, itemPlaced, placer);

        //remove the item if not in creative
        if(placer.getGameMode() != GameMode.CREATIVE)
            itemPlaced.setAmount(itemPlaced.getAmount()-1);

        Sound placeSound = placementSound();

        if(placeSound != null)
            placeSpot.getWorld().playSound(placeSpot, placeSound, 1, 1);

        //call anything set to happen when the block is placed
        onPlaced(blockReplaced, itemPlaced, placer);
        ItemCallbackManager.invokeCallbacks(this, OnInteractionPlaced.class, null, interaction, itemPlaced, placer);

        return true;
    }

    //endregion


    //region Events

    /*=================================================================================================
                        -  Events  -
    =================================================================================================*/

    /**
     * Called when the interaction entity is broken (damaged by other entity), Before breaking logic is done on LATE priority.
     * The other entity is NOT always a player. (also .getEntity() should be the interaction paper api isn't that clear here)
     */
    @ItemEvent("breaking")
    public void onInteractorBroken(EntityDamageByEntityEvent e) {}

    /**
     * Called when the interaction entity is interacted with by a player
     */
    @ItemEvent("placeable")
    public void onInteractorInteracted(PlayerInteractAtEntityEvent e) {}

    /**
     * Called once an entity explodes with this in the (approximate) radius before explodedLogic runs
     * @param e the Explosion Bukkit event
     */
    @ItemEvent
    public void onEntityExplode(EntityExplodeEvent e) {}

    /**
     * Called once a block explodes with this in the radius before explodedLogic runs
     * @param e the Explosion Bukkit event
     */
    @ItemEvent
    public void onBlockExplode(BlockExplodeEvent e) {}

    /**
     * Called once an entity explodes with this in the (approximate) radius before explodedLogic runs
     * @param e the Explosion Bukkit event
     * @param customInteraction The custom block which is set to be exploded
     */
    @ItemEvent("interaction")
    public void onEntityExplode(EntityExplodeEvent e, Interaction customInteraction) {}

    /**
     * Called once a block explodes with this in the radius before explodedLogic runs
     * @param e the Explosion Bukkit event
     * @param customInteraction The custom block which is set to be exploded
     */
    @ItemEvent("interaction")
    public void onBlockExplode(BlockExplodeEvent e, Interaction customInteraction) {}

    /**
     * Called after the interaction is placed
     * @param interaction interaction which was placed
     * @param placer player who placed the interaction
     */
    @OnInteractionPlaced
    public void onPlaced(Interaction interaction, ItemStack placed, Player placer){}

    //endregion


    //region Logic By Events

    /*=================================================================================================
                        -  Logic By Events  -
    =================================================================================================*/


    /**
     * Called when the interaction entity is broken (damaged by other entity), on LATE priority.
     * The other entity is NOT always a player
     */
    @ItemEvent(value = "breaking", priority = ItemEventPriority.LATE)
    public void interactorBrokenLogic(EntityDamageByEntityEvent e)
    {
        //call generic breaking event fist
        onPlacementBroken(e.getEntity().getLocation().getBlock(), e.getDamager(), e);

        if (e.isCancelled())
            return;

        naturalBlockBreak((Interaction)e.getEntity(),
                !(e.getDamager() instanceof Player player) || player.getGameMode() != GameMode.CREATIVE);
    }

    /**
     * when an explosion reaches this block.
     * Calls the naturalBreakBlock function if we are not explosion resistant
     */
    public void explodedLogic(Cancellable e, Interaction explodedInteraction)
    {
        if(e.isCancelled())
            return;

        if(!explosionResistant())
        {
            //break it ourselves
            naturalBlockBreak(explodedInteraction, true);
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
    public void naturalBlockBreak(Interaction blockInteraction, boolean dropItem)
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

        System.out.println("BreAKING Interaction");
        System.out.println("BreAKING Interaction");

        //then drop the block item if set to
        if(dropItem)
        {
            System.out.println("Dropping it3m " + offsetLocation);
            var world = blockInteraction.getWorld();
            Block block = blockInteraction.getLocation().getBlock();

            //interaction drops
            for (ItemStack droppedItem : basicDroppedItems(blockInteraction)) {
                world.dropItemNaturally(offsetLocation, droppedItem);
            }
            //generic drops
            for (ItemStack droppedItem : basicDroppedItems(block)) {
                world.dropItemNaturally(offsetLocation, droppedItem);
            }


            //get callback items
            ArrayList<ItemStack> CallbackItems = new ArrayList<>();
            ItemCallbackManager.invokeCallbacks(
                    this,
                    OnBlockDropItems.class,
                    (a, ann) -> CallbackItems.addAll((List<ItemStack>)a),
                    block
            );
            ItemCallbackManager.invokeCallbacks(
                    this,
                    OnInteractionDropItems.class,
                    (a, ann) -> CallbackItems.addAll((List<ItemStack>)a),
                    blockInteraction
            );

            //drop callback items
            for (ItemStack droppedItem : CallbackItems) {
                world.dropItemNaturally(offsetLocation, droppedItem);
            }
        }
    }

    /**
     * Naturally removes the custom block from the given block and drops it's items
     * @param interactionBlock the custom block being broken
     */
    @Override
    public void naturalBlockBreak(Block interactionBlock, boolean dropItem)
    {
        naturalBlockBreak(getInteraction(interactionBlock), dropItem);
    }

    /**
     * Deletes the interaction placement and the display
     */
    public void delete(Interaction interaction)
    {
        //clear the custom data
        new CustomBlockData(interaction.getLocation().getBlock(), CustomItemsAPI.singleton).clear();
        removeLink(interaction);
        interaction.remove();
    }

    /**
     * Deletes display of the provided interaction entity
     */
    public void removeLink(Interaction interaction)
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


    //region Placement Data

    /*=================================================================================================
                        -  Placement Data  -
    =================================================================================================*/

    @Override
    public PersistentDataContainer getPlacedItemData(Block placedItem) {
        return getPlacedItemData(getInteraction(placedItem));
    }


    public PersistentDataContainer getPlacedItemData(Interaction placedItem) {
        return placedItem.getPersistentDataContainer();
    }

    //endregion

}
