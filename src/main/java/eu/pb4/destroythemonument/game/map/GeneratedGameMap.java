package eu.pb4.destroythemonument.game.map;

import eu.pb4.destroythemonument.game.logic.BaseGameLogic;
import eu.pb4.destroythemonument.game.GameConfig;
import eu.pb4.destroythemonument.game.data.TeamData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;

public final class GeneratedGameMap extends GameMap {
    private GeneratedGameMap(MapConfig config) {
        super(config, BlockBounds.of(-1000, 0, -1000, 1000, 255, 1000));
        this.validSpawn.add(new BlockPos(0, 100, 0));
    }

    public static GameMap create(MinecraftServer server, MapConfig config) {
        return new GeneratedGameMap(config);
    }

    public ChunkGenerator asGenerator(MinecraftServer server) {
        return null;//new DtmChunkGenerator(server, this);
    }

    @Override
    public void setTeamRegions(GameTeamKey team, TeamData data, GameConfig config) {

    }

    @Override
    public void onGameStart(BaseGameLogic logic) {

    }
}
