package eu.pb4.destroythemonument.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.plasmid.game.GameOpenException;

import java.io.IOException;

public class MapBuilder {

    private final MapConfig config;

    public MapBuilder(MapConfig config) {
        this.config = config;
    }

    public GameMap create(MinecraftServer server) throws GameOpenException {
        try {
            MapTemplate template = MapTemplateSerializer.loadFromResource(server, this.config.id);

            GameMap map = new GameMap(template, config);
            template.setBiome(RegistryKey.of(Registry.BIOME_KEY, config.biome));

            return map;
        } catch (IOException e) {
            throw new GameOpenException(new LiteralText("Failed to load template"), e);
        }
    }
}

