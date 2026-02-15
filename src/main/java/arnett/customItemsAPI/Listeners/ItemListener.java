package arnett.customItemsAPI.Listeners;

import arnett.customItemsAPI.CustomItems.CustomItemLibrary;
import org.bukkit.event.Listener;

public abstract class ItemListener implements Listener {

    CustomItemLibrary data;

    public ItemListener(CustomItemLibrary data)
    {
        this.data = data;
    }

}
