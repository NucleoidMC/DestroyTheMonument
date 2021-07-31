package eu.pb4.destroythemonument.game_logic;

import com.google.common.collect.Multimap;
import eu.pb4.destroythemonument.game.*;
import eu.pb4.destroythemonument.map.Map;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class DebugGameLogic extends StandardGameLogic {

    public DebugGameLogic(GameSpace gameSpace, Map map, GameConfig config, Multimap<GameTeam, ServerPlayerEntity> playerTeams, Object2ObjectMap<PlayerRef, PlayerData> participants, Teams teams) {
        super(gameSpace, map, config, playerTeams, participants, teams);

        Text text = new LiteralText("+-----------------DEBUG----------------+").formatted(Formatting.AQUA);
        this.gameSpace.getPlayers().sendMessage(text);
    }

    public static void open(GameSpace gameSpace, Map map, GameConfig config, Multimap<GameTeam, ServerPlayerEntity> playerTeams, Object2ObjectMap<PlayerRef, PlayerData> participants, Teams teams) {
        gameSpace.setActivity(game -> {
            BaseGameLogic active = new DebugGameLogic(gameSpace, map, config, playerTeams, participants, teams);
            active.setupGame(game, map, config);
        });
    }

    protected void maybeEliminate(GameTeam team, TeamData regions) {

    }

    @Override
    protected boolean checkIfShouldEnd() {
        return false;
    }
}
