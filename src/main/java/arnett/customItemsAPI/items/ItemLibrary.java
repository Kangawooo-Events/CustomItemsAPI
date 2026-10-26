package arnett.customItemsAPI.items;

import arnett.customItemsAPI.items.modules.callbacks.TrackedCallbacks;
import cd.arnett.caddamands.cattamands.arguments.ArgumentHelper;
import cd.arnett.caddamands.cattamands.arguments.Cattarameter;
import cd.arnett.caddamands.cattamands.cattamand.Cattamand;
import cd.arnett.caddamands.cattamands.cattamand.LiteralCattamand;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;

public abstract class ItemLibrary {

    //region Static Properties

    /*=================================================================================================
                    -  Static Properties  -
    =================================================================================================*/

    /**
     * Namespace used to identify something generally as a custom item
     */
    public static NamespacedKey customItemTag = new NamespacedKey("customitems", "customitem");

    //endregion


    //region Abstract Properties

    /*=================================================================================================
                    -  Abstract Properties  -
    =================================================================================================*/

    /**
     * @return The name of this item
     */
    public abstract String name();

    /**
     * @return Namespace used to identify the specific item
     */
    public abstract NamespacedKey identifier();

    /**
     * @return Namespaced Key which leads to the Item's resource in a resource pack
     */
    public abstract NamespacedKey itemModelKey();

    /**
     * @return The base material this item uses in inventory
     */
    public abstract Material baseMaterial();

    //endregion


    //region Optional Properties

    /*=================================================================================================
                    -  Optional Properties  -
    =================================================================================================*/

    /**
     * @return List of Listeners to be registered along with this item,
     * useful for unloading whenever that gets implemented, but I wouldn't
     * worry about using this it's not a big deal
     */
    public List<Listener> listeners()
    {
        return List.of();
    }

    /**
     * @return The Display name of this item (defaults to getName())
     */
    public String displayName()
    {
        return name();
    }

    /**
     * @return The Give cattamand provided by the itemLibrary to be applied as a child of <br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;/cgive &ltPlayer(s)&gt ... <br>
     */
    public Cattamand giveItemCommand()
    {
        return new LiteralCattamand(
                name(),
                "op",
                List.of(
                        new Cattarameter(
                                "count",
                                IntegerArgumentType.integer(
                                        0,
                                        baseMaterial().getMaxStackSize()
                                ),
                                null,
                                (ctx) -> {
                                    int count = ctx.getArgument("count", int.class);

                                    //get the player(s) to give to
                                    ArgumentHelper.getPlayersFromArgs("receiver", ctx).forEach(player -> {
                                        player.give(getItem(count));
                                    });

                                    //successful execution
                                    return Command.SINGLE_SUCCESS;
                                }
                        )
                ),
                context -> {

                    //get the player(s) to give to
                    ArgumentHelper.getPlayersFromArgs("receiver", context).forEach(player -> {
                        player.give(getItem());
                    });

                    //successful execution
                    return Command.SINGLE_SUCCESS;
                }
        ).setAliases(List.of(identifier().toString()));
    }

    /**
     * @return Whether to allow this item to be used in crafting as it's material base
     * i.e. Using dynamite as paper in crafting a book
     */
    public boolean keepBaseCrafts()
    {
        return false;
    }

    /**
     * @return Whether this item should continue its interaction behavior when it would be canceled by WorldGuard.
     * this DOES NOT override worldguard, it only overrides the check CustomItemsAPI does before running
     * its interaction code.
     */
    public boolean overrideWorldGuardInteract()
    {
        return false;
    }

    /**
     * Determines whether costly inventory movement related events like
     * PlayerInventorySlotChangeEvent are tracked for this item to save on performance
     * @return Override to TRUE to track these events for this item
     */
    public boolean trackInventoryMovements()
    {
        return false;
    }
    //endregion


    //region Get Item

    /*=================================================================================================
                    -  Get Item  -
    =================================================================================================*/

    /**
     * @return A new ItemStack of this Libraries item
     */
    public ItemStack getItem() {

        //create item stack of config set material
        ItemStack item = ItemStack.of(baseMaterial());

        item.setItemMeta(getModifiedItemMeta(item));

        return item;
    }

    /**
     * @param count amount to set itemstack to
     * @return Itemstack of this Item with (count) amount
     */
    public ItemStack getItem(int count)
    {
        ItemStack stack = getItem();
        stack.setAmount(count);
        return stack;
    }


