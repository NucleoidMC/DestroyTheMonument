package eu.pb4.destroythemonument.map;

import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.game.TeamData;
import eu.pb4.destroythemonument.other.DtmUtil;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GameMap {
    private final MapTemplate template;
    private final MapConfig config;
    private final Set<BlockBounds> unbreakable;
    private final Set<BlockPos> taters;
    private final List<BlockPos> validSpawn;
    public BlockBounds mapBounds;
    public BlockBounds mapDeathBounds;
    public ServerWorld world;


    public GameMap(MapTemplate template, MapConfig config) {
        this.template = template;
        this.config = config;
        this.unbreakable = template.getMetadata().getRegionBounds("unbreakable").collect(Collectors.toSet());
        this.validSpawn = new ArrayList<>();
        for (BlockPos pos : template.getMetadata().getFirstRegionBounds("general_spawn")) {
            BlockState blockState = this.template.getBlockState(pos);
            if (blockState.isAir() || TagRegistry.block(DtmUtil.id("spawnable")).contains(blockState.getBlock())) {
                this.validSpawn.add(pos.toImmutable());
            }
        }
        this.mapBounds = template.getBounds();
        this.mapDeathBounds = BlockBounds.of(this.mapBounds.min().mutableCopy().add(-5, -5, -5), this.mapBounds.max().mutableCopy().add(5, 5, 5));
        this.taters = template.getMetadata().getRegionBounds("tater").map((r) -> r.min()).collect(Collectors.toSet());
    }

    public ChunkGenerator asGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template);
    }

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

    public void setTeamRegions(GameTeam team, TeamData data) {
        TemplateRegion spawn = this.template.getMetadata().getFirstRegion(team.key() + "_spawn");
        Set<BlockBounds> monuments = this.template.getMetadata().getRegionBounds(team.key() + "_monument").collect(Collectors.toSet());
        Set<BlockBounds> classChange = this.template.getMetadata().getRegionBounds(team.key() + "_class_change").collect(Collectors.toSet());

        for (BlockBounds monument : monuments) {
            this.template.setBlockState(monument.min(), Blocks.BEACON.getDefaultState());
        }

        List<BlockPos> validSpawnPos = new ArrayList<>();

        for (BlockPos pos : spawn.getBounds()) {
            BlockState blockState = this.template.getBlockState(pos);
            if (blockState.isAir() || TagRegistry.block(DtmUtil.id("spawnable")).contains(blockState.getBlock())) {
                validSpawnPos.add(pos.toImmutable());
            }
        }

        data.setTeamRegions(validSpawnPos, MathHelper.wrapDegrees(spawn.getData().getFloat("yaw")), monuments, classChange);
    }

    public BlockPos getRandomSpawnPos() {
        return this.validSpawn.get(DTM.RANDOM.nextInt(this.validSpawn.size()));
    }

    public Vec3d getRandomSpawnPosAsVec3d() {
        BlockPos pos = getRandomSpawnPos();
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }
}
