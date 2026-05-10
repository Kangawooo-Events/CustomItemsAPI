package arnett.customItemsAPI.Helpers;

import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

public class DisplayHelper {

    //region Entity Glow

    /*=================================================================================================
                    -  Entity Glow  -
    =================================================================================================*/

    /**
     * Sends glow packet to make Entity glow for ONE player
     * @param player Player who recieves the glow packet
     * @param entity Entity to set glow
     * @param doGlow Whether this packet should make the entity glow or remove the entities glow
     * @param textColor Color for the entity to glow
     */
    public static void setGlowForPlayer(Player player, Entity entity, boolean doGlow, String textColor)
    {
        ChatFormatting color = getTextFormatColor(textColor);

        EntityDataAccessor<Byte> entityFlagAccessor = new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);
        Scoreboard glowScoreboard = new Scoreboard();

        //make sure entity is present
        if(entity == null)
            return;

        ServerPlayer cPlayer = ((CraftPlayer)player).getHandle();
        net.minecraft.world.entity.Entity cEntity = ((CraftEntity)entity).getHandle();

        //grab the data
        SynchedEntityData entityData = cEntity.getEntityData();

        //this contains the flags of the entity
        byte flags = entityData.get(entityFlagAccessor);

        // change the flag with a mask
        // 0x40 = 01000000

        if(doGlow)
            flags |= 0x40;
        else
            flags &= ~0x40;


        //data values which will be sent to the player
        List<SynchedEntityData.DataValue<?>> glowingData = List.of(SynchedEntityData.DataValue.create(entityFlagAccessor, flags));

        //create custom team
        PlayerTeam glowTeam = new PlayerTeam(glowScoreboard, (color.getName() + "_Glow"));

        //set the color of the team (which will be the glow color)
        glowTeam.setColor(color);

        //tell client to create or remove team
        cPlayer.connection.send(
                // ...Packet(team, add/update[true] or remove[false])
                ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(glowTeam, doGlow)
        );

        //add the display to the team
        cPlayer.connection.send(
                //this kinda just pretends like the entity is a player and ships it's uuid instead
                ClientboundSetPlayerTeamPacket.createPlayerPacket(
                        glowTeam,
                        entity.getUniqueId().toString(),
                        doGlow ? ClientboundSetPlayerTeamPacket.Action.ADD : ClientboundSetPlayerTeamPacket.Action.REMOVE
                )
        );

        //ship glow to the player
        cPlayer.connection.send(
                new ClientboundSetEntityDataPacket(
                        entity.getEntityId(),
                        glowingData
                ));


        //set it back for everyone else
        flags &= ~0x40;
        entityData.set(entityFlagAccessor, flags);
    }

    /**
     * Gets a Text color from a string (Case Insensitive), ex// light_gray -> ChatFormatting.GRAY <br>
     * Exists because ChatFormatting is nms so user may not have it.<br>
     * You may also use Hex code ex// "ABCDEF" -> really light blue
     * @param color Name of color
     * @return ChatFormatting color
     */
    public static ChatFormatting getTextFormatColor(String color)
    {
        try {
            //unfortunately, these colors don't match the ChatFormatting colors so we gotta do some mismatching
            return switch (color.toUpperCase())
            {
                case "LIGHT_GRAY" -> ChatFormatting.GRAY;
                case "GRAY" -> ChatFormatting.DARK_GRAY;
                case "BLACK" -> ChatFormatting.BLACK;
                case "BROWN" -> ChatFormatting.GOLD;
                case "RED" -> ChatFormatting.RED;
                case "ORANGE" -> ChatFormatting.GOLD;
                case "YELLOW" -> ChatFormatting.YELLOW;
                case "LIME" -> ChatFormatting.GREEN;
                case "GREEN" -> ChatFormatting.GREEN;
                case "CYAN" -> ChatFormatting.AQUA;
                case "LIGHT_BLUE" -> ChatFormatting.BLUE;
                case "BLUE" -> ChatFormatting.DARK_BLUE;
                case "PURPLE" -> ChatFormatting.DARK_PURPLE;
                case "MAGENTA" -> ChatFormatting.LIGHT_PURPLE;
                case "PINK" -> ChatFormatting.LIGHT_PURPLE;
                default -> ChatFormatting.getByHexValue(Integer.parseInt(color.substring(0, 6), 16));
            };

        }
        catch (Exception e)
        {
            return ChatFormatting.WHITE;
        }
    }

    //endregion

}
