package eu.pb4.destroythemonument.game.map.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.plasmid.api.game.level.generator.TemplateChunkGenerator;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class TemplateWithLayerGenerator extends TemplateChunkGenerator {
    private final List<BlockState> layer;
    private final int height;

    public TemplateWithLayerGenerator(MinecraftServer server, MapTemplate template, List<BlockState> layer, int height) {
        super(server, template);
        this.layer = layer;
        this.height = height;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState noiseConfig, StructureManager structureAccessor, ChunkAccess chunk) {
        return super.fillFromNoise(blender, noiseConfig, structureAccessor, chunk).handle(this::addLayers);
    }

    private ChunkAccess addLayers(ChunkAccess chunk, Throwable throwable) {
        int y = this.height;

        var mutablePos = new BlockPos.MutableBlockPos();

        var chunkPos = chunk.getPos();
        int minWorldX = chunkPos.getMinBlockX();
        int minWorldZ = chunkPos.getMinBlockZ();

        var pos = new BlockPos.MutableBlockPos();

        for (var state : this.layer) {
            pos.setY(y++);
            for (int x = 0; x < 16; x++) {
                pos.setX(minWorldX + x);
                for (int z = 0; z < 16; z++) {
                    pos.setZ(minWorldZ + z);

                    var current = chunk.getBlockState(pos);

                    if (current.isAir()) {
                        chunk.setBlockState(pos, state);
                    }
                }
            }
        }


        return chunk;
    }
}
