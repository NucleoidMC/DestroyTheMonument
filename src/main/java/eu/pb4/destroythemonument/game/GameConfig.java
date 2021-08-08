package eu.pb4.destroythemonument.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import xyz.nucleoid.codecs.MoreCodecs;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;
import eu.pb4.destroythemonument.map.MapConfig;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;

import java.util.List;

public record GameConfig(String gamemode, PlayerConfig players,
                         MapConfig map, List<GameTeam> teams,
                         boolean allowJoiningInGame, int gameTime,
                         List<Identifier> kits, int tickRespawnTime,
                         String shortName) {

    public static final Codec<GameConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("gamemode").forGetter(GameConfig::gamemode),
            PlayerConfig.CODEC.fieldOf("players").forGetter(GameConfig::players),
            MapConfig.CODEC.fieldOf("map").forGetter(GameConfig::map),
            GameTeam.CODEC.listOf().fieldOf("teams").forGetter(GameConfig::teams),
            Codec.BOOL.optionalFieldOf("allowJoiningInGame", false).forGetter(GameConfig::allowJoiningInGame),
            Codec.INT.optionalFieldOf("gameTime", -1).forGetter(GameConfig::gameTime),
            Codec.list(Identifier.CODEC).fieldOf("kits").forGetter(GameConfig::kits),
            Codec.INT.optionalFieldOf("tickRespawnTime", 60).forGetter(GameConfig::tickRespawnTime),
            Codec.STRING.fieldOf("short_name").forGetter(GameConfig::shortName)
            ).apply(instance, GameConfig::new));
}
