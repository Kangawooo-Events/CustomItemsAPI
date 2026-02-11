package arnett.customItemsAPI;

import arnett.cattamands.Cattamand;
import arnett.cattamands.LiteralCattamand;
import arnett.customItemsAPI.CustomItems.CustomItemData;
import arnett.customItemsAPI.CustomItems.Useable.CustomUsableData;
import arnett.customItemsAPI.Listeners.GeneralItemListener;
import com.jeff_media.customblockdata.CustomBlockData;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CustomItemManager {

    JavaPlugin plugin;
    HashMap<NamespacedKey, CustomItemData> items = new HashMap<>();
    GeneralItemListener generalListener;

    public static NamespacedKey DisplayLinkNamespace = new NamespacedKey("customitems", "linkeddisplay");

    public CustomItemManager(JavaPlugin plugin, List<CustomItemData> items)
    {
        this.plugin = plugin;

        ArrayList<Cattamand> giveCommands = new ArrayList<>();

        //fill items map
        fillItemMap(items);

        items.forEach(( item) -> {
            //set any recipes
            registerRecipes(item);
            registerEvents(item);

            //get the list of give command arguments
            giveCommands.add(item.getGiveCommand());
        });

        //create one listener for the general events we need to listen to
        generalListener = new GeneralItemListener(this);

        //register general item listener
        plugin.getServer().getPluginManager().registerEvents(generalListener, plugin);

        //register the give commands of the items
        new LiteralCattamand.Builder("cigive")
                .children(giveCommands)
                .aliases(List.of("ci", "cg", "cgive"))
                .permission("op")
                .build()
                .registerAsRoot(plugin);
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
        return items.values().stream().map(CustomItemData::getName).toList();
    }

    public HashMap<NamespacedKey, CustomItemData> getItems()
    {
        return items;
    }

    public CustomItemData getItemFromName(String name) throws IllegalArgumentException {

        for(CustomItemData data : items.values())
        {
            if(data.getName().equals(name))
                return data;
        }

        throw new IllegalArgumentException("Can't find item of name: " + name.toString());
    }


    public CustomItemData getItemFromNamespace(NamespacedKey key) throws IllegalArgumentException {
        if(items.containsKey(key))
            return items.get(key);

        throw new IllegalArgumentException("Can't find item of name: " + key.toString());
    }

    //refresh call for when config changes have been made
    public void refresh()
    {
        fillItemMap(items.values().stream().toList());
    }

    //update map of items to match their keys
    public void fillItemMap(List<CustomItemData> items)
    {
        if(items != null)
            this.items.clear();

        for(CustomItemData item : items)
        {
            this.items.put(item.getIdentifier(), item);
        }
    }

    public CustomItemData getData(ItemStack stack)
    {
        if(stack == null)
            return null;

        PersistentDataContainerView pdc = stack.getPersistentDataContainer();

        return getData(pdc);
    }

    public CustomItemData getData(PersistentDataContainerView pdc)
    {
        //is this a custom item?
        if(!pdc.has(CustomItemData.customItemTag))
            return null;

        NamespacedKey itemType = NamespacedKey.fromString(pdc.get(CustomItemData.customItemTag, PersistentDataType.STRING));

        //was it a namespace (did someone screw up storing this correctly check)
        if(itemType == null)
            return null;

        //is this one in this manager?
        if(!items.containsKey(itemType))
            return null;

        //it is an item in this manager
        return items.get(itemType);
    }

    public CustomItemData getData(CustomBlockData data)
    {
        //is this a custom item?
        if(!data.has(CustomItemData.customItemTag))
            return null;

        NamespacedKey itemType = NamespacedKey.fromString(data.get(CustomItemData.customItemTag, PersistentDataType.STRING));

        //was it a namespace (did someone screw up storing this correctly check)
        if(itemType == null)
            return null;

        //is this one in this manager?
        if(!items.containsKey(itemType))
            return null;

        //it is an item in this manager
        return items.get(itemType);
    }


    public CustomItemData getData(Block block)
    {
        if(block == null)
            return null;

        if(!CustomBlockData.hasCustomBlockData(block, CustomItemsAPI.singleton))
            return null;

        return getData(new CustomBlockData(block, CustomItemsAPI.singleton));
    }
}
