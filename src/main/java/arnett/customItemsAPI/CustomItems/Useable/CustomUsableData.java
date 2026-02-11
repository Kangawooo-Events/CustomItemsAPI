package arnett.customItemsAPI.CustomItems.Useable;

import arnett.customItemsAPI.CustomItems.CustomItemData;
import org.bukkit.event.player.PlayerInteractEvent;

public abstract class CustomUsableData extends CustomItemData {
    public abstract void onItemUsed(PlayerInteractEvent e);
}
