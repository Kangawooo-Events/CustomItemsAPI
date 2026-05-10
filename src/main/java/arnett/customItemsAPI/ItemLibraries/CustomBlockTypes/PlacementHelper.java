package arnett.customItemsAPI.ItemLibraries.CustomBlockTypes;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.*;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;

public class PlacementHelper {

    //region Up / Down

    /*=================================================================================================
                    -  Up / Down  -
    =================================================================================================*/

    /**
     * Gets the placement location and rotation from the placement information passed,
     * This rotates the display to face Up or Down, like dripstone
     * @param player The player who placed the block
     * @param against the face the block was placed against
     * @return Quaternion Rotation
     */
    static Quaternionf placeUD(Player player, BlockFace against)
    {
        return switch (against) {

            case UP -> new Quaternionf().rotateZ((float)Math.PI);

            case DOWN -> new Quaternionf();

            //placed on a wall so base of placed direction
            default -> player.getPitch() > -15 ?
                    //looking up
                    new Quaternionf().rotateY((float)Math.PI) :
                    //looking down
                    new Quaternionf();
        };
    }

    /**
     * Gets a rotation from the placement information passed,
     * This rotates the display to face up or down and rotates,
     * it to be placed on the wall at it's -x rotation
     * (so use NESW rotations to set that first)
     * @param player The player who placed the block
     * @param against the face the block was placed against
     * @return Quaternion Rotation
     */
    static Quaternionf placeWallUD(Player player, BlockFace against)
    {
        return switch (against)
        {
            //if this is aginst the top flip it
            case DOWN -> new Quaternionf().rotateZ((float)Math.PI);
            //if this is aginst the floor do nothing
            case UP -> new Quaternionf();


            default  -> {
                Quaternionf rotation = new Quaternionf();

                //if the player is looking up, flip the model at this point
                if(player.getPitch() > -15)
                {
                    rotation.rotateY((float)Math.PI);
                }

                //rotate it to be placed on the wall
                yield rotation.rotateX(-(float)Math.PI/2);
            }
        };
    }

    //endregion


    //region North / East / South / West

    /*=================================================================================================
                    -  NESW  -
    =================================================================================================*/

    /**
     * Gets a rotation from the placement information passed,
     * This rotates the display to face only NESW like a chest,
     * but it requires a block in that direction, in CAPI this is
     * only used to rotate the dispaly towards a wall,
     * and then gets rotated elsewhere by PI/2 to be placed on the wall
     * @param player The player who placed the block
     * @param against the face the block was placed against
     * @return Quaternion Rotation
     */
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


    /**
     * Gets a rotation from the placement information passed,
     * This rotates the display to face only NESW like a chest
     * @param player The player who placed the block
     * @param against the face the block was placed against
     * @return Quaternion Rotation
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
                case SOUTH -> new Quaternionf().rotateY((float)Math.PI);
                case WEST -> new Quaternionf().rotateY((float)Math.PI/2);
                case EAST -> new Quaternionf().rotateY(-(float)Math.PI/2);
                default -> new Quaternionf().rotateY(0);
            };
        };
    }

    //endregion


    //region Wall

    /*=================================================================================================
                    -  Wall  -
    =================================================================================================*/


    /**
     * Gets a rotation from the placement information passed,
     * This rotates the display to face up or down or
     * on the wall at it's -x rotation
     * @param against the face the block was placed against
     * @return Quaternion Rotation
     */
    static Quaternionf placeWallD(BlockFace against)
    {
        return switch (against)
        {
            case DOWN -> new Quaternionf();
            default  -> new Quaternionf().rotateX(-(float)Math.PI/2);
        };
    }

    /**
     * Gets a rotation from the placement information passed,
     * This rotates the display to face up or down depending
     * on player pitch, or does nothing if on Floor or Roof
     * @param player The player who placed the block
     * @param against the face the block was placed against
     * @return Quaternion Rotation
     */
    static Quaternionf placeWallDirectional(Player player, BlockFace against)
    {
        if(BlockFace.UP == against || against == BlockFace.DOWN)
            return new Quaternionf();

        return player.getPitch() > -15 ?
                //looking up
                new Quaternionf().rotateX((float)Math.PI) :
                //looking down
                new Quaternionf();
    }

    //endregion


    //region Find Block

    /*=================================================================================================
                    -  Find Block  -
    =================================================================================================*/

    /**
     * Checks adjacent blocks to determine if they can be built on
     * @param newBlockSpot Center Block to check
     * @param failed Blockface which has already failed the check (leave NULL if unavailable)
     * @param wallOnly Is this only placed on a wall or can it be placed on the floor or roof
     * @return pair of Quaternion rotation (NESW rotation on Y) to a buildable blockface and the blockface itself
     */
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

    //endregion

}
