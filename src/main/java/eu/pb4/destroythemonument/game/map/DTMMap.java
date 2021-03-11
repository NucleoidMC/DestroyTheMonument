package eu.pb4.destroythemonument.game.map;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.TemplateChunkGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.util.Set;
import java.util.stream.Collectors;

public class DTMMap {
    private final MapTemplate template;
    private final DTMMapConfig config;
    private final Set<BlockBounds> unbreakable;
    public BlockBounds spawn;
    public BlockBounds mapBounds;
    public BlockBounds mapDeathBounds;

    public final Object2ObjectMap<GameTeam, DTMTeamRegions> teamRegions = new Object2ObjectOpenHashMap<>();

    public DTMMap(MapTemplate template, DTMMapConfig config) {
        this.template = template;
        this.config = config;
        this.unbreakable = template.getMetadata().getRegionBounds("unbreakable").collect(Collectors.toSet());
        this.spawn = template.getMetadata().getFirstRegionBounds("general_spawn");
        this.mapBounds = template.getBounds();
        this.mapDeathBounds = new BlockBounds(this.mapBounds.getMin().mutableCopy().add(-5, -5, -5), this.mapBounds.getMax().mutableCopy().add(5, 5, 5));
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

    public void addTeamRegions(GameTeam team) {
        BlockBounds spawn = this.template.getMetadata().getFirstRegionBounds(team.getKey() + "_spawn");
        Set<BlockBounds> monuments = this.template.getMetadata().getRegionBounds(team.getKey() + "_monument").collect(Collectors.toSet());
        BlockBounds classChange = this.template.getMetadata().getFirstRegionBounds(team.getKey() + "_class_change");

        for (BlockBounds monument : monuments) {
            this.template.setBlockState(monument.getMin(), Blocks.BEACON.getDefaultState());
        }

        this.teamRegions.put(team, new DTMTeamRegions(team, spawn, monuments, classChange));
    }
}
