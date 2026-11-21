package arnett.customItemsAPI.Listeners;

import arnett.customItemsAPI.Helpers.BlockHelper;
import arnett.customItemsAPI.items.modules.callbacks.ItemCallbackManager;
import arnett.customItemsAPI.items.modules.placeable.interactor.InteractorPlaceableItemLibrary;
import arnett.customItemsAPI.CustomItemsAPI;
import arnett.customItemsAPI.Helpers.WorldGuardHelper;
import arnett.customItemsAPI.ItemManager;
import arnett.customItemsAPI.items.modules.placeable.blockState.BlockStatePlaceableItemLibrary;
import arnett.customItemsAPI.items.modules.placeable.BasePlaceableItemModule;
import arnett.customItemsAPI.items.ItemLibrary;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class GeneralItemListener implements Listener {


    //region Player Place / Break

    /*=================================================================================================
                    -  Placement  -
    =================================================================================================*/

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e)
    {
        //check if this is taken by a custom block
        //do a thorough check here because we can't do the quick exit if it is not a listed material type
        if(ItemManager.getLibrary(e.getBlock()) != null)
        {
            e.setCancelled(true);
            return;
        }

        ItemLibrary lib = ItemManager.getLibrary(e.getItemInHand());

        //is this a custom item
        if(lib == null)
            return;

        if(lib instanceof BlockStatePlaceableItemLibrary)
            ItemCallbackManager.invokeEventCallbacks(lib, e);
    }

    @EventHandler
    public void onBlockRemovedByPlayer(BlockBreakEvent e)
    {
        //get custom data (getLibrary has a quick exit)
        ItemLibrary lib = ItemManager.getLibrary(e.getBlock());

        //is this a custom item
        if(lib == null)
            return;

        //call generic
        if(lib instanceof BasePlaceableItemModule basePlaceableItemModule)
        {
            //call item's break function
            basePlaceableItemModule.onPlacementBroken(e.getBlock(), e.getPlayer(), e);
            ItemCallbackManager.invokeEventCallbacks(lib, e);
        }

        //call specific
        if(lib instanceof BlockStatePlaceableItemLibrary blockStateData)
            //call item's break function
            blockStateData.onBlockBroken(e);

    }

    //endregion

    //also contains Placement for non-playable items
    //region Use

    /*=================================================================================================
                    -  Use  -
    =================================================================================================*/

    @EventHandler
    public void onInteract(PlayerInteractEvent e)
    {
        //block clicked
        blockInteractCheck(e);

        itemInteractCheck(e);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e)
    {
        //get item used
        ItemStack used = e.getPlayer().getInventory().getItem(e.getHand());

        ItemLibrary lib = ItemManager.getLibrary(used);

        //is this a custom item
        if(lib == null)
            return;

        //world guard check
        if(CustomItemsAPI.worldGuardEnabled &&
                !lib.overrideWorldGuardInteract() &&
                !WorldGuardHelper.canWorldGuardInteract(e.getPlayer(), e.getRightClicked().getLocation()))
        {
            e.setCancelled(true);
            return;
        }

        //call its use function
        ItemCallbackManager.invokeEventCallbacks(lib, e);
    }

    void blockInteractCheck(PlayerInteractEvent e)
    {
        if(e.getClickedBlock() == null)
            return;

        ItemLibrary data = ItemManager.getLibrary(e.getClickedBlock());

        //is this a custom item
        if(data == null)
            return;

        //world guard check
        if(!data.overrideWorldGuardInteract() &&
                !WorldGuardHelper.canWorldGuardInteract(e.getPlayer(), e.getClickedBlock().getLocation()))
        {
            e.setCancelled(true);
            return;
        }

        //they are interacting with a placeable block
        if(data instanceof BlockStatePlaceableItemLibrary blockStateData)
        {
            //call item's interact function
            blockStateData.onBlockInteracted(e);
        }
    }

    void itemInteractCheck(PlayerInteractEvent e)
    {
        //check the item used
        ItemLibrary lib = ItemManager.getLibrary(e.getItem());

        //is this a custom item
        if(lib == null)
            return;

        if(lib instanceof BasePlaceableItemModule && isPlacementInteraction(e))
        {
            //call item's place function
            ItemCallbackManager.invokeEventCallbacks(lib, e, "place");
        }
        else
        {
            //call its use function
            ItemCallbackManager.invokeEventCallbacks(lib, e);
        }
    }

    boolean isPlacementInteraction(PlayerInteractEvent e)
    {
        Block clicked = e.getClickedBlock();

        //is this something that can be placed on?
        if(clicked == null)
            return false;

        //if the block's interact function doesn't prevent placing then continue
        if(e.useItemInHand() == Event.Result.DENY || e.getAction().isLeftClick())
            return false;

        //if the player is sneaking and the block interaction overrides the placement cancel
        if(!e.getPlayer().isSneaking() && BlockHelper.isUsable(e.getClickedBlock()))
        {
            return false;
        }

        //world guard check
        if(CustomItemsAPI.worldGuardEnabled &&
                !WorldGuardHelper.canWorldGuardPlace(e.getPlayer(), e.getInteractionPoint()))
        {
            e.setCancelled(true);
            return false;
        }

        //all checks passed this is a placement interaction
        return true;
    }

    @EventHandler
    public void onEntityAttackEntity(EntityDamageByEntityEvent e)
    {
        if(!(e.getDamager() instanceof LivingEntity living))
            return;

        var equipment = living.getEquipment();
        if (equipment == null)
            return;

        var item = equipment.getItemInMainHand();
        ItemLibrary lib = ItemManager.getLibrary(item);

        if(lib != null)
        {
            ItemCallbackManager.invokeEventCallbacks(lib, e);
            ItemCallbackManager.invokeEventCallbacks(lib, e, "item", item);
        }
    }

    //endregion


    //region Copy

    /*=================================================================================================
                    -  Copy  -
    =================================================================================================*/

    @EventHandler
    public void onCopy(PlayerPickItemEvent e)
    {
        if(e.isCancelled())
            return;

        Player player = e.getPlayer();

        //get what was selected
        var results = e.getPlayer().getWorld().rayTrace(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                (int)player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE).getValue(),
                FluidCollisionMode.NEVER,
                false,
                .1,
                entity -> entity != player
        );

        if(results != null && results.getHitBlock() != null)
        {
            ItemLibrary lib = ItemManager.getLibrary(results.getHitBlock());

            //is this a custom item
            if(lib == null)
                return;

            if(!(lib instanceof BasePlaceableItemModule))
                return;

            //call callbacks
            ItemCallbackManager.invokeEventCallbacks(lib, e);
        }
    }

    //endregion


    //region Block Events

    /*=================================================================================================
                    -  Block Events  -
    =================================================================================================*/

    @EventHandler
    public void onPhysicsEvent(BlockPhysicsEvent e)
    {
        if(e.isCancelled())
            return;

        ItemLibrary lib = ItemManager.getLibrary(e.getBlock());

        //handles 99% of cases without a custom block in O(1)
        if(lib == null)
            return;

        if(lib instanceof BasePlaceableItemModule)
            ItemCallbackManager.invokeEventCallbacks(lib, e);

    }

    //endregion


    //region Explosions

    /*=================================================================================================
                    -  Explosions  -
    =================================================================================================*/

    //when an entity explodes
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e)
    {
        if(e.isCancelled())
            return;

        // check the surrounding blocks
        e.blockList().forEach(block ->{
            //check if it is a custom block
            ItemLibrary lib = ItemManager.getLibrary(block);

            //not a custom item
            if(lib == null)
                return;

            //it is a custom item
            if(lib instanceof BlockStatePlaceableItemLibrary blockStateLib)
            {
                ItemCallbackManager.invokeEventCallbacks(lib, e);
                ItemCallbackManager.invokeEventCallbacks(lib, e, "block", block);
                blockStateLib.explodedLogic(e, block);
            }
        });

        //check for nearby interactors

        //get the radius first since there's no getRadius on this thing >:(
        double radius = 4.0f;

        if(e.getEntity() instanceof Explosive exp)
        {
            if(e.getEntity().getType() == EntityType.BREEZE_WIND_CHARGE ||
                    e.getEntity().getType() ==EntityType.WIND_CHARGE)
            {
                return;
            }
            //yield for explosive is different from the event which gives the block break %
            radius = exp.getYield() * .75f;
        }
        else if(e.getEntity().getType() == EntityType.WITHER_SKULL)
        {
            radius = 2;
        }
        else if(e.getEntity().getType() == EntityType.END_CRYSTAL)
        {
            radius = 6;
        }
        else if(e.getEntity() != null && e.getEntity().getPersistentDataContainer().has(BasePlaceableItemModule.explosiveRangeNamespace))
        {
            //maybe a custom explosive, so check it's explosive pdc
            radius = e.getEntity().getPersistentDataContainer().get(
                    BasePlaceableItemModule.explosiveRangeNamespace, PersistentDataType.FLOAT
            );
        }

        Collection<Interaction> interactions = e.getLocation().getWorld().getNearbyEntitiesByType(
                Interaction.class,
                e.getLocation(),
                radius
        );

        interactions.forEach(interaction -> {
            ItemLibrary lib = ItemManager.getLibrary(interaction);

            if(lib == null)
                return;

            if(lib instanceof InteractorPlaceableItemLibrary interactorLibrary)
            {
                ItemCallbackManager.invokeEventCallbacks(lib, e);
                ItemCallbackManager.invokeEventCallbacks(lib, e, "interaction", interaction);
                interactorLibrary.explodedLogic(e, interaction);
            }
        });
    }

    //when a block explodes
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e)
    {
        if(e.isCancelled())
            return;

        // check the surrounding blocks
        e.blockList().forEach(block ->{
            //check if it is a custom block
            ItemLibrary lib = ItemManager.getLibrary(block);

            //not a custom item
            if(lib == null)
                return;

            //it is a custom item
            if(lib instanceof BlockStatePlaceableItemLibrary blockStateLib)
            {
                ItemCallbackManager.invokeEventCallbacks(lib, e);
                ItemCallbackManager.invokeEventCallbacks(lib, e, "block", block);
                blockStateLib.explodedLogic(e, block);
            }

            //some other case
            return;
        });

        //check for nearby interactors

        //find the radius

        double radius = 4;
        if(e.getBlock().getType().toString().contains("BED"))
        {
            radius = 6;
        }
        else if(e.getBlock().getType() == Material.RESPAWN_ANCHOR)
        {
            radius = 6;
        }

        Collection<Interaction> interactions = e.getBlock().getWorld().getNearbyEntitiesByType(
                Interaction.class,
                e.getBlock().getLocation(),
                radius
        );

        interactions.forEach(interaction -> {
            ItemLibrary lib = ItemManager.getLibrary(interaction);

            if(lib == null)
                return;

            if(lib instanceof InteractorPlaceableItemLibrary interactorLibrary)
            {
                ItemCallbackManager.invokeEventCallbacks(lib, e);
                ItemCallbackManager.invokeEventCallbacks(lib, e, "interaction", interaction);
                interactorLibrary.explodedLogic(e, interaction);
            }
        });

    }

    //endregion


    //region Entity

    /*=================================================================================================
                    -  Entity  -
    =================================================================================================*/

    @EventHandler
    public void entityDamageEntity(EntityDamageByEntityEvent e)
    {
        //if it is not damaged by one of the destruction events don't do anything
        if(!(e.getDamager() instanceof Player ||
                e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION))
        {
            return;
        }

        if(!(e.getEntity() instanceof Interaction interaction))
        {
            return;
        }

        ItemLibrary lib = ItemManager.getLibrary(interaction);

        if(!(lib instanceof InteractorPlaceableItemLibrary interactorLibrary))
        {
            return;
        }

        //world guard check
        //maybe they are trying to place it
        if(e.getDamager() instanceof Player player &&
                CustomItemsAPI.worldGuardEnabled &&
                !WorldGuardHelper.canWorldGuardBreak(player, e.getEntity().getLocation()))
        {
            e.setCancelled(true);
            return;
        }

        ItemCallbackManager.invokeEventCallbacks(lib, e, "breaking");
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e)
    {
        if(!(e.getRightClicked() instanceof Interaction interaction))
            return;

        ItemLibrary lib = ItemManager.getLibrary(interaction);

        if(!(lib instanceof InteractorPlaceableItemLibrary interactorLibrary))
        {
            return;
        }

        //world guard check
        if(!lib.overrideWorldGuardInteract() &&
                !WorldGuardHelper.canWorldGuardInteract(e.getPlayer(), e.getRightClicked().getLocation()))
        {
            e.setCancelled(true);
            return;
        }

        ItemCallbackManager.invokeEventCallbacks(lib, e, "placeable");
    }

    //endregion


    //region Piston Push / Pull

    /*=================================================================================================
                    -  Piston Push / Pull  -
    =================================================================================================*/

    @EventHandler
    public void onPistonPush(BlockPistonExtendEvent e)
    {
        //No blocks being pushed
        if(e.getBlocks().isEmpty())
        {
            Block tip = e.getBlock().getRelative(e.getDirection());

            ItemLibrary lib = ItemManager.getLibrary(tip);

            if(lib == null)
                return;

            if(!(lib instanceof BasePlaceableItemModule blockLib))
                return;

            e.setCancelled(switch (blockLib.pistonPushable())
            {
                case BREAK -> {
                    blockLib.naturalBlockBreak(tip, true);
                    yield false;
                }
                case BLOCK, MOVE -> true;
                default -> true;
            });
        }

        //blocks being pushed
        else {

            HashSet<Block> processed = new HashSet<>();
            HashSet<Block> into = new HashSet<>();

            //check the blocks being pushed if they are a custom block
            if(e.getBlocks().stream().anyMatch(block -> {


                processed.add(block);
                into.add(block.getRelative(e.getDirection()));

                ItemLibrary lib = ItemManager.getLibrary(block);

                if(lib == null)
                    return false;

                //check the current block
                if(!(lib instanceof BasePlaceableItemModule blockLib))
                    return false;
                else
                {
                    //block is custom
                    return switch (blockLib.pistonPushable())
                    {
                        case BREAK -> {
                            blockLib.naturalBlockBreak(block, true);
                            yield false;
                        }
                        case BLOCK, MOVE -> true;
                        default -> true;
                    };
                }


            }))
            {
                e.setCancelled(true);
            }

            //blocks being pushed into
            else
            {


                into.removeAll(processed);

                for(Block block : into)
                {
                    ItemLibrary lib = ItemManager.getLibrary(block);

                    if(lib == null)
                        continue;

                    if(!(lib instanceof BasePlaceableItemModule blockLib))
                        continue;

                    if(switch (blockLib.pistonPushable())
                    {
                        case BREAK -> {
                            blockLib.naturalBlockBreak(block, true);
                            yield false;
                        }
                        case BLOCK, MOVE -> true;
                        default -> true;
                    })
                    {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }

    }

    @EventHandler
    public void onPistonPull(BlockPistonRetractEvent e)
    {
        for(Block block : e.getBlocks())
        {
            if (ItemManager.getLibrary(block) instanceof BlockStatePlaceableItemLibrary lib)
            {
                e.setCancelled(true);
            }
        }
    }

    //endregion


    //region CustomBlockData

    /*=================================================================================================
                    -  CustomBlockData  -
    =================================================================================================*/

    @EventHandler
    public void onDataRemove(CustomBlockDataRemoveEvent e) {
        ItemLibrary lib = ItemManager.getLibrary(e.getBlock());
        if(lib instanceof InteractorPlaceableItemLibrary)
        {
            ((InteractorPlaceableItemLibrary) lib).naturalBlockBreak(e.getBlock(), true);
        }
    }

    //endregion


    //region Crafting

    /*=================================================================================================
                    -  Crafting  -
    =================================================================================================*/

    @EventHandler
    public void onItemCraftPrepare(PrepareItemCraftEvent e)
    {
        if(hasNonBaseCraftable(e.getInventory().getContents()))
        {
            e.getInventory().setResult(null);
        }
    }

    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent e)
    {
        if(!(e.getBlock() instanceof Crafter crafter))
        {
            return;
        }

        if(hasNonBaseCraftable(crafter.getInventory().getContents()))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCraftTable(CraftItemEvent e)
    {
        if(hasNonBaseCraftable(e.getInventory().getContents()))
        {
            e.setCancelled(true);
        }
    }

    boolean hasNonBaseCraftable(ItemStack[] recipeContents)
    {
        for(ItemStack stack : recipeContents)
        {

            ItemLibrary lib = ItemManager.getLibrary(stack);

            if(lib != null)
            {
                //remove the result
                return !lib.keepBaseCrafts();
            }
        }

        return false;
    }

    //endregion


    //region Block Formation

    /*=================================================================================================
                    -  Block Formation  -
    =================================================================================================*/

    @EventHandler
    public void onBlockForm(BlockFormEvent e)
    {
        //get library
        ItemLibrary lib = ItemManager.getLibrary(e.getBlock());

        if(lib instanceof InteractorPlaceableItemLibrary interactor)
        {
            interactor.naturalBlockBreak(e.getBlock(), true);
        }
    }

    //endregion


    //region Inventory Movements

    /*=================================================================================================
                       -  Inventory Movements  -
    =================================================================================================*/

    HashMap<UUID, ItemStack[]> inventoryContentCounter = new HashMap<>();

    void prepForInventorySync(Player player)
    {
        if(!ItemManager.listenToInventoryMovement)
            return;

        UUID playerId = player.getUniqueId();
        inventoryContentCounter.put(playerId, player.getInventory().getContents());

        Bukkit.getScheduler().runTaskLater(CustomItemsAPI.singleton, (task) -> {
            inventoryContentCounter.remove(playerId);
        }, 1);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e)
    {
        prepForInventorySync(e.getPlayer());
    }

    @EventHandler
    public void onPlayerOpenInventory(InventoryOpenEvent e)
    {
        if (e.getPlayer() instanceof Player player) {
            prepForInventorySync(player);
        }
    }

    @EventHandler
    public void onPlayerInventorySlotChange(PlayerInventorySlotChangeEvent e)
    {
        //only run this expensive listener if there is something we need to listen for
        if(!ItemManager.listenToInventoryMovement)
            return;

        //skip this if it is a syncing thing (don't ask)
        ItemStack[] existingInventoryData = inventoryContentCounter.get(e.getPlayer().getUniqueId());

        if(existingInventoryData != null && e.getSlot() < existingInventoryData.length)
        {
            ItemStack existingItem = existingInventoryData[e.getSlot()];

            System.out.println(existingItem);

            if(existingItem != null && existingItem.isSimilar(e.getNewItemStack()))
            {
                return;
            }
        }

        ItemStack newStack = e.getNewItemStack();
        ItemStack oldStack = e.getOldItemStack();

        ItemLibrary newLib = ItemManager.getLibrary(newStack);
        ItemLibrary oldLib = ItemManager.getLibrary(oldStack);

        boolean newIsTracked = newLib != null && newLib.trackInventoryMovements();
        boolean oldIsTracked = oldLib != null && oldLib.trackInventoryMovements();

        //if both of these are the same custom items make sure they're not stacking
        if(newIsTracked && oldIsTracked && newStack.isSimilar(oldStack))
        {
            int difference = newStack.getAmount() - oldStack.getAmount();

            if(difference > 0)
            {
                ItemCallbackManager.invokeEventCallbacks(newLib, e);
                ItemCallbackManager.invokeEventCallbacks(newLib, e, "enter");
            }
            else
            {
                ItemCallbackManager.invokeEventCallbacks(oldLib, e);
                ItemCallbackManager.invokeEventCallbacks(oldLib, e, "exit");
            }
        }
        else
        {
            if(newIsTracked)
            {
                ItemCallbackManager.invokeEventCallbacks(newLib, e);
                ItemCallbackManager.invokeEventCallbacks(newLib, e, "enter");
            }
            else if(oldIsTracked)
            {
                ItemCallbackManager.invokeEventCallbacks(oldLib, e);
                ItemCallbackManager.invokeEventCallbacks(oldLib, e, "exit");
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e)
    {
        ItemLibrary lib = ItemManager.getLibrary(e.getItemDrop().getItemStack());

        if(lib == null)
            return;

        ItemCallbackManager.invokeEventCallbacks(lib, e);
    }


    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent e)
    {
        ItemLibrary lib = ItemManager.getLibrary(e.getItem().getItemStack());

        if(lib == null)
            return;

        ItemCallbackManager.invokeEventCallbacks(lib, e);

        if(e.getEntity() instanceof Player player)
            ItemCallbackManager.invokeEventCallbacks(lib, e, "player", player);
    }



    //endregion

}
