package arnett.customItemsAPI.CustomItems;

import cd.arnett.cattamands.arguments.ArgumentHelper;
import cd.arnett.cattamands.arguments.Cattarameter;
import cd.arnett.cattamands.cattamand.Cattamand;
import cd.arnett.cattamands.cattamand.LiteralCattamand;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    protected JavaPlugin plugin;

    public abstract NamespacedKey getIdentifier();

    public static NamespacedKey customItemTag = new NamespacedKey("customitems", "customitem");

    public abstract NamespacedKey getItemModelKey();

    public List<Listener> getListeners()
    {
        return List.of();
    }

    public abstract Material getBaseMaterial();

    public abstract String getName();

    public String getDisplayName()
    {
        return getName();
    }

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

    public Cattamand getGiveCommand()
    {
        return new LiteralCattamand(
                getName(),
                "op",
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

}
