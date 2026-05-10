package arnett.customItemsAPI.ItemLibraries.CustomBlockTypes;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.joml.Quaternionf;

public record PlacementData
        (
                Location location,
                Quaternionf rotation,
                BlockFace faceOn
        )
{ }
