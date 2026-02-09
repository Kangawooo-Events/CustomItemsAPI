package arnett.customItemsAPI.Listeners;

import arnett.customItemsAPI.CustomItems.CustomItemData;
import org.bukkit.event.Listener;

public abstract class ItemListener implements Listener {

    CustomItemData data;

    public ItemListener(CustomItemData data)
    {
        this.data = data;
    }

}
