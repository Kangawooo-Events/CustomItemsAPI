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

    /**
     * Plugin to whom this item belongs
     */
    protected JavaPlugin plugin;

    /**
     * @return Namespace used to identify the specific item
     */
    public abstract NamespacedKey getIdentifier();

    /**
     * Namespace used to identify something generally as a custom item
     */
    public static NamespacedKey customItemTag = new NamespacedKey("customitems", "customitem");

    /**
     * @return Namespaced Key which leads to the Item's resource in a resource pack
     */
    public abstract NamespacedKey getItemModelKey();

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
     * @return The base material this item uses in inventory
     */
    public abstract Material getBaseMaterial();

    /**
     * @return The name of this item
     */
    public abstract String getName();

    /**
     * @return The Display name of this item (defaults to getName())
     */
    public String getDisplayName()
    {
        return getName();
    }

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

    public List<Recipe> getRecipes() {
        return List.of();
    }

    public List<NamespacedKey> getRecipeKeys(){
        return List.of();
    }

    public void setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isItem(ItemStack stack)
    {
        if(stack == null)
            return false;

        //start with general material check
        if(!stack.getType().equals(getBaseMaterial()))
            return false;

        return stack.getPersistentDataContainer().has(getIdentifier());
    }

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

    public void onItemUsed(PlayerInteractEvent e)
    {
        return;
    }

    public void onItemUsedOnEntity(PlayerInteractEntityEvent e)
    {
        return;
    }

    public boolean keepBaseCrafts()
    {
        return false;
    }

    public boolean overrideWorldGuardInteract()
    {
        return false;
    }
}
