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
    public static void setGlowForPlayer(Player player, Entity e, boolean doGlow, String textColor)
    {
        ChatFormatting color = TextColorHelper.getTextFormatColor(textColor);

        EntityDataAccessor<Byte> entityFlagAccessor = new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);
        Scoreboard glowScoreboard = new Scoreboard();

        //make sure entity is present
        if(e == null)
            return;

        ServerPlayer cPlayer = ((CraftPlayer)player).getHandle();
        net.minecraft.world.entity.Entity cEntity = ((CraftEntity)e).getHandle();

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
                        e.getUniqueId().toString(),
                        doGlow ? ClientboundSetPlayerTeamPacket.Action.ADD : ClientboundSetPlayerTeamPacket.Action.REMOVE
                )
        );

        //ship glow to the player
        cPlayer.connection.send(
                new ClientboundSetEntityDataPacket(
                        e.getEntityId(),
                        glowingData
                ));


        //set it back for everyone else
        flags &= ~0x40;
        entityData.set(entityFlagAccessor, flags);
    }
}
