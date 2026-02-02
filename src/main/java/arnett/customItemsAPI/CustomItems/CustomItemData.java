package arnett.customItemsAPI.CustomItems;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public abstract class CustomItemData {

    protected JavaPlugin plugin;

    public abstract NamespacedKey getItemModelKey();

    public abstract NamespacedKey getItemIdentifierKey();

    protected abstract List<Listener> getListeners();

    public abstract Material getBaseMaterial();

    public abstract String getName();

    public ItemStack getItem() {
        //create item stack of config set material
        ItemStack safe = ItemStack.of(getBaseMaterial());

        //change the item model
        ItemMeta meta = safe.getItemMeta();

        meta.setItemModel(getItemModelKey());

        meta.getPersistentDataContainer().set(getItemIdentifierKey(), PersistentDataType.BOOLEAN, true);

        safe.setItemMeta(meta);

        return safe;
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
        //start with general material check
        if(!stack.getType().equals(getBaseMaterial()))
            return false;

        return stack.getPersistentDataContainer().has(getItemIdentifierKey());
    }
}
