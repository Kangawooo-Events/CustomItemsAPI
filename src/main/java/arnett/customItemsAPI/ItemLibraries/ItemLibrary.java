package arnett.customItemsAPI.ItemLibraries;

import cd.arnett.caddamands.cattamands.arguments.ArgumentHelper;
import cd.arnett.caddamands.cattamands.arguments.Cattarameter;
import cd.arnett.caddamands.cattamands.cattamand.Cattamand;
import cd.arnett.caddamands.cattamands.cattamand.LiteralCattamand;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

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
    public abstract String getName();
    /**
     * @return Namespace used to identify the specific item
     */
    public abstract NamespacedKey getIdentifier();

    /**
     * @return Namespaced Key which leads to the Item's resource in a resource pack
     */
    public abstract NamespacedKey getItemModelKey();

    /**
     * @return The base material this item uses in inventory
     */
    public abstract Material getBaseMaterial();

    //endregion


    //region Optional Properties

    /*=================================================================================================
                    -  Optional Properties  -
    =================================================================================================*/

    /**
     * @return List of Listeners to be registered along with this item,
     * useful for unloading whenever that gets implemented, but I wouldn't
     * worry about using this it's not a bih deal
     */
    public List<Listener> getListeners()
    {
        return List.of();
    }

    /**
     * @return The Display name of this item (defaults to getName())
     */
    public String getDisplayName()
    {
        return getName();
    }

    /**
     * @return The Give cattamand provided by the itemLibrary to be applied as a child of <br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;/cgive &ltPlayer(s)&gt ... <br>
     */
    public Cattamand getGiveItemCommand()
    {
        return new LiteralCattamand(
                getName(),
                "op",
                List.of(
                        Cattarameter.of(
                                "count",
                                IntegerArgumentType.integer(
                                        0,
                                        getBaseMaterial().getMaxStackSize()
                                ),
                                (Command<CommandSourceStack>) (ctx) -> {
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
        ).setAliases(List.of(getIdentifier().toString()));
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
     * @return Whether this item should continue its interaction behaviour when it would be canceled by WorldGuard.
     * this DOES NOT override worldguard, it only overrides the check CustomItemsAPI does before running
     * its interaction code.
     */
    public boolean overrideWorldGuardInteract()
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
        ItemStack item = ItemStack.of(getBaseMaterial());

        //change the item model
        ItemMeta meta = item.getItemMeta();

        meta.setItemModel(getItemModelKey());

        meta.getPersistentDataContainer().set(getIdentifier(), PersistentDataType.BOOLEAN, true);

        //tag it as a custom item with a namespace which can be easily accessed later to get the specific item type
        meta.getPersistentDataContainer().set(customItemTag, PersistentDataType.STRING, getIdentifier().toString());

        meta.itemName(MiniMessage.miniMessage().deserialize(getDisplayName()));

        item.setItemMeta(meta);

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
        if(!stack.getType().equals(getBaseMaterial()))
            return false;

        return stack.getPersistentDataContainer().has(getIdentifier());
    }

    /**
     * Gets the item slot in which this item is found in the given Inventory
     * @param inventory Inventory to search
     * @return item slot number (-1 if not found)
     */
    public int findInInventory(Inventory inventory)
    {
        var itemSlots = inventory.all(getBaseMaterial());

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
            if(inventory.getItem(i) == null || inventory.getItem(i).getType() != getBaseMaterial())
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
     * @param e PlayerInteractEvent for this interaction
     */
    public void onItemUsed(PlayerInteractEvent e)
    {
        return;
    }

    /**
     * Called whenever the Item is used (Right click with item in hand)
     * @param e PlayerInteractEntityEvent for this interaction
     */
    public void onItemUsedOnEntity(PlayerInteractEntityEvent e)
    {
        return;
    }

    //endregion

}
