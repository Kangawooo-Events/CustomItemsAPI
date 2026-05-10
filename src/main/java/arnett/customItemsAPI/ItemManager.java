package arnett.customItemsAPI;

import arnett.customItemsAPI.ItemLibraries.ItemLibrary;
import cd.arnett.caddamands.cattamands.arguments.Cattarameter;
import cd.arnett.caddamands.cattamands.cattamand.Cattamand;
import cd.arnett.caddamands.cattamands.cattamand.LiteralCattamand;
import com.jeff_media.customblockdata.CustomBlockData;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.persistence.PersistentDataContainerView;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Interaction;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class ItemManager {


    //region Variables

    /*=================================================================================================
                    -  Variables  -
    =================================================================================================*/

    static HashMap<NamespacedKey, ItemLibrary> items = new HashMap<>();
    static HashSet<Material> customItemMaterials = new HashSet<>();

    //endregion


    //region Getters

    /*=================================================================================================
                    -  Getters  -
    =================================================================================================*/

    /**
     * @return Gets a List of the names of Items which have been registered
     */
    public static List<String> getItemNames() {
        return items.values().stream().map(ItemLibrary::getName).toList();
    }

    /**
     * @return Gets the map of ItemLibraries indexed by their item's identifier Namespace
     */
    public static HashMap<NamespacedKey, ItemLibrary> getItems()
    {
        return items;
    }

    //endregion


    //region Registration Center

    /*=================================================================================================
                    -  Registration Center  -
    =================================================================================================*/

    /**
     * Registers an ItemLibrary as a custom item (for multiple see "registerItems")
     * @param plugin Plugin whose responsible for this item
     * @param item Item to register
     */
    public static void registerItem(JavaPlugin plugin, ItemLibrary item)
    {
        //fill items map
        addItems(List.of(item));

        //set any recipes
        registerRecipes(plugin, item);
        registerEvents(plugin, item);
    }

    /**
     * Registers List of ItemLibraries as custom items
     * @param plugin Plugin whose responsible for these items
     * @param items Items to register
     */
    public static void registerItems(JavaPlugin plugin, List<ItemLibrary> items)
    {
        //fill items map
        addItems(items);

        items.forEach(( item) -> {
            //set any recipes
            registerRecipes(plugin, item);
            registerEvents(plugin, item);
        });
    }

    /**
     * Adds a list of ItemLibraries to the registered custom Items
     * @param items List of Items to add
     */
    static void addItems(List<ItemLibrary> items)
    {
        //add all the items to the item map
        for(ItemLibrary item : items)
        {
            customItemMaterials.add(item.getBaseMaterial());
            ItemManager.items.put(item.getIdentifier(), item);
        }
    }

    //endregion


    //region Command Registration

    /*=================================================================================================
                    -  Command Registration  -
    =================================================================================================*/

    /**
     * Registers the give command provided by the ItemLibrary <br>
     * Called from OnEnable
     * @param event Registrar Event to register the commands on (provided by Lifecycle Manager)
     */
    static void registerGiveCommand(ReloadableRegistrarEvent<Commands> event)
    {
        ArrayList<Cattamand> giveCommands = new ArrayList<>();

        items.forEach(( item, lib) -> {
            giveCommands.add(lib.getGiveItemCommand());
        });

        //register the give commands of the items
        new LiteralCattamand.Builder("cigive")
                .argument(List.of(
                        Cattarameter.of(
                                "receiver",
                                ArgumentTypes.players()
                        )
                ))
                .children(
                        giveCommands
                )
                .aliases(List.of("cig", "cg", "cgive"))
                .permission("op")
                .build()
                .registerAsRoot(CustomItemsAPI.singleton, event);
    }

    //endregion


    //region Event Registration

    /*=================================================================================================
                    -  Event Registration  -
    =================================================================================================*/

    /**
     * Registers the items Listeners if present
     * @param plugin Plugin to give registration credit to
     * @param item ItemLibrary to register
     */
    static void registerEvents(JavaPlugin plugin, ItemLibrary item)
    {
        item.getListeners().forEach(listener -> {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        });
    }

    /**
     * Unregisters Listeners associated with an ItemLibrary
     * @param item ItemLibrary whose listeners to unregister
     */
    static void unregisterEvents(ItemLibrary item)
    {
        item.getListeners().forEach(listener -> {
            HandlerList.unregisterAll(listener);
        });
    }

    //endregion


    //region Recipe Registration

    /*=================================================================================================
                    -  Recipe Registration  -
    =================================================================================================*/

    /**
     * Unregisters Recipe related to ItemLibrary
     * @param item ItemLibrary to unregister Recipes from
     */
    static void unregisterRecipes(ItemLibrary item)
    {
        for(Recipe recipe : item.getRecipes())
        {
            if(recipe instanceof Keyed keyed)
            {
                if(Bukkit.getRecipe(keyed.getKey()) != null)
                    Bukkit.removeRecipe(keyed.getKey());
            }
        }
    }

    /**
     * Registers recipes related to ItemLibrary
     * @param plugin Plugin which the Recipe belongs to
     * @param item ItemLibrary whose recipes to register
     */
    static void registerRecipes(JavaPlugin plugin, ItemLibrary item)
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

    //endregion


    //region Library Getters

    /*=================================================================================================
                    -  Library Getters  -
    =================================================================================================*/


    /**
     * Gets an ItemLibrary Object which corresponds to a given Name
     * @param name Name to check
     * @return A new ItemLibrary insance, Null if none was found
     */
    public static ItemLibrary getItemFromName(String name) throws IllegalArgumentException {

        for(ItemLibrary data : items.values())
        {
            if(data.getName().equals(name))
                return data;
        }

        throw new IllegalArgumentException("Can't find item of name: " + name.toString());
    }


    /**
     * Gets an ItemLibrary Object which corresponds to a given NamespacedKey
     * @param key Item Stack to check
     * @return A new ItemLibrary insance, Null if none was found
     */
    public static ItemLibrary getLibraryFromNamespace(NamespacedKey key) throws IllegalArgumentException {
        if(items.containsKey(key))
            return items.get(key);

        throw new IllegalArgumentException("Can't find item of name: " + key.toString());
    }

    /**
     * Gets an ItemLibrary Object which corresponds to a given ItemStack
     * @param stack Item Stack to check
     * @return A new ItemLibrary insance, Null if none was found
     */
    public static ItemLibrary getLibrary(ItemStack stack)
    {
        if(stack == null)
            return null;

        //quick generic check before more expensive pdc
        if(!customItemMaterials.contains(stack.getType()))
            return null;

        PersistentDataContainerView pdc = stack.getPersistentDataContainer();

        return getLibrary(pdc);
    }


    /**
     * Gets an ItemLibrary Object which corresponds to a given PersistentDataContainerView
     * @param pdc Persistent Data Container to check
     * @return A new ItemLibrary insance, Null if none was found
     */
    public static ItemLibrary getLibrary(PersistentDataContainerView pdc)
    {
        //is this a custom item?
        if(!pdc.has(ItemLibrary.customItemTag))
            return null;

        NamespacedKey itemType = NamespacedKey.fromString(pdc.get(ItemLibrary.customItemTag, PersistentDataType.STRING));

        //was it a namespace (did someone screw up storing this correctly check)
        if(itemType == null)
            return null;

        //is this one in this manager?
        if(!items.containsKey(itemType))
            return null;

        //it is an item in this manager
        return items.get(itemType);
    }


    /**
     * Gets an ItemLibrary Object which corresponds to a given CustomBlockData
     * @param data Custom Block Data to check
     * @return A new ItemLibrary insance, Null if none was found
     */
    public static ItemLibrary getLibrary(CustomBlockData data)
    {
        //is this a custom item?
        if(!data.has(ItemLibrary.customItemTag))
            return null;

        NamespacedKey itemType = NamespacedKey.fromString(data.get(ItemLibrary.customItemTag, PersistentDataType.STRING));

        //was it a namespace (did someone screw up storing this correctly check)
        if(itemType == null)
            return null;

        //is this one in this manager?
        if(!items.containsKey(itemType))
            return null;

        //it is an item in this manager
        return items.get(itemType);
    }


    /**
     * Gets an ItemLibrary Object which corresponds to a given Interaction Entity
     * @param interaction Interaction entity to check
     * @return A new ItemLibrary insance, Null if none was found
     */
    public static ItemLibrary getLibrary(Interaction interaction)
    {
        //is this a custom item?
        if(!interaction.getPersistentDataContainer().has(ItemLibrary.customItemTag))
            return null;

        String ciTag = interaction.getPersistentDataContainer().get(ItemLibrary.customItemTag, PersistentDataType.STRING);

        if(ciTag == null)
            return null;

        NamespacedKey itemType = NamespacedKey.fromString(ciTag);

        //was it a namespace (did someone screw up storing this correctly check)
        if(itemType == null)
            return null;

        //is this one in the manager?
        if(!items.containsKey(itemType))
            return null;

        //it is an item in this manager
        return items.get(itemType);
    }


    /**
     * Gets an ItemLibrary Object which corresponds to a given block
     * @param block block to check
     * @return A new ItemLibrary insance, Null if none was found
     */
    public static ItemLibrary getLibrary(Block block)
    {
        if(block == null)
            return null;

        if(!CustomBlockData.hasCustomBlockData(block, CustomItemsAPI.singleton))
            return null;

        return getLibrary(new CustomBlockData(block, CustomItemsAPI.singleton));
    }

    //endregion

}
