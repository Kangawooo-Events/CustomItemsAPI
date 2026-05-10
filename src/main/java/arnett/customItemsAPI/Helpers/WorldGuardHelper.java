package arnett.customItemsAPI.Helpers;

import arnett.customItemsAPI.CustomItemsAPI;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardHelper {

    //Does not use Worldguard API so will not cause errors if Worldguard is Missing
    //region Can World Guard X

    /*=================================================================================================
                    -  Can World Guard X  -
    =================================================================================================*/

    /**
     * Checks whether World guard permits the player to interact in this spot (Does NOT use Worldguard API)
     * @param player Player whose perms to check
     * @param spot Spot to check
     * @return Whether they can interact or not in that spot
     */
    public static boolean canWorldGuardInteract(Player player, Location spot)
    {
        if(!CustomItemsAPI.worldGuardEnabled)
            return true;

        return WorldGuardHook.canWorldGuardInteract(player, spot);
    }

    /**
     * Checks whether World guard permits the player to place in this spot (Does NOT use Worldguard API)
     * @param player Player whose perms to check
     * @param spot Spot to check
     * @return Whether they can place or not in that spot
     */
    public static boolean canWorldGuardPlace(Player player, Location spot)
    {
        if(!CustomItemsAPI.worldGuardEnabled)
            return true;

        return WorldGuardHook.canWorldGuardPlace(player, spot);
    }

    /**
     * Checks whether World guard permits the player to break in this spot (Does NOT use Worldguard API)
     * @param player Player whose perms to check
     * @param spot Spot to check
     * @return Whether they can break or not in that spot
     */
    public static boolean canWorldGuardBreak(Player player, Location spot)
    {
        if(!CustomItemsAPI.worldGuardEnabled)
            return true;

        return WorldGuardHook.canWorldGuardBreak(player, spot);
    }

    //endregion

}