    /**
     * Modifies the item meta of the item passed, usually used just to tag it and set its model but is also
     * useful to override when adding tags so that a second item meta isn't created (since that is slower).
     * This DOES NOT set the item meta back on the ItemStack
     * @param item The base item to get the itemMeta from
     * @return the modified Item Meta
     */
    public ItemMeta getModifiedItemMeta(ItemStack item)
    {
        //change the item model
        ItemMeta meta = item.getItemMeta();

        meta.setItemModel(itemModelKey());

        meta.getPersistentDataContainer().set(identifier(), PersistentDataType.BOOLEAN, true);

        //tag it as a custom item with a namespace which can be easily accessed later to get the specific item type
        meta.getPersistentDataContainer().set(customItemTag, PersistentDataType.STRING, identifier().toString());

        meta.itemName(MiniMessage.miniMessage().deserialize(displayName()));

        return meta;
    }

    //endregion


    //region Recipes

    /*=================================================================================================
                    -  Recipes  -
    =================================================================================================*/

    /**
     * @return List of recipes to craft this item
     */
    public List<Recipe> getRecipes() {
        return List.of();
    }

    //endregion


    //region Identification

    /*=================================================================================================
                    -  Identification  -
    =================================================================================================*/

    /**
     * Checks whether a given ItemStack is of this ItemLibrary
     * @param stack ItemStack to check
     * @return
     */
    public boolean isItem(ItemStack stack)
    {
        if(stack == null)
            return false;

        //start with general material check
        if(!stack.getType().equals(baseMaterial()))
            return false;

        return stack.getPersistentDataContainer().has(identifier());
    }

    /**
     * Gets the item slot in which this item is found in the given Inventory
     * @param inventory Inventory to search
     * @return item slot number (-1 if not found)
     */
    public int findInInventory(Inventory inventory)
    {
        var itemSlots = inventory.all(baseMaterial());

        //not in this inventory
        if(itemSlots.isEmpty())
            return -1;

        for(int slot : itemSlots.keySet())
        {
            //base material is here so now check if it has the identifier
            if(isItem(inventory.getItem(slot)))
                return slot;
        }

        //not the custom item (just shares the base material)
        return -1;
    }

    /**
     * Gets all the item slots in which this item is found in the given Inventory
     * @param inventory Inventory to search
     * @return List of slot numbers with this item
     */
    public List<Integer> findAllInInventory(Inventory inventory)
    {
        ArrayList<Integer> containingSlots = new ArrayList<>();

        for(int i = 0; i < inventory.getSize(); i++)
        {
            if(inventory.getItem(i) == null || inventory.getItem(i).getType() != baseMaterial())
                continue;
            if(isItem(inventory.getItem(i)))
                containingSlots.add(i);

        }

        return containingSlots;
    }

    //endregion


    //region Events

    /*=================================================================================================
                    -  Events  -
    =================================================================================================*/

    /**
     * Called whenever the Item is used (Right/Left click with item in hand)
     */
    public void onItemUsed(PlayerInteractEvent e)
    {
        return;
    }

    /**
     * Called whenever the Item is used (Right click with item in hand)
     */
    public void onItemUsedOnEntity(PlayerInteractEntityEvent e)
    {
        return;
    }


    /**
     * Called whenever the Item is used to attack another entity
     */
    public void onItemUsedInAttack(EntityDamageByEntityEvent e, ItemStack item) {return;}

    /**
     * Called whenever the Item is picked up by a player specifically (also calls onEntityPickupItem)
     */
    public void onPlayerPickupItem(EntityPickupItemEvent e, Player player){return;}

    /**
     * Called whenever the Item is picked up by a player
     */
    public void onEntityPickupItem(EntityPickupItemEvent e){return;}

    /**
     * Called whenever the Item is dropped by a player
     */
    public void onPlayerDropItem(PlayerDropItemEvent e){return;}

    /**
     * Called whenever the Item is added to a player's inventory slot (including movement between them).
     * To be used you MUST OVERRIDE trackInventoryMovements() and return TRUE,
     * It is like this to save on performance since this is a very common event
     */
    public void onItemEnterPlayerInventorySlot(PlayerInventorySlotChangeEvent e){return;}

    /**
     * Called whenever the Item is removed from a player's inventory slot (including movement between them).
     * To be used you MUST OVERRIDE trackInventoryMovements() and return TRUE,
     * It is like this to save on performance since this is a very common event
     */
    public void onItemExitPlayerInventorySlot(PlayerInventorySlotChangeEvent e){return;}



    //endregion

    //idk where to put this so it's just left outside :/
    @Override
    public String toString()
    {
        return name();
    }


}
