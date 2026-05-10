package arnett.customItemsAPI.Helpers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardHook {

    //uses the World Guard API so may cause errors if Worldguard is not loaded
    //region Can World Guard X

    /*=================================================================================================
                    -  Can World Guard X   -
    =================================================================================================*/

    /**
     * Checks whether World guard permits the player to interact in this spot (Uses Worldguard API)
     * @param player Player whose perms to check
     * @param spot Spot to check
     * @return Whether they can interact or not in that spot
     */
    public static boolean canWorldGuardInteract(Player player, Location spot)
    {
        return canWorldGuardFlag(player, spot, Flags.INTERACT);
    }

    /**
     * Checks whether World guard permits the player to place in this spot (Uses Worldguard API)
     * @param player Player whose perms to check
     * @param spot Spot to check
     * @return Whether they can place or not in that spot
     */
    public static boolean canWorldGuardPlace(Player player, Location spot)
    {
        return canWorldGuardFlag(player, spot, Flags.BLOCK_PLACE);
    }

    /**
     * Checks whether World guard permits the player to break in this spot (Uses Worldguard API)
     * @param player Player whose perms to check
     * @param spot Spot to check
     * @return Whether they can break or not in that spot
     */
    public static boolean canWorldGuardBreak(Player player, Location spot)
    {
        return canWorldGuardFlag(player, spot, Flags.BLOCK_BREAK);
    }

    /**
     * Checks whether World guard permits the player to do (FLAG) in this spot (Uses Worldguard API)
     * @param player Player whose perms to check
     * @param spot Spot to check
     * @param flag Flag (permission) to check
     * @returnWhether they can (FLAG) or not in that spot
     */
    public static boolean canWorldGuardFlag(Player player, Location spot, StateFlag flag)
    {
        if(player.isOp())
            return true;

        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        LocalPlayer worldGuardPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

        return query.queryValue(BukkitAdapter.adapt(spot), worldGuardPlayer, flag) != StateFlag.State.DENY;
    }

    //endregion

}
