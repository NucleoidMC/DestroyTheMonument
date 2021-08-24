package eu.pb4.destroythemonument.game.map.generator;

import eu.pb4.destroythemonument.game.map.GameMap;
import kdotjpg.opensimplex.OpenSimplexNoise;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.game.world.generator.GameChunkGenerator;
import xyz.nucleoid.plasmid.game.world.generator.GeneratorBlockSamples;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DtmChunkGenerator extends GameChunkGenerator {
    private final GameMap map;
    private final OpenSimplexNoise baseNoise1;
    private final OpenSimplexNoise baseNoise2;
    private final OpenSimplexNoise heightNoise1;
    private final OpenSimplexNoise heightNoise2;
    private final OpenSimplexNoise mountainNoise1;

    public DtmChunkGenerator(MinecraftServer server, GameMap map) {
        super(createBiomeSource(server, RegistryKey.of(Registry.BIOME_KEY, map.config.biome())), new StructuresConfig(Optional.empty(), Collections.emptyMap()));
        this.map = map;
        long seed = (long) (Math.random() * Long.MAX_VALUE);

        this.heightNoise1 = new OpenSimplexNoise(Math.round(seed * 60 * Math.sin(seed ^ 3) * 10000));
        this.heightNoise2 = new OpenSimplexNoise(Math.round(seed * 60 * 10000));
        this.baseNoise1 = new OpenSimplexNoise(Math.round(seed * Math.sin(seed ^ 2) * 10000));
        this.baseNoise2 = new OpenSimplexNoise(Math.round(seed * 10000));
        this.mountainNoise1 = new OpenSimplexNoise(seed * 5238 + 132);
    }

    @Override
    public void setStructureStarts(DynamicRegistryManager registryManager, StructureAccessor accessor, Chunk chunk, StructureManager manager, long seed) {
    }

    @Override
    public void addStructureReferences(StructureWorldAccess world, StructureAccessor accessor, Chunk chunk) {
    }

    @Override
    public void carve(long seed, BiomeAccess access, Chunk chunk, GenerationStep.Carver carver) {
    }

    @Override
    public void buildSurface(ChunkRegion region, Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int i = chunkPos.x;
        int j = chunkPos.z;
        ChunkRandom chunkRandom = new ChunkRandom();
        chunkRandom.setTerrainSeed(i, j);
        ChunkPos chunkPos2 = chunk.getPos();
        int k = chunkPos2.getStartX();
        int l = chunkPos2.getStartZ();
        double d = 0.0625D;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for(int m = 0; m < 16; ++m) {
            for(int n = 0; n < 16; ++n) {
                int o = k + m;
                int p = l + n;
                int q = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, m, n) + 1;
                region.getBiome(mutable.set(k + m, q, l + n)).buildSurface(chunkRandom, chunk, o, p, q, 3, Blocks.STONE.getDefaultState(),Blocks.WATER.getDefaultState(), this.getSeaLevel(), 0, region.getSeed());
            }
        }
    }

    @Override
    public void generateFeatures(ChunkRegion region, StructureAccessor accessor) {
        ChunkPos chunkPos = region.getCenterPos();
        int i = chunkPos.getStartX();
        int j = chunkPos.getStartZ();
        BlockPos blockPos = new BlockPos(i, region.getBottomY(), j);
        Biome biome = this.populationSource.getBiomeForNoiseGen(chunkPos);
        ChunkRandom chunkRandom = new ChunkRandom();
        long l = chunkRandom.setPopulationSeed(region.getSeed(), i, j);

        try {
            biome.generateFeatureStep(accessor, this, region, l, chunkRandom, blockPos);
        } catch (Exception var13) {
            CrashReport crashReport = CrashReport.create(var13, "Biome decoration");
            crashReport.addElement("Generation").add("CenterX", (Object)chunkPos.x).add("CenterZ", (Object)chunkPos.z).add("Seed", (Object)l).add("Biome", (Object)biome);
            throw new CrashException(crashReport);
        }
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, StructureAccessor accessor, Chunk chunk) {
        var chunkPos = chunk.getPos();

        return CompletableFuture.supplyAsync(() -> {
            var protoChunk = (ProtoChunk) chunk;

            int minWorldX = chunkPos.getStartX();
            int minWorldZ = chunkPos.getStartZ();
            var templatePos = new BlockPos.Mutable();

            int minSectionY = this.map.mapBounds.min().getY() >> 4;
            int maxSectionY = this.map.mapBounds.max().getY() >> 4;

            double baseHeightVariation = 6;
            double heightVariation = 12;

            for (int sectionY = maxSectionY; sectionY >= minSectionY; sectionY--) {

                var section = protoChunk.getSection(sectionY);
                section.lock();

                try {
                    int minWorldY = sectionY << 4;

                    for (int z = 0; z < 16; z++) {
                        int worldZ = minWorldZ + z;
                        double dWorldZ = worldZ;

                        for (int x = 0; x < 16; x++) {
                            int worldX = minWorldX + x;

                            double dWorldX = worldX;


                            var layer1 = this.heightNoise1.eval(dWorldX / 120, dWorldZ / 120) + 0.4;
                            var layer2 = this.heightNoise2.eval(dWorldX / 10, dWorldZ / 10);
                            var mountain = this.mountainNoise1.eval(dWorldX / 60, dWorldZ / 60) + 1;

                            double h = layer1 + (layer2 + 1) / 2;

                            for (int y = 0; y < 16; y++) {
                                int worldY = y + minWorldY;
                                double dWorldY = worldY;

                                var dim = this.baseNoise1.eval(dWorldX / 90, dWorldY / 90, dWorldZ / 90);
                                var dim2 = this.baseNoise2.eval(dWorldX / 40, dWorldY / 40, dWorldZ / 40);

                                double height = 68 - (dim * (3 - h) + dim2 * h * layer1) * (baseHeightVariation + mountain * heightVariation);

                                var state = worldY < height ? Blocks.STONE.getDefaultState() : worldY >= this.getSeaLevel() ? Blocks.AIR.getDefaultState() : Blocks.WATER.getDefaultState();
                                templatePos.set(x + minWorldX, worldY, z + minWorldZ);
                                section.setBlockState(x, y, z, state, false);
                            }
                        }
                    }
                } finally {
                    section.unlock();
                }
            }

            return chunk;
        }, executor);
    }

    @Override
    public void populateEntities(ChunkRegion region) {

    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world) {
        if (this.map.mapBounds.contains(x, z)) {
            return 20;
        }
        return 0;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world) {
        if (this.map.mapBounds.contains(x, z)) {
            var mutablePos = new BlockPos.Mutable(x, 0, z);

            int minY = this.map.mapBounds.min().getY();
            int maxY = this.map.mapBounds.max().getY();

            var column = new BlockState[maxY - minY + 1];
            for (int y = maxY; y >= minY; y--) {
                mutablePos.setY(y);
                column[y] = Blocks.AIR.getDefaultState();
            }

            return new VerticalBlockSample(minY, column);
        }

        return GeneratorBlockSamples.VOID;
    }
}
