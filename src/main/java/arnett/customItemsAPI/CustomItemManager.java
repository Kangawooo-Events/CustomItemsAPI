package arnett.customItemsAPI;

import arnett.customItemsAPI.CustomItems.CustomItemLibrary;
import cd.arnett.cattamands.arguments.Cattarameter;
import cd.arnett.cattamands.cattamand.Cattamand;
import cd.arnett.cattamands.cattamand.LiteralCattamand;
import com.jeff_media.customblockdata.CustomBlockData;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class CustomItemManager {

    static HashMap<NamespacedKey, CustomItemLibrary> items = new HashMap<>();
    static HashSet<Material> customItemMaterials = new HashSet<Material>();

    public static NamespacedKey DisplayLinkNamespace = new NamespacedKey("customitems", "linkeddisplay");

    public static void registerItems(JavaPlugin plugin, List<CustomItemLibrary> items)
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
        new LiteralCattamand.Builder("cigive")
                .argument(List.of(
                        new Cattarameter(
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

    static void registerEvents(JavaPlugin plugin, CustomItemLibrary item)
    {
        item.getListeners().forEach(listener -> {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        });
    }

    //unused
    static void unregisterEvents(CustomItemLibrary item)
    {
        item.getListeners().forEach(listener -> {
            HandlerList.unregisterAll(listener);
        });
    }

    static void registerRecipes(JavaPlugin plugin, CustomItemLibrary item)
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

    static void unregisterRecipes(CustomItemLibrary item)
    {
        for(NamespacedKey key : item.getRecipeKeys())
        {
            if(Bukkit.getRecipe(key) != null)
                Bukkit.removeRecipe(key);
        }
    }

    public static List<String> getItemNames() {
        return items.values().stream().map(CustomItemLibrary::getName).toList();
    }

    public static HashMap<NamespacedKey, CustomItemLibrary> getItems()
    {
        return items;
    }

    public static CustomItemLibrary getItemFromName(String name) throws IllegalArgumentException {

        for(CustomItemLibrary data : items.values())
        {
            if(data.getName().equals(name))
                return data;
        }

        throw new IllegalArgumentException("Can't find item of name: " + name.toString());
    }

    public static <T extends CustomItemLibrary> T getFromClass(Class<T> lib)
    {
        try
        {
            var instance = lib.getDeclaredConstructor().newInstance();
            return (T)items.get(instance.getIdentifier());
        }
        catch (Exception e)
        {
            CustomItemsAPI.logger.warning("Unable to Find Library for Class: " + lib.getName());
            return null;
        }
    }

    public static CustomItemLibrary getItemFromNamespace(NamespacedKey key) throws IllegalArgumentException {
        if(items.containsKey(key))
            return items.get(key);

        throw new IllegalArgumentException("Can't find item of name: " + key.toString());
    }

    static void addItems(List<CustomItemLibrary> items)
    {
        //add all the items to the item map
        for(CustomItemLibrary item : items)
        {
            customItemMaterials.add(item.getBaseMaterial());
            CustomItemManager.items.put(item.getIdentifier(), item);
        }
    }

    public static CustomItemLibrary getLibrary(ItemStack stack)
    {
        if(stack == null)
            return null;

        //quick generic check before more expensive pdc
        if(!customItemMaterials.contains(stack.getType()))
            return null;

        PersistentDataContainerView pdc = stack.getPersistentDataContainer();

        return getLibrary(pdc);
    }

    public static CustomItemLibrary getLibrary(PersistentDataContainerView pdc)
    {
        //is this a custom item?
        if(!pdc.has(CustomItemLibrary.customItemTag))
            return null;

        NamespacedKey itemType = NamespacedKey.fromString(pdc.get(CustomItemLibrary.customItemTag, PersistentDataType.STRING));

        //was it a namespace (did someone screw up storing this correctly check)
        if(itemType == null)
            return null;

        //is this one in this manager?
        if(!items.containsKey(itemType))
            return null;

        //it is an item in this manager
        return items.get(itemType);
    }

    public static CustomItemLibrary getLibrary(CustomBlockData data)
    {
        //is this a custom item?
        if(!data.has(CustomItemLibrary.customItemTag))
            return null;

        NamespacedKey itemType = NamespacedKey.fromString(data.get(CustomItemLibrary.customItemTag, PersistentDataType.STRING));

        //was it a namespace (did someone screw up storing this correctly check)
        if(itemType == null)
            return null;

        //is this one in this manager?
        if(!items.containsKey(itemType))
            return null;

        //it is an item in this manager
        return items.get(itemType);
    }


    public static CustomItemLibrary getLibrary(Block block)
    {
        if(block == null)
            return null;

        //quick check before more expensive pdc
        if(!customItemMaterials.contains(block.getType()))
            return null;

        if(!CustomBlockData.hasCustomBlockData(block, CustomItemsAPI.singleton))
            return null;

        return getLibrary(new CustomBlockData(block, CustomItemsAPI.singleton));
    }
}
