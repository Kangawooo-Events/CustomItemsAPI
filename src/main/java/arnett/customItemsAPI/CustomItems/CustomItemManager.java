package arnett.customItemsAPI.CustomItems;

import arnett.customItemsAPI.CustomItems.Listener.CustomItemListener;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class CustomItemManager {

    JavaPlugin plugin;
    List<CustomItemData> items;
    CustomItemListener generalListener;

    public static NamespacedKey DisplayLinkNamespace = new NamespacedKey("customitems", "linkeddisplay");

    public CustomItemManager(JavaPlugin plugin, List<CustomItemData> items)
    {
        this.plugin = plugin;
        this.items = items;
        items.forEach(item -> {
            //set any recipes
            registerRecipes(item);
            registerEvents(item);
        });

        //create one listener for the general events we need to listen to
        generalListener = new CustomItemListener(this);

        //register general item listener
        plugin.getServer().getPluginManager().registerEvents(generalListener, plugin);
    }

    public void registerEvents(CustomItemData item)
    {
        item.getListeners().forEach(listener -> {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        });
    }

    //unused
    public void unregisterEvents(CustomItemData item)
    {
        item.getListeners().forEach(listener -> {
            HandlerList.unregisterAll(listener);
        });
    }

    public void registerRecipes(CustomItemData item)
    {
        for(Recipe r : item.getRecipes())
        {
            try
            {
                if(r instanceof Keyed keyedRecipe)
                    plugin.getLogger().info("Added Recipe: " + keyedRecipe.getKey());
                else
                    plugin.getLogger().info("Added Recipe for: " );
                Bukkit.addRecipe(r);
            }
            catch (Exception ignored)
            {

            }
        }
    }
    public void unregisterRecipes(CustomItemData item)
    {
        for(NamespacedKey key : item.getRecipeKeys())
        {
            if(Bukkit.getRecipe(key) != null)
                Bukkit.removeRecipe(key);
        }
    }

    public void reloadRecipe(CustomItemData data)
    {
        //refresh the recipes
        unregisterRecipes(data);
        registerRecipes(data);
    }

    public List<String> getItemNames() {
        return items.stream().map(CustomItemData::getName).toList();
    }

    public List<CustomItemData> getItems()
    {
        return items;
    }

    public CustomItemData getItemFromName(String name) throws IllegalArgumentException {
        for(CustomItemData item : items)
        {
            if(item.getName().equals(name))
                return item;
        }

        throw new IllegalArgumentException("Can't find item of name: " + name);
    }
}
