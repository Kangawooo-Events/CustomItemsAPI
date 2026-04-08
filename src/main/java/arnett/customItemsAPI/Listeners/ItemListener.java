package arnett.customItemsAPI.Listeners;

import arnett.customItemsAPI.CustomItems.ItemLibrary;
import org.bukkit.event.Listener;

public abstract class ItemListener implements Listener {

    ItemLibrary data;

    public ItemListener(ItemLibrary data)
    {
        this.data = data;
    }

}
