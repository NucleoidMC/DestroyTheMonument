package eu.pb4.destroythemonument.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.BiomeKeys;
import xyz.nucleoid.codecs.MoreCodecs;

import java.util.List;
import java.util.Optional;

public record MapConfig(Identifier id, long time, Identifier biome,
                        String author, BlockState monument,
                        List<BlockState> generatedLayer,
                        Optional<Integer> startLayerAt,
                        Optional<Integer> deathPlane
) {
    public static final Codec<MapConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(MapConfig::id),
            Codec.LONG.optionalFieldOf("time", 6000L).forGetter(MapConfig::time),
            Identifier.CODEC.optionalFieldOf("biome", BiomeKeys.FOREST.getValue()).forGetter(MapConfig::biome),
            Codec.STRING.optionalFieldOf("author", "Unknown (pls fix)").forGetter(MapConfig::author),
            MoreCodecs.BLOCK_STATE.optionalFieldOf("monument_block", Blocks.BEACON.getDefaultState()).forGetter(MapConfig::monument),
            Codec.list(MoreCodecs.BLOCK_STATE).optionalFieldOf("generated_layer", List.of()).forGetter(MapConfig::generatedLayer),
            Codec.INT.optionalFieldOf("layer_start").forGetter(MapConfig::startLayerAt),
            Codec.INT.optionalFieldOf("death_plane").forGetter(MapConfig::deathPlane)

    ).apply(instance, MapConfig::new));

}