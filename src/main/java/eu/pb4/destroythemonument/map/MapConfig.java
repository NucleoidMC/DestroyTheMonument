package eu.pb4.destroythemonument.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.BiomeKeys;

public record MapConfig(Identifier id, long time, Identifier biome,
                        String author) {
    public static final Codec<MapConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(config -> config.id),
            Codec.LONG.optionalFieldOf("time", 6000L).forGetter(config -> config.time),
            Identifier.CODEC.optionalFieldOf("biome", BiomeKeys.FOREST.getValue()).forGetter(config -> config.biome),
            Codec.STRING.optionalFieldOf("author", "Unknown (pls fix)").forGetter(config -> config.author)

    ).apply(instance, MapConfig::new));

}