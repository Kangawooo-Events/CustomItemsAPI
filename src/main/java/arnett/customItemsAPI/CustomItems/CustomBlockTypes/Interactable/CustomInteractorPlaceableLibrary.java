package arnett.customItemsAPI.CustomItems.CustomBlockTypes.Interactable;

import arnett.customItemsAPI.CustomItems.CustomBlockTypes.CustomPlaceableLibrary;
import org.bukkit.util.Vector;

public abstract class CustomInteractorPlaceableLibrary extends CustomPlaceableLibrary {
    public abstract Vector hitboxOffset();

    public Vector hitboxWallOffset()
    {
        return hitboxOffset();
    }
}
