package eu.pb4.destroythemonument.map;

import net.minecraft.text.LiteralText;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.BiomeKeys;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.MapTemplateSerializer;

import java.io.IOException;

public class MapBuilder {

    private final MapConfig config;

    public MapBuilder(MapConfig config) {
        this.config = config;
    }

    public Map create() throws GameOpenException {
        try {
            MapTemplate template = MapTemplateSerializer.INSTANCE.loadFromResource(this.config.id);

            Map map = new Map(template, config);
            template.setBiome(RegistryKey.of(Registry.BIOME_KEY, config.biome));

            return map;
        } catch (IOException e) {
            throw new GameOpenException(new LiteralText("Failed to load template"), e);
        }
    }
}

