package arnett.customItemsAPI.CustomItems.CustomBlockTypes.Interactable;

import arnett.customItemsAPI.CustomItems.CustomBlockTypes.CustomPlaceableData;
import org.bukkit.util.Vector;

public abstract class CustomInteractorPlaceableData extends CustomPlaceableData {
    public abstract Vector hitboxOffset();

    public Vector hitboxWallOffset()
    {
        return hitboxOffset();
    }
}
