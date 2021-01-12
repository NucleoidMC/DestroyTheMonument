package eu.pb4.destroythemonument.game.map;

import eu.pb4.destroythemonument.game.map.DTMMapConfig;

import net.minecraft.text.LiteralText;
import net.minecraft.world.biome.BiomeKeys;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.MapTemplateMetadata;
import xyz.nucleoid.plasmid.map.template.MapTemplateSerializer;

import java.io.IOException;

public class DTMMapBuilder {

    private final DTMMapConfig config;

    public DTMMapBuilder(DTMMapConfig config) {
        this.config = config;
    }

    public DTMMap create() throws GameOpenException {
        try {
            MapTemplate template = MapTemplateSerializer.INSTANCE.loadFromResource(this.config.id);

            DTMMap map = new DTMMap(template, config);
            template.setBiome(BiomeKeys.FOREST);

            return map;
        } catch (IOException e) {
            throw new GameOpenException(new LiteralText("Failed to load template"), e);
        }
    }
}

