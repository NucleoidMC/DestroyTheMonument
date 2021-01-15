package eu.pb4.destroythemonument.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

public class DTMMapConfig {
    public static final Codec<DTMMapConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(config -> config.id),
            Codec.LONG.optionalFieldOf("time", 6000L).forGetter(config -> config.time)
    ).apply(instance, DTMMapConfig::new));

    public final Identifier id;
    public final long time;

    public DTMMapConfig(Identifier id, long time) {
        this.id = id;
        this.time = time;
    }
}