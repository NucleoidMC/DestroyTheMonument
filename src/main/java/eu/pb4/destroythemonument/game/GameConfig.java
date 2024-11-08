package eu.pb4.destroythemonument.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import eu.pb4.destroythemonument.game.map.MapConfig;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamList;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record GameConfig(String gamemode, WaitingLobbyConfig players,
                         MapConfig map, GameTeamList teams,
                         boolean allowJoiningInGame, int gameTime,
                         List<Identifier> kits, int tickRespawnTime,
                         Optional<Map<String, String>> monumentRemaps
) {

    public static final MapCodec<GameConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("gamemode").forGetter(GameConfig::gamemode),
            WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(GameConfig::players),
            MapConfig.CODEC.fieldOf("map").forGetter(GameConfig::map),
            GameTeamList.CODEC.fieldOf("teams").forGetter(GameConfig::teams),
            Codec.BOOL.optionalFieldOf("allowJoiningInGame", false).forGetter(GameConfig::allowJoiningInGame),
            Codec.INT.optionalFieldOf("gameTime", -1).forGetter(GameConfig::gameTime),
            Codec.list(Identifier.CODEC).fieldOf("kits").forGetter(GameConfig::kits),
            Codec.INT.optionalFieldOf("tickRespawnTime", 60).forGetter(GameConfig::tickRespawnTime),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("monument_renames").forGetter(GameConfig::monumentRemaps)
            ).apply(instance, GameConfig::new));
}
