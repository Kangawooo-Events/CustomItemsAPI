package arnett.customItemsAPI.CustomItems.Useable;

import arnett.customItemsAPI.CustomItems.CustomItemLibrary;
import org.bukkit.event.player.PlayerInteractEvent;

public abstract class CustomUsableLibrary extends CustomItemLibrary {
    public void onItemUsed(PlayerInteractEvent e)
    {
        return;
    }
}
