package eu.pb4.destroythemonument.game_logic;

import com.google.common.collect.Multimap;
import eu.pb4.destroythemonument.game.*;
import eu.pb4.destroythemonument.map.Map;
import eu.pb4.destroythemonument.other.DtmUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StandardGameLogic extends BaseGameLogic {

    public StandardGameLogic(GameSpace gameSpace, Map map, GlobalWidgets widgets, GameConfig config, Multimap<GameTeam, ServerPlayerEntity> playerTeams, Object2ObjectMap<PlayerRef, PlayerData> participants, Teams teams) {
        super(gameSpace, map, config, playerTeams, participants, teams);
        this.scoreboard = new GameScoreboard(widgets, "Destroy The Monument", this);

        List<Text> texts = new ArrayList<>();

        texts.add(new LiteralText("+--------------------------------------+").formatted(Formatting.DARK_GRAY));
        texts.add(new LiteralText("§6§l           Destroy The Monument").formatted(Formatting.GOLD));
        texts.add(DtmUtil.getText("message", "about").formatted(Formatting.WHITE));
        texts.add(new LiteralText("+--------------------------------------+").formatted(Formatting.DARK_GRAY));


        for (Text text : texts) {
            this.gameSpace.getPlayers().sendMessage(text);
        }
    }

    public static void open(GameSpace gameSpace, Map map, GameConfig config, Multimap<GameTeam, ServerPlayerEntity> playerTeams, Object2ObjectMap<PlayerRef, PlayerData> participants, Teams teams) {
        gameSpace.openGame(game -> {
            GlobalWidgets widgets = new GlobalWidgets(game);
            BaseGameLogic active = new StandardGameLogic(gameSpace, map, widgets, config, playerTeams, participants, teams);
            active.setupGame(game, gameSpace, map, config);
        });
    }

    protected void maybeEliminate(GameTeam team, TeamData regions) {
        if (regions.getMonumentCount() <= 0) {
            for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
                PlayerData dtmPlayer = this.participants.get(PlayerRef.of(player));
                if (dtmPlayer != null && dtmPlayer.team == team) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }
        }
    }

    @Override
    protected boolean checkIfShouldEnd() {
        int aliveTeams = 0;

        for (GameTeam team : this.config.teams) {
            int players = 0;

            for (PlayerData dtmPlayer : this.participants.values()) {
                if (dtmPlayer.team == team) {
                    players += 1;
                }
            }
            if (this.teams.teamData.get(team).getMonumentCount() > 0 && players > 0) {
                aliveTeams += 1;
            }
        }

        return aliveTeams <= 1;
    }

    public WinResult checkWinResult() {
        GameTeam winners = null;
        int monumentsWinner = 0;

        for (GameTeam team : this.config.teams) {
            int monuments = this.teams.teamData.get(team).getMonumentCount();
            int players = 0;

            for (PlayerData dtmPlayer : this.participants.values()) {
                if (dtmPlayer.team == team) {
                    players += 1;
                }
            }

            if (monuments > 0 && players > 0) {
                if (winners != null) {
                    if (monuments > monumentsWinner) {
                        winners = team;
                        monumentsWinner = monuments;
                    } else if (monuments == monumentsWinner) {
                        return WinResult.no();
                    }
                } else {
                    winners = team;
                    monumentsWinner = monuments;
                }
            }
        }

        return (winners != null) ? WinResult.win(winners) : WinResult.no();
    }

    @Override
    public Collection<String> getTeamScoreboards(GameTeam team, boolean compact) {
        List<String> lines = new ArrayList<>();

        int monuments = this.teams.teamData.get(team).getMonumentCount();

        if (compact) {
            lines.add(team.getFormatting().toString() + Formatting.BOLD + (monuments == 0 ? Formatting.STRIKETHROUGH.toString() : "") + team.getDisplay() +
                    Formatting.GRAY + " » " +
                    Formatting.WHITE + monuments +
                    Formatting.GRAY + "/" + Formatting.WHITE +
                    this.teams.teamData.get(team).monumentStartingCount +
                    Formatting.WHITE);
        } else {
            if (monuments != 0) {
                lines.add(team.getFormatting().toString() + Formatting.BOLD + team.getDisplay() + " Team:");
                lines.add(Formatting.GRAY + "» " +
                        Formatting.WHITE + monuments +
                        Formatting.GRAY + "/" + Formatting.WHITE +
                        this.teams.teamData.get(team).monumentStartingCount +
                        Formatting.WHITE + " left"
                );
            } else {
                lines.add(team.getFormatting().toString() + Formatting.BOLD
                        + Formatting.STRIKETHROUGH + team.getDisplay() + " Team:");
                lines.add(Formatting.GRAY + "» " +
                        Formatting.WHITE + "Eliminated!"
                );
            }
            lines.add(" ");
        }

        return lines;
    }
}
