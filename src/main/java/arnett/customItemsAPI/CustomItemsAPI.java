package arnett.customItemsAPI;

import arnett.customItemsAPI.Listeners.GeneralItemListener;
import arnett.customItemsAPI.items.modules.callbacks.ItemCallbackManager;
import arnett.customItemsAPI.items.data.annotations.ProvideDataEntry;
import arnett.customItemsAPI.items.data.annotations.ProvidePlacementDataEntry;
import arnett.customItemsAPI.items.modules.callbacks.annotations.ModifyNewItemMeta;
import com.jeff_media.customblockdata.CustomBlockData;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class CustomItemsAPI extends JavaPlugin {

    //region Properties

    /*=================================================================================================
                    -  Properties  -
    =================================================================================================*/

    public static Logger logger;
    public static JavaPlugin singleton;
    public static boolean worldGuardEnabled;

    //endregion


    //region Enable / Disable

    /*=================================================================================================
                    -  Enable / Disable  -
    =================================================================================================*/

    @Override
    public void onEnable() {
        //set logger for easy logging
        logger = getLogger();

        // Plugin startup logic
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            logger.info("Loading World Guard");
            worldGuardEnabled = true;
        }

        //register the general item listener
        getServer().getPluginManager().registerEvents(new GeneralItemListener(), this);

        //set singleton for easy reference to plugin
        singleton = this;

        //register the custom block data events
        CustomBlockData.registerListener(this);

        //Call registration a single time here so that other plugins have time to add their items
        getLifecycleManager().registerEventHandler(
                LifecycleEvents.COMMANDS.newHandler(ItemManager::registerGiveCommand).priority(6767)
        );

        ItemCallbackManager.trackAnnotation(ProvideDataEntry.class);
        ItemCallbackManager.trackAnnotation(ProvidePlacementDataEntry.class);
        ItemCallbackManager.trackAnnotation(ModifyNewItemMeta.class);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    //endregion

}
