package arnett.customItemsAPI.items.modules;

import arnett.customItemsAPI.items.ItemLibrary;
import org.bukkit.Material;

public interface ItemModule {

    default ItemLibrary getLibrary()
    {
        return (ItemLibrary) this;
    }

}
