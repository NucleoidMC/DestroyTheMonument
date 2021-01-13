package eu.pb4.destroythemonument.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import eu.pb4.destroythemonument.game.map.DTMMapConfig;
import xyz.nucleoid.plasmid.game.player.GameTeam;

import java.util.List;

public class DTMConfig {
    public static final Codec<DTMConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlayerConfig.CODEC.fieldOf("players").forGetter(config -> config.playerConfig),
            DTMMapConfig.CODEC.fieldOf("map").forGetter(config -> config.mapConfig),
            GameTeam.CODEC.listOf().fieldOf("teams").forGetter(config -> config.teams),
            Codec.BOOL.optionalFieldOf("allowJoiningInGame", false).forGetter(config -> config.allowJoiningInGame),
            Codec.INT.optionalFieldOf("gameTime", -1).forGetter(config -> config.gameTime)
    ).apply(instance, DTMConfig::new));

    public final PlayerConfig playerConfig;
    public final DTMMapConfig mapConfig;
    public final List<GameTeam> teams;
    public final boolean allowJoiningInGame;
    public final int gameTime;

    public DTMConfig(PlayerConfig players, DTMMapConfig mapConfig, List<GameTeam> teams, boolean allowJoiningInGame, int gameTime) {
        this.playerConfig = players;
        this.mapConfig = mapConfig;
        this.teams = teams;
        this.allowJoiningInGame = allowJoiningInGame;
        this.gameTime = gameTime;
    }
}
