package arnett.customItemsAPI.CustomItems.CustomBlockTypes;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;

public class PlacementHelper {

    /**
     * Gets the placement location and rotation from the placement information passed
     * @param player The player who placed the block
     * @param against the face the block was placed against
     * @return
     */
    static Quaternionf placeNESW(Player player, BlockFace against)
    {
        return switch (against)
        {
            //base it off wall placement
            case NORTH -> new Quaternionf().rotateY(0);
            case EAST -> new Quaternionf().rotateY(-(float)Math.PI/2);
            case SOUTH -> new Quaternionf().rotateY((float)Math.PI);
            case WEST -> new Quaternionf().rotateY((float)Math.PI/2);

            //placed on floor or roof
            //base it off player direction
            default -> switch (player.getFacing())
            {
                //opposite here since the player is facing opposite to the face they're placing on
                case NORTH -> new Quaternionf().rotateY((float)Math.PI);
                case EAST -> new Quaternionf().rotateY((float)Math.PI/2);
                case WEST -> new Quaternionf().rotateY(-(float)Math.PI/2);
                default -> new Quaternionf().rotateY(0);
            };
        };
    }

    /**
     * Gets the placement location and rotation from the placement information passed
     * @param player The player who placed the block
     * @param against the face the block was placed against
     * @return
     */
    static Quaternionf placeUD(Player player, BlockFace against)
    {
        return switch (against) {
            case UP -> new Quaternionf();
            case DOWN -> new Quaternionf().rotateZ((float)Math.PI);

            //placed on a wall so base of placed direction
            default -> player.getFacing() == BlockFace.UP ?
                    new Quaternionf().rotateZ((float)Math.PI) :
                    new Quaternionf();
        };
    }

    static Pair<Quaternionf, BlockFace> placeNESWBlock(Location newBlockSpot, Player player, BlockFace against)
    {
        BlockFace movedFace = against;
        Quaternionf rotation = switch (against)
        {
            //base it off wall placement
            case NORTH -> new Quaternionf().rotateY(0);
            case EAST -> new Quaternionf().rotateY(-(float)Math.PI/2);
            case SOUTH -> new Quaternionf().rotateY((float)Math.PI);
            case WEST -> new Quaternionf().rotateY((float)Math.PI/2);

            //placed on floor or roof
            //base it off player direction
            default -> switch (player.getFacing())
            {
                //opposite here since the player is facing opposite to the face they're placing on

                case NORTH ->
                {
                    if(newBlockSpot.getBlock().getRelative(BlockFace.NORTH).isSolid())
                    {
                        yield new Quaternionf().rotateY((float)Math.PI);
                    }
                    else
                    {
                        var data = findBlockRot(newBlockSpot, BlockFace.NORTH, true);
                        movedFace = data.getRight();
                        yield data.getLeft();
                    }

                }

                case EAST ->
                {
                    if(newBlockSpot.getBlock().getRelative(BlockFace.EAST).isSolid())
                    {
                        yield new Quaternionf().rotateY((float)Math.PI/2);
                    }
                    else
                    {
                        var data = findBlockRot(newBlockSpot, BlockFace.EAST, true);
                        movedFace = data.getRight();
                        yield data.getLeft();
                    }

                }

                case WEST ->
                {
                    if(newBlockSpot.getBlock().getRelative(BlockFace.WEST).isSolid())
                    {
                        yield new Quaternionf().rotateY(-(float)Math.PI/2);
                    }
                    else
                    {
                        var data = findBlockRot(newBlockSpot, BlockFace.WEST, true);
                        movedFace = data.getRight();
                        yield data.getLeft();
                    }

                }

                case SOUTH ->
                {
                    if(newBlockSpot.getBlock().getRelative(BlockFace.SOUTH).isSolid())
                    {
                        yield new Quaternionf();
                    }
                    else
                    {
                        var data = findBlockRot(newBlockSpot, BlockFace.SOUTH, true);
                        movedFace = data.getRight();
                        yield data.getLeft();
                    }
                }

                default -> null;
            };
        };

        return Pair.of(rotation, movedFace);
    }

    static Pair<Quaternionf, BlockFace> findBlockRot(Location newBlockSpot, BlockFace failed, boolean wallOnly)
    {
        if(failed != BlockFace.NORTH && newBlockSpot.getBlock().getRelative(BlockFace.NORTH).isSolid())
        {
            return Pair.of(new Quaternionf().rotateY((float)Math.PI), BlockFace.SOUTH);
        }
        else if(failed != BlockFace.EAST && newBlockSpot.getBlock().getRelative(BlockFace.EAST).isSolid())
        {
            return Pair.of(new Quaternionf().rotateY((float)Math.PI/2), BlockFace.WEST);
        }
        else if(failed != BlockFace.SOUTH && newBlockSpot.getBlock().getRelative(BlockFace.SOUTH).isSolid())
        {
            return Pair.of(new Quaternionf().rotateY(0), BlockFace.NORTH);
        }
        else if(failed != BlockFace.WEST && newBlockSpot.getBlock().getRelative(BlockFace.WEST).isSolid())
        {
            return Pair.of(new Quaternionf().rotateY(-(float)Math.PI/2), BlockFace.EAST);
        }

        else if(!wallOnly)
        {
            if(failed != BlockFace.DOWN && newBlockSpot.getBlock().getRelative(BlockFace.DOWN).isSolid())
            {
                return Pair.of(new Quaternionf(), BlockFace.UP);
            }
            if(failed != BlockFace.UP && newBlockSpot.getBlock().getRelative(BlockFace.UP).isSolid())
            {
                return Pair.of(new Quaternionf(), BlockFace.DOWN);
            }
        }

        //no block found to place on
        return null;
    }

    static Quaternionf placeWallUD(Player player, BlockFace against)
    {
        return switch (against)
        {
            case DOWN -> new Quaternionf().rotateZ((float)Math.PI);
            case UP -> new Quaternionf();
            default  -> {
                Quaternionf rotation = new Quaternionf();

                //if the player is looking up, flip the model at this point
                if(player.getFacing().equals(BlockFace.UP))
                {
                    rotation.rotateY((float)Math.PI);
                }

                yield rotation.rotateX(-(float)Math.PI/2);
            }
        };
    }


    static Quaternionf placeWallD(BlockFace against)
    {
        return switch (against)
        {
            case DOWN -> new Quaternionf();
            default  -> new Quaternionf().rotateX(-(float)Math.PI/2);
        };
    }
}
