package eu.pb4.destroythemonument.kit;

/*
 * Based on https://github.com/NucleoidMC/plasmid/blob/1.16/src/main/java/xyz/nucleoid/plasmid/game/config/GameConfigs.java
 */

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import eu.pb4.destroythemonument.DTM;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import com.mojang.datafixers.util.Pair;
import xyz.nucleoid.plasmid.registry.TinyRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;

public class KitsRegistry {
    private static final TinyRegistry<Kit> KITS = TinyRegistry.create();

    public static void register() {
        ResourceManagerHelper serverData = ResourceManagerHelper.get(ResourceType.SERVER_DATA);

        serverData.registerReloadListener(new SimpleSynchronousResourceReloadListener() {

            @Override
            public Identifier getFabricId() {
                return new Identifier(DTM.ID, "kits_dtm");
            }

            @Override
            public void reload(ResourceManager manager) {
                KITS.clear();

                var resources = manager.findResources("kits_dtm", path -> path.getPath().endsWith(".json"));

                for (var path : resources.entrySet()) {
                    try {
                        try (Reader reader = new BufferedReader(new InputStreamReader(path.getValue().getInputStream()))) {
                            JsonElement json = new JsonParser().parse(reader);

                            Identifier identifier = identifierFromPath(path.getKey());

                            DataResult<Kit> result = Kit.CODEC.decode(JsonOps.INSTANCE, json).map(Pair::getFirst);

                            result.result().ifPresent(game -> KITS.register(identifier, game));

                            result.error().ifPresent(error -> DTM.LOGGER.error("Failed to decode kit at {}: {}", path, error.toString()));
                        }
                    } catch (IOException e) {
                        DTM.LOGGER.error("Failed to kit at {}", path, e);
                    }
                }
            }
        });
    }

    private static Identifier identifierFromPath(Identifier location) {
        String path = location.getPath();
        path = path.substring("kits_dtm/".length(), path.length() - ".json".length());
        return new Identifier(location.getNamespace(), path);
    }

    @Nullable
    public static Kit get(Identifier identifier) {
        return KITS.get(identifier);
    }
}
