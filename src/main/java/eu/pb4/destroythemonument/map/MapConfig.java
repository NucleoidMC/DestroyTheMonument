package eu.pb4.destroythemonument.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

public class MapConfig {
    public static final Codec<MapConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(config -> config.id),
            Codec.LONG.optionalFieldOf("time", 6000L).forGetter(config -> config.time)
    ).apply(instance, MapConfig::new));

    public final Identifier id;
    public final long time;

    public MapConfig(Identifier id, long time) {
        this.id = id;
        this.time = time;
    }
}