package eu.pb4.destroythemonument.game;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.plasmid.game.GameSpace;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.game.map.DTMMap;
import xyz.nucleoid.plasmid.util.BlockBounds;
import xyz.nucleoid.plasmid.util.PlayerRef;

import java.util.Random;

public class DTMSpawnLogic {
    private final GameSpace gameSpace;
    private final DTMMap map;
    private final Object2ObjectMap<PlayerRef, DTMPlayer> participants;

    public DTMSpawnLogic(GameSpace gameSpace, DTMMap map, Object2ObjectMap<PlayerRef, DTMPlayer> participants) {
        this.gameSpace = gameSpace;
        this.map = map;
        this.participants = participants;
    }

    public void resetPlayer(ServerPlayerEntity player, GameMode gameMode) {
        player.setGameMode(gameMode);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0f;
    }

    public void spawnPlayer(ServerPlayerEntity entity) {
        ServerWorld world = this.gameSpace.getWorld();
        if (this.participants != null) {
            DTMPlayer player = participants.get(PlayerRef.of(entity));

            if (player != null && player.team != null) {
                BlockBounds spawn = this.map.teamRegions.get(player.team).getSpawn();

                Double x = MathHelper.nextDouble(entity.getRandom(), spawn.getMin().getX(), spawn.getMax().getX());
                Double z = MathHelper.nextDouble(entity.getRandom(), spawn.getMin().getZ(), spawn.getMax().getZ());

                entity.teleport(world, x, spawn.getMin().getY(), z, entity.yaw, entity.pitch);
                return;
            }
        }

        Double x = MathHelper.nextDouble(entity.getRandom(), this.map.spawn.getMin().getX(), this.map.spawn.getMax().getX());
        Double z = MathHelper.nextDouble(entity.getRandom(), this.map.spawn.getMin().getZ(), this.map.spawn.getMax().getZ());

        entity.teleport(world, x, this.map.spawn.getMin().getY(), z, entity.yaw, entity.pitch);
    }
}
