package eu.pb4.destroythemonument.game;

import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.destroythemonument.game.data.TeamData;
import eu.pb4.destroythemonument.game.map.GameMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.util.PlayerRef;

public record SpawnLogic(GameSpace gameSpace, GameMap map,
                         Object2ObjectMap<PlayerRef, PlayerData> participants,
                         Teams teams) {

    public void resetPlayer(ServerPlayerEntity player, GameMode gameMode) {
        this.resetPlayer(player, gameMode, true);
    }

    public void resetPlayer(ServerPlayerEntity player, GameMode gameMode, boolean resetInventory) {
        player.setInvisible(false);
        player.setNoGravity(false);
        player.changeGameMode(gameMode);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0f;
        player.setHealth(player.getMaxHealth());
        player.getHungerManager().setFoodLevel(20);
        player.clearStatusEffects();
        if (resetInventory) {
            player.getInventory().clear();
        }
    }

    public void spawnPlayer(ServerPlayerEntity entity) {
        ServerWorld world = this.map.world;
        if (this.participants != null) {
            PlayerData player = participants.get(PlayerRef.of(entity));
            if (player != null && player.teamData != null) {
                BlockPos pos = player.nextSpawnPos;

                if (pos == null) {
                    pos = player.teamData.getRandomSpawnPos();
                }
                player.nextSpawnPos = null;
                entity.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.teamData.spawnYaw, 0);
                return;
            }
        }

        BlockPos pos = this.map.getRandomSpawnPos();

        entity.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, entity.getYaw(), entity.getPitch());
    }
}
