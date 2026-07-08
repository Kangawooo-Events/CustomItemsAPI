package arnett.customItemsAPI;

import arnett.customItemsAPI.Listeners.GeneralItemListener;
import cd.arnett.caddamands.cattamands.arguments.Cattarameter;
import cd.arnett.caddamands.cattamands.cattamand.LiteralCattamand;
import cd.arnett.caddamands.cattamands.interpretation.Catterpreter;
import com.jeff_media.customblockdata.CustomBlockData;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
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
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    //endregion

}
