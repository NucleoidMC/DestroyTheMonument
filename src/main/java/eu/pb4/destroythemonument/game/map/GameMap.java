package eu.pb4.destroythemonument.game.map;

import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.game.logic.BaseGameLogic;
import eu.pb4.destroythemonument.game.GameConfig;
import eu.pb4.destroythemonument.game.data.Monument;
import eu.pb4.destroythemonument.game.data.TeamData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.game.common.team.GameTeamKey;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class GameMap {
    public final MapConfig config;
    protected final Set<BlockBounds> unbreakable = new HashSet<>();
    protected final Set<BlockPos> taters  = new HashSet<>();
    protected final List<BlockPos> validSpawn = new ArrayList<>();
    protected final List<BlockBounds> destroyOnStart = new ArrayList<>();
    public BlockBounds mapBounds;
    public BlockBounds mapDeathBounds;
    public ServerWorld world;
    public final List<Monument> monuments = new ArrayList<>();


    public GameMap(MapConfig config, BlockBounds mapBounds) {
        this.config = config;
        this.mapBounds = mapBounds;
        this.mapDeathBounds = BlockBounds.of(this.mapBounds.min().mutableCopy().add(-5, -5, -5), this.mapBounds.max().mutableCopy().add(5, 5, 5));
    }

    public abstract ChunkGenerator asGenerator(MinecraftServer server);

    public boolean isUnbreakable(BlockPos block) {
        for (BlockBounds bound : this.unbreakable) {
            if (bound.contains(block)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTater(BlockPos taterPos) {
        for (BlockPos pos : this.taters) {
            if (pos.equals(taterPos)) {
                return true;
            }
        }
        return false;
    }

    public abstract void setTeamRegions(GameTeamKey team, TeamData data, GameConfig config);

    public BlockPos getRandomSpawnPos() {
        return this.validSpawn.get(DTM.RANDOM.nextInt(this.validSpawn.size()));
    }

    public Vec3d getRandomSpawnPosAsVec3d() {
        BlockPos pos = getRandomSpawnPos();
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }

    public abstract void onGameStart(BaseGameLogic logic);
}
