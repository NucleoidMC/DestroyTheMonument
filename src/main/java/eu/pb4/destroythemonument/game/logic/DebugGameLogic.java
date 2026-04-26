package eu.pb4.destroythemonument.game.logic;

import com.google.common.collect.Multimap;
import eu.pb4.destroythemonument.game.*;
import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.destroythemonument.game.data.TeamData;
import eu.pb4.destroythemonument.game.map.GameMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.util.PlayerMap;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public class DebugGameLogic extends StandardGameLogic {

    public DebugGameLogic(GameSpace gameSpace, GameMap map, GameConfig config, PlayerMap<PlayerData> participants, Teams teams) {
        super(gameSpace, map, config, participants, teams);

        Component text = Component.literal("+-----------------DEBUG----------------+").withStyle(ChatFormatting.AQUA);
        this.gameSpace.getPlayers().sendMessage(text);
    }

    public static void open(GameSpace gameSpace, GameMap map, GameConfig config, Multimap<GameTeamKey, ServerPlayer> playerTeams, PlayerMap<PlayerData> participants, Teams teams) {
        gameSpace.setActivity(game -> {
            BaseGameLogic active = new DebugGameLogic(gameSpace, map, config, participants, teams);
            active.setupGame(game, map, config, playerTeams);
        });
    }

    protected void maybeEliminate(GameTeam team, TeamData regions) {

    }

    @Override
    protected boolean checkIfShouldEnd() {
        return false;
    }
}
