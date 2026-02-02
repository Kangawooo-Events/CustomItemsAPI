package arnett.customItemsAPI;

import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class CustomItemsAPI extends JavaPlugin {

    public static Logger logger;
    public static JavaPlugin singleton;

    @Override
    public void onEnable() {
        // Plugin startup logic

        //set singleton for easy reference to plugin
        singleton = this;

        //register the custom block data events
        CustomBlockData.registerListener(this);

        //set logger for easy logging
        logger = getLogger();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
