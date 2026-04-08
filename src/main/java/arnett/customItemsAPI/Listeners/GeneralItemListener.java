package arnett.customItemsAPI.Listeners;

import arnett.customItemsAPI.CustomItems.CustomBlockTypes.Interactable.InteractorLibrary;
import arnett.customItemsAPI.ItemManager;
import arnett.customItemsAPI.CustomItems.CustomBlockTypes.BlockState.BlockStateLibrary;
import arnett.customItemsAPI.CustomItems.CustomBlockTypes.PlaceableLibrary;
import arnett.customItemsAPI.CustomItems.ItemLibrary;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.List;

public class GeneralItemListener implements Listener {


    // ================ PLACEMENT ============================


    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e)
    {
        if(e.isCancelled())
            return;

        ItemLibrary data = ItemManager.getLibrary(e.getItemInHand());

        //check if this is taken by a custom block
        if(ItemManager.getLibrary(e.getBlock()) != null)
            return;

        //is this a custom item
        if(data == null)
            return;

        if(data instanceof BlockStateLibrary blockData)
            blockData.onBlockPlaced(e);
    }



    // ================ INTERACTIONS ============================



    @EventHandler
    public void onInteract(PlayerInteractEvent e)
    {
        if(e.useInteractedBlock() != Event.Result.DENY)
            //block clicked
            blockInteractCheck(e);

        if(e.useItemInHand() != Event.Result.DENY)
            //item used
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

        //call its use function
        data.onItemUsedOnEntity(e);
    }

    void blockInteractCheck(PlayerInteractEvent e)
    {
        ItemLibrary data = ItemManager.getLibrary(e.getClickedBlock());

        //is this a custom item
        if(data == null)
            return;

        //they are interacting with a placeable block
        if(data instanceof BlockStateLibrary blockStateData)
        {
            //call item's interact function
            blockStateData.onBlockInteracted(e);
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

        //is this something which should be placed?
        if(e.getClickedBlock() == null)
            return;

        //if the block's interact function doesn't prevent placing then continue
        if(e.useItemInHand() == Event.Result.DENY || e.getAction().isLeftClick())
            return;

        ItemLibrary handItemLib = ItemManager.getLibrary(e.getItem());

        //possibly they are trying to place an interactable item since this doesn't need to be a placeable block state
        if(handItemLib instanceof InteractorLibrary interactorData)
        {
            //call item's place function
            interactorData.onItemPlacementInteraction(e);
        }
    }

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


    // =================== BREAKAGE ==============================


    @EventHandler
    public void onBlockRemovedByPlayer(BlockBreakEvent e)
    {
        if(e.isCancelled())
            return;

        //get custom data (getLibrary has a quick exit)
        ItemLibrary data = ItemManager.getLibrary(e.getBlock());

        //is this a custom item
        if(data == null)
            return;

        if(data instanceof BlockStateLibrary blockStateData)
            //call item's break function
            blockStateData.onBlockBroken(e);

    }

    //when an entity explodes
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e)
    {
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

        interactorLibrary.onInteractorInteracted(e, interaction);
    }

    // =================== PISTONS =====================================

    @EventHandler
    public void onPistonPush(BlockPistonExtendEvent e)
    {
        Block tip;

        //get last pushed block
        if(e.getBlocks().isEmpty())
        {
            tip = e.getBlock().getRelative(e.getDirection());
        }
        else {
            tip = e.getBlocks().getLast().getRelative(e.getDirection());

            //check the blocks being pushed if they are a custom block
            //unrelated to the tip
            if(e.getBlocks().stream().anyMatch(block -> {
                ItemLibrary lib = ItemManager.getLibrary(block);

                if(lib == null)
                    return false;

                if(!(lib instanceof BlockStateLibrary blockLib))
                    return false;

                return switch (blockLib.getPistonPushable())
                {
                    case BREAK -> {
                        blockLib.naturalBlockBreak(block, true);
                        yield false;
                    }
                    case BLOCK, MOVE -> true;
                    default -> true;
                };
            }))
            {
                e.setCancelled(true);
                return;
            }
        }

        //check the tip block if it is pushing into an interactor then break it
        if(ItemManager.getLibrary(tip) instanceof InteractorLibrary library)
        {
            library.naturalBlockBreak(tip, true);
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
}
