package eu.pb4.destroythemonument.game;

import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.destroythemonument.game.map.GameMap;
import eu.pb4.destroythemonument.other.DtmResetable;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.util.PlayerMap;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public record SpawnLogic(GameSpace gameSpace, GameMap map,
                         PlayerMap<PlayerData> participants,
                         Teams teams) {

    public void resetPlayer(ServerPlayer player, GameType gameMode) {
        this.resetPlayer(player, gameMode, true);
    }

    public void resetPlayer(ServerPlayer player, GameType gameMode, boolean resetInventory) {
        player.setInvisible(false);
        player.setNoGravity(false);
        player.setRemainingFireTicks(0);
        player.setGameMode(gameMode);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0f;
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
        player.removeAllEffects();
        ((DtmResetable) player.getAttributes()).dtm$reset();
        ((DtmResetable) player.gameMode).dtm$reset();
        if (resetInventory) {
            player.getInventory().clearContent();
        }
    }

    public void spawnPlayer(ServerPlayer entity) {
        ServerLevel world = this.map.world;
        if (this.participants != null) {
            PlayerData player = participants.get(PlayerRef.of(entity));
            if (player != null && player.teamData != null) {
                BlockPos pos = player.nextSpawnPos;

                if (pos == null) {
                    pos = player.teamData.getRandomSpawnPos();
                }
                player.nextSpawnPos = null;

                entity.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), player.teamData.spawnYaw, 0, false);
                return;
            }
        }

        BlockPos pos = this.map.getRandomSpawnPos();

        entity.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), entity.getYRot(), entity.getXRot(), false);
    }
}
