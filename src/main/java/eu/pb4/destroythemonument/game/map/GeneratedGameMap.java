package eu.pb4.destroythemonument.game.map;

import eu.pb4.destroythemonument.game.BaseGameLogic;
import eu.pb4.destroythemonument.game.data.TeamData;
import eu.pb4.destroythemonument.game.map.generator.DtmChunkGenerator;
import eu.pb4.destroythemonument.other.DtmUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class GeneratedGameMap extends GameMap {
    private GeneratedGameMap(MapConfig config) {
        super(config, BlockBounds.of(-1000, 0, -1000, 1000, 255, 1000));
        this.validSpawn.add(new BlockPos(0, 100, 0));
    }

    public static GameMap create(MinecraftServer server, MapConfig config) {
        return new GeneratedGameMap(config);
    }

    public ChunkGenerator asGenerator(MinecraftServer server) {
        return new DtmChunkGenerator(server, this);
    }

    public void setTeamRegions(GameTeamKey team, TeamData data) {

    }

    @Override
    public void onGameStart(BaseGameLogic logic) {

    }
}
