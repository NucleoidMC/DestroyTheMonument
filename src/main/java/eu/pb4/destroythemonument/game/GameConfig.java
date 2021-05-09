package eu.pb4.destroythemonument.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import eu.pb4.destroythemonument.map.MapConfig;
import xyz.nucleoid.plasmid.game.player.GameTeam;

import java.util.List;

public class GameConfig {
    public static final Codec<GameConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("gamemode").forGetter(config -> config.gamemode),
            PlayerConfig.CODEC.fieldOf("players").forGetter(config -> config.playerConfig),
            MapConfig.CODEC.fieldOf("map").forGetter(config -> config.mapConfig),
            GameTeam.CODEC.listOf().fieldOf("teams").forGetter(config -> config.teams),
            Codec.BOOL.optionalFieldOf("allowJoiningInGame", false).forGetter(config -> config.allowJoiningInGame),
            Codec.INT.optionalFieldOf("gameTime", -1).forGetter(config -> config.gameTime),
            Codec.list(Identifier.CODEC).fieldOf("kits").forGetter(config -> config.kits),
            Codec.INT.optionalFieldOf("tickRespawnTime", 60).forGetter(config -> config.tickRespawnTime)
            ).apply(instance, GameConfig::new));

    public final PlayerConfig playerConfig;
    public final MapConfig mapConfig;
    public final List<GameTeam> teams;
    public final boolean allowJoiningInGame;
    public final int gameTime;
    public final List<Identifier> kits;
    public final String gamemode;
    public final int tickRespawnTime;


    public GameConfig(String gamemode, PlayerConfig players, MapConfig mapConfig, List<GameTeam> teams, boolean allowJoiningInGame, int gameTime, List<Identifier> kits, int tickRespawnTime) {
        this.gamemode = gamemode;
        this.playerConfig = players;
        this.mapConfig = mapConfig;
        this.teams = teams;
        this.allowJoiningInGame = allowJoiningInGame;
        this.gameTime = gameTime;
        this.kits = kits;
        this.tickRespawnTime = tickRespawnTime;
    }
}
