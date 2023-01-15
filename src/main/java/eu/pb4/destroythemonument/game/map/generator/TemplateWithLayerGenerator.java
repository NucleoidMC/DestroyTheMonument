package eu.pb4.destroythemonument.game.map.generator;

import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.noise.NoiseConfig;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

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
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        return super.populateNoise(executor, blender, noiseConfig, structureAccessor, chunk).handleAsync(this::addLayers, executor);
    }

    private Chunk addLayers(Chunk chunk, Throwable throwable) {
        int y = this.height;

        var mutablePos = new BlockPos.Mutable();

        var chunkPos = chunk.getPos();
        int minWorldX = chunkPos.getStartX();
        int minWorldZ = chunkPos.getStartZ();

        var pos = new BlockPos.Mutable();

        for (var state : this.layer) {
            pos.setY(y++);
            for (int x = 0; x < 16; x++) {
                pos.setX(minWorldX + x);
                for (int z = 0; z < 16; z++) {
                    pos.setZ(minWorldZ + z);

                    var current = chunk.getBlockState(pos);

                    if (current.isAir()) {
                        chunk.setBlockState(pos, state, false);
                    }
                }
            }
        }


        return chunk;
    }
}
