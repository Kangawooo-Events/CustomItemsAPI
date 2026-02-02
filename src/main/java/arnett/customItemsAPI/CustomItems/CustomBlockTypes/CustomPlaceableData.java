package arnett.customItemsAPI.CustomItems.CustomBlockTypes;

import arnett.customItemsAPI.CustomItems.CustomItemData;
import arnett.customItemsAPI.CustomItems.Directionality;
import arnett.customItemsAPI.CustomItemsAPI;
import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

public abstract class CustomPlaceableData extends CustomItemData {

    public abstract NamespacedKey getDisplayModelKey();

    public List<NamespacedKey> getAttachmentModelKeys()
    {
        return List.of();
    }

    public Vector getDisplayModelOffset() {
        return new Vector(.5, .5, .5);
    }

    public abstract Directionality getDirectionality();

    public abstract Material getBreakParticleMaterial();

    public abstract Sound getBreakSound();

    protected PlaceableReceiver placeableReceiver;

    public final PlaceableReceiver getPlaceableReceiver()
    {
        if(placeableReceiver != null)
            return placeableReceiver;

        placeableReceiver = createPlaceableReceiver();
        return placeableReceiver;
    }

    protected abstract PlaceableReceiver createPlaceableReceiver();

    public abstract UUID createDisplay(Location spot, double rollRot);

    public abstract UUID createDisplay(Location spot, BlockPlaceEvent e, Directionality directionality);
}
