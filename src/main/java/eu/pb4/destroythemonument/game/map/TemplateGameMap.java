package eu.pb4.destroythemonument.game.map;

import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.game.data.Monument;
import eu.pb4.destroythemonument.game.logic.BaseGameLogic;
import eu.pb4.destroythemonument.game.GameConfig;
import eu.pb4.destroythemonument.game.data.TeamData;
import eu.pb4.destroythemonument.game.map.generator.TemplateWithLayerGenerator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.apache.commons.lang3.mutable.MutableInt;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class TemplateGameMap extends GameMap {
    private final MapTemplate template;

    private TemplateGameMap(MapTemplate template, MapConfig config) {
        super(config, template.getBounds());
        this.template = template;
        this.unbreakable.addAll(template.getMetadata().getRegionBounds("unbreakable").toList());
        for (BlockPos pos : Objects.requireNonNull(template.getMetadata().getFirstRegionBounds("general_spawn"))) {
            BlockState blockState = this.template.getBlockState(pos);
            if (blockState.isAir() || blockState.isIn(DTM.SPAWNABLE_TAG)) {
                this.validSpawn.add(pos.toImmutable());
            }
        }
        this.taters.addAll(template.getMetadata().getRegionBounds("tater").map(BlockBounds::min).toList());
        this.destroyOnStart.addAll(template.getMetadata().getRegionBounds("destroy_on_start").toList());
    }

    public static GameMap create(MinecraftServer server, MapConfig config) throws IOException {
        MapTemplate template = MapTemplateSerializer.loadFromResource(server, config.id());
        template.setBiome(RegistryKey.of(RegistryKeys.BIOME, config.biome()));
        GameMap map = new TemplateGameMap(template, config);
        return map;
    }

    public ChunkGenerator asGenerator(MinecraftServer server) {
        return config.startLayerAt().isPresent() ? new TemplateWithLayerGenerator(server, this.template, config.generatedLayer(), config.startLayerAt().get()) : new TemplateChunkGenerator(server, this.template);
    }

    public void setTeamRegions(GameTeamKey team, TeamData data, GameConfig config) {
        TemplateRegion spawn = this.template.getMetadata().getFirstRegion(team.id() + "_spawn");
        var monuments = this.template.getMetadata().getRegions(team.id() + "_monument").collect(Collectors.toList());
        var classChange = this.template.getMetadata().getRegionBounds(team.id() + "_class_change").collect(Collectors.toSet());

        for (var monument : monuments) {
            this.template.setBlockState(monument.getBounds().min(), this.config.monument());
        }

        List<BlockPos> validSpawnPos = new ArrayList<>();

        assert spawn != null;
        for (BlockPos pos : spawn.getBounds()) {
            BlockState blockState = this.template.getBlockState(pos);
            if (blockState.isAir() || blockState.isIn(DTM.SPAWNABLE_TAG)) {
                validSpawnPos.add(pos.toImmutable());
            }
        }

        data.setTeamRegions(validSpawnPos, MathHelper.wrapDegrees(spawn.getData().getFloat("yaw")), monuments, classChange, this, config);
    }

    public void onGameStart(BaseGameLogic logic) {
        for (var bound : this.destroyOnStart) {
            for (var pos : bound) {
                this.world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS, 1);
            }
        }
    }

    @Override
    public void loadCaptureGamemodeData(GameConfig config) {
        var i = new MutableInt();

        this.template.getMetadata().getRegions("monument").forEach(region -> {
            this.addMonument(Monument.createFrom(config, this, region, "" + i.getAndIncrement(), "capturable.", null));
        });


        this.template.getMetadata().getRegionBounds("monument_region").forEach(this.monumentRegionBounds::add);
    }
}
