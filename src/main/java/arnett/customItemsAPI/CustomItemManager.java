package arnett.customItemsAPI;

import arnett.cattamands.Cattamand;
import arnett.cattamands.CattamandArgument;
import arnett.cattamands.LiteralCattamand;
import arnett.customItemsAPI.CustomItems.CustomItemData;
import arnett.customItemsAPI.CustomItems.Useable.CustomUsableData;
import arnett.customItemsAPI.Listeners.GeneralItemListener;
import com.jeff_media.customblockdata.CustomBlockData;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public final class CustomItemManager {

    static HashMap<NamespacedKey, CustomItemData> items = new HashMap<>();
    static Set<Material> customItemMaterials = Set.of();

    public static NamespacedKey DisplayLinkNamespace = new NamespacedKey("customitems", "linkeddisplay");

    public static void registerItems(JavaPlugin plugin, List<CustomItemData> items)
    {
        ArrayList<Cattamand> giveCommands = new ArrayList<>();

        //fill items map
        addItems(items);

        items.forEach(( item) -> {
            //set any recipes
            registerRecipes(plugin, item);
            registerEvents(plugin, item);

            //get the list of give command arguments
            giveCommands.add(item.getGiveCommand());
        });

        //register the give commands of the items
        new LiteralCattamand.Builder("give")
                .args(List.of(
                        new CattamandArgument(
                                "receiver",
                                ArgumentTypes.players()
                        )
                ))
                .children(giveCommands)
                .aliases(List.of("cig", "cg", "cgive"))
                .permission("op")
                .build()
                .registerAsRoot(plugin);
    }

    static void registerEvents(JavaPlugin plugin, CustomItemData item)
    {
        item.getListeners().forEach(listener -> {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        });
    }

    //unused
    static void unregisterEvents(CustomItemData item)
    {
        item.getListeners().forEach(listener -> {
            HandlerList.unregisterAll(listener);
        });
    }

    static void registerRecipes(JavaPlugin plugin, CustomItemData item)
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

    static void unregisterRecipes(CustomItemData item)
    {
        for(NamespacedKey key : item.getRecipeKeys())
        {
            if(Bukkit.getRecipe(key) != null)
                Bukkit.removeRecipe(key);
        }
    }

    public static List<String> getItemNames() {
        return items.values().stream().map(CustomItemData::getName).toList();
    }

    public static HashMap<NamespacedKey, CustomItemData> getItems()
    {
        return items;
    }

    public static CustomItemData getItemFromName(String name) throws IllegalArgumentException {

        for(CustomItemData data : items.values())
        {
            if(data.getName().equals(name))
                return data;
        }

        throw new IllegalArgumentException("Can't find item of name: " + name.toString());
    }


    public static CustomItemData getItemFromNamespace(NamespacedKey key) throws IllegalArgumentException {
        if(items.containsKey(key))
            return items.get(key);

        throw new IllegalArgumentException("Can't find item of name: " + key.toString());
    }

    static void addItems(List<CustomItemData> items)
    {
        //add all the items to the item map
        for(CustomItemData item : items)
        {
            customItemMaterials.add(item.getBaseMaterial());
            CustomItemManager.items.put(item.getIdentifier(), item);
        }
    }

    public static CustomItemData getData(ItemStack stack)
    {
        if(stack == null)
            return null;

        //quick generic check before more expensive pdc
        if(!customItemMaterials.contains(stack.getType()))
            return null;

        PersistentDataContainerView pdc = stack.getPersistentDataContainer();

        return getData(pdc);
    }

    public static CustomItemData getData(PersistentDataContainerView pdc)
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

    public static CustomItemData getData(CustomBlockData data)
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


    public static CustomItemData getData(Block block)
    {
        if(block == null)
            return null;

        //quick check before more expensive pdc
        if(!customItemMaterials.contains(block.getType()))
            return null;

        if(!CustomBlockData.hasCustomBlockData(block, CustomItemsAPI.singleton))
            return null;

        return getData(new CustomBlockData(block, CustomItemsAPI.singleton));
    }
}
