package arnett.customItemsAPI;

import arnett.customItemsAPI.Listeners.GeneralItemListener;
import cd.arnett.cattamands.arguments.Cattarameter;
import cd.arnett.cattamands.cattamand.LiteralCattamand;
import com.jeff_media.customblockdata.CustomBlockData;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Logger;

public final class CustomItemsAPI extends JavaPlugin {

    public static Logger logger;
    public static JavaPlugin singleton;

    @Override
    public void onEnable() {
        // Plugin startup logic

        //register the general item listener
        getServer().getPluginManager().registerEvents(new GeneralItemListener(), this);

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
