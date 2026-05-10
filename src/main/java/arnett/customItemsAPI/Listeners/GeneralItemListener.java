package arnett.customItemsAPI.Listeners;

import arnett.customItemsAPI.Helpers.BlockHelper;
import arnett.customItemsAPI.ItemLibraries.CustomBlockTypes.Interactable.InteractorLibrary;
import arnett.customItemsAPI.ItemLibraries.CustomBlockTypes.PlacementHelper;
import arnett.customItemsAPI.CustomItemsAPI;
import arnett.customItemsAPI.Helpers.WorldGuardHelper;
import arnett.customItemsAPI.ItemManager;
import arnett.customItemsAPI.ItemLibraries.CustomBlockTypes.BlockState.BlockStateLibrary;
import arnett.customItemsAPI.ItemLibraries.CustomBlockTypes.PlaceableLibrary;
import arnett.customItemsAPI.ItemLibraries.ItemLibrary;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import io.papermc.paper.event.player.PlayerPickItemEvent;
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
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.HashSet;

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

        ItemLibrary data = ItemManager.getLibrary(e.getItemInHand());

        //is this a custom item
        if(data == null)
            return;

        if(data instanceof BlockStateLibrary blockData)
            blockData.onBlockPlaced(e);
    }

    @EventHandler
    public void onBlockRemovedByPlayer(BlockBreakEvent e)
    {
        //get custom data (getLibrary has a quick exit)
        ItemLibrary data = ItemManager.getLibrary(e.getBlock());

        //is this a custom item
        if(data == null)
            return;

        if(data instanceof BlockStateLibrary blockStateData)
            //call item's break function
            blockStateData.onBlockBroken(e);

    }

    //endregion

    //also contains Placement for non-placable items
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

        ItemLibrary data = ItemManager.getLibrary(used);

        //is this a custom item
        if(data == null)
            return;

        if(CustomItemsAPI.worldGuardEnabled &&
                !data.overrideWorldGuardInteract() &&
                !WorldGuardHelper.canWorldGuardInteract(e.getPlayer(), e.getRightClicked().getLocation()))
        {
            e.setCancelled(true);
            return;
        }

        //call its use function
        data.onItemUsedOnEntity(e);
    }

    void blockInteractCheck(PlayerInteractEvent e)
    {
        if(e.getClickedBlock() == null)
            return;

        ItemLibrary data = ItemManager.getLibrary(e.getClickedBlock());

        //is this a custom item
        if(data == null)
            return;

        if(!data.overrideWorldGuardInteract() &&
                !WorldGuardHelper.canWorldGuardInteract(e.getPlayer(), e.getClickedBlock().getLocation()))
        {
            e.setCancelled(true);
            return;
        }

        //they are interacting with a placeable block
        if(data instanceof BlockStateLibrary blockStateData)
        {
            //call item's interact function
            blockStateData.onBlockInteracted(e, e.useInteractedBlock());
        }
    }

    void itemInteractCheck(PlayerInteractEvent e)
    {
        //check the item used
        ItemLibrary data = ItemManager.getLibrary(e.getItem());

        //is this a custom item
        if(data == null)
            return;

        //call its use function
        data.onItemUsed(e);

        Block clicked = e.getClickedBlock();

        //is this something that can be placed on?
        if(clicked == null)
            return;

        //if the block's interact function doesn't prevent placing then continue
        if(e.useItemInHand() == Event.Result.DENY || e.getAction().isLeftClick())
            return;


        if(!e.getPlayer().isSneaking() && BlockHelper.isUsable(e.getClickedBlock()))
        {
            return;
        }

        //maybe they are trying to place it
        if(CustomItemsAPI.worldGuardEnabled &&
                !WorldGuardHelper.canWorldGuardPlace(e.getPlayer(), e.getInteractionPoint()))
        {
            e.setCancelled(true);
            return;
        }

        //possibly they are trying to place an interactable item since this doesn't need to be a placeable block state
        if(data instanceof InteractorLibrary interactorData)
        {
            //call item's place function
            interactorData.onItemPlacementInteraction(e, e.useItemInHand());
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
            ItemLibrary data = ItemManager.getLibrary(results.getHitBlock());

            //is this a custom item
            if(data == null)
                return;

            if(!(data instanceof PlaceableLibrary placeableData))
                return;

            //call item's copy function
            placeableData.onCopy(e, results.getHitEntity(), results.getHitBlock());
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

        ItemLibrary data = ItemManager.getLibrary(e.getBlock());

        //handles 99% of cases without a custom block in O(1)
        if(data == null)
            return;

        if(data instanceof PlaceableLibrary placeable)
            placeable.onBlockPhysicsUpdate(e);

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
        e.blockList().removeIf(block ->{
            //check if it is a custom block
            ItemLibrary lib = ItemManager.getLibrary(block);

            //not a custom item
            if(lib == null)
                return false;

            //it is a custom item
            if(lib instanceof BlockStateLibrary blockStateLib)
            {
                return blockStateLib.onEntityExplode(e, block);
            }

            //some other case
            return false;
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
        else if(e.getEntity() != null && e.getEntity().getPersistentDataContainer().has(PlaceableLibrary.explosiveRangeNamespace))
        {
            //probably a custom explosive, so check it's explosive pdc
            radius = e.getEntity().getPersistentDataContainer().get(
                    PlaceableLibrary.explosiveRangeNamespace, PersistentDataType.FLOAT
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

            if(lib instanceof InteractorLibrary interactorLibrary)
            {
                interactorLibrary.onEntityExplode(e, interaction);
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
        e.blockList().removeIf(block ->{
            //check if it is a custom block
            ItemLibrary lib = ItemManager.getLibrary(block);

            //not a custom item
            if(lib == null)
                return false;

            //it is a custom item
            if(lib instanceof BlockStateLibrary blockStateLib)
            {
                return blockStateLib.onBlockExplode(e, block);
            }

            //some other case
            return false;
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

            if(lib instanceof InteractorLibrary interactorLibrary)
            {
                interactorLibrary.onBlockExplode(e, interaction);
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
        //if it is not damaged by one of there destruction events don't do anything
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

        if(!(lib instanceof InteractorLibrary interactorLibrary))
        {
            return;
        }

        //maybe they are trying to place it
        if(e.getDamager() instanceof Player player &&
                CustomItemsAPI.worldGuardEnabled &&
                !WorldGuardHelper.canWorldGuardBreak(player, e.getEntity().getLocation()))
        {
            e.setCancelled(true);
            return;
        }


        interactorLibrary.onInteractorBroken(e, interaction);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e)
    {
        if(!(e.getRightClicked() instanceof Interaction interaction))
            return;

        ItemLibrary lib = ItemManager.getLibrary(interaction);

        if(!(lib instanceof InteractorLibrary interactorLibrary))
        {
            return;
        }

        if(!lib.overrideWorldGuardInteract() &&
                !WorldGuardHelper.canWorldGuardInteract(e.getPlayer(), e.getRightClicked().getLocation()))
        {
            e.setCancelled(true);
            return;
        }

        interactorLibrary.onInteractorInteracted(e, interaction);
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

            if(!(lib instanceof PlaceableLibrary blockLib))
                return;

            e.setCancelled(switch (blockLib.getPistonPushable())
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
                if(!(lib instanceof PlaceableLibrary blockLib))
                    return false;
                else
                {
                    //block is custom
                    return switch (blockLib.getPistonPushable())
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

                    if(!(lib instanceof PlaceableLibrary blockLib))
                        continue;

                    if(switch (blockLib.getPistonPushable())
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
            if (ItemManager.getLibrary(block) instanceof BlockStateLibrary lib)
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
        if(lib instanceof InteractorLibrary)
        {
            ((InteractorLibrary) lib).naturalBlockBreak(e.getBlock(), true);
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

        if(lib instanceof InteractorLibrary interactor)
        {
            interactor.naturalBlockBreak(e.getBlock(), true);
        }
    }

    //endregion

}
