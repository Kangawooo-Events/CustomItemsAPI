package arnett.customItemsAPI.Helpers;

import arnett.customItemsAPI.CustomItemsAPI;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class WorldGuardHelper {

    public static boolean canWorldGuardFlag(Player player, Location spot, StateFlag flag)
    {
        if(!CustomItemsAPI.worldGuardEnabled)
            return true;

        if(player.isOp())
            return true;

        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        LocalPlayer worldGuardPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

        return query.queryValue(BukkitAdapter.adapt(spot), worldGuardPlayer, flag) != StateFlag.State.DENY;
    }

    public static boolean canWorldGuardInteract(Player player, Location spot)
    {
        return canWorldGuardFlag(player, spot, Flags.INTERACT);
    }

    public static boolean canWorldGuardBuild(Player player, Location spot)
    {
        return canWorldGuardFlag(player, spot, Flags.BUILD);
    }
}
