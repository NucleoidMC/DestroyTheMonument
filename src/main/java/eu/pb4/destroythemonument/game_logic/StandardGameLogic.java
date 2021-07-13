package eu.pb4.destroythemonument.game_logic;

import com.google.common.collect.Multimap;
import eu.pb4.destroythemonument.game.*;
import eu.pb4.destroythemonument.map.Map;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.destroythemonument.other.FormattingUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.util.PlayerRef;


import java.util.ArrayList;
import java.util.List;

public class StandardGameLogic extends BaseGameLogic {
    public StandardGameLogic(GameSpace gameSpace, Map map, GameConfig config, Multimap<GameTeam, ServerPlayerEntity> playerTeams, Object2ObjectMap<PlayerRef, PlayerData> participants, Teams teams) {
        super(gameSpace, map, config, playerTeams, participants, teams);
        List<Text> texts = new ArrayList<>();

        texts.add(new LiteralText("+--------------------------------------+").formatted(Formatting.DARK_GRAY));
        texts.add(new LiteralText("           Destroy The Monument").formatted(Formatting.GOLD, Formatting.BOLD));
        texts.add(DtmUtil.getText("message", "about").formatted(Formatting.WHITE));
        texts.add(new LiteralText("+--------------------------------------+").formatted(Formatting.DARK_GRAY));


        for (Text text : texts) {
            this.gameSpace.getPlayers().sendMessage(text);
        }
    }

    public static void open(GameSpace gameSpace, Map map, GameConfig config, Multimap<GameTeam, ServerPlayerEntity> playerTeams, Object2ObjectMap<PlayerRef, PlayerData> participants, Teams teams) {
        gameSpace.setActivity(gameSpace.getSourceConfig(), game -> {
            BaseGameLogic active = new StandardGameLogic(gameSpace, map, config, playerTeams, participants, teams);
            active.setupGame(game, map, config);
        });
    }

    public void setupGame(GameActivity game, Map map, GameConfig config) {
        super.setupGame(game, map, config);
    }

    protected void maybeEliminate(GameTeam team, TeamData regions) {
        if (regions.getMonumentCount() <= 0) {
            for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
                PlayerData dtmPlayer = this.participants.get(PlayerRef.of(player));
                if (dtmPlayer != null && dtmPlayer.team == team) {
                    player.changeGameMode(GameMode.SPECTATOR);
                }
            }
        }
    }

    @Override
    protected ActionResult onPlayerBreakBlock(ServerPlayerEntity player, ServerWorld world, BlockPos blockPos) {
        PlayerData playerData = this.participants.get(PlayerRef.of(player));

        if (playerData != null) {
            if (this.teams.teamData.get(playerData.team).isMonument(blockPos)) {
                player.sendMessage(DtmUtil.getText("message", "cant_break_own").formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            } else {
                for (GameTeam team : this.config.teams) {
                    TeamData regions = this.teams.teamData.get(team);

                    if (regions.isMonument(blockPos)) {
                        regions.removeMonument(blockPos);

                        Text text = FormattingUtil.format(FormattingUtil.PICKAXE_PREFIX,
                                FormattingUtil.GENERAL_STYLE,
                                DtmUtil.getText("message", "monument_broken",
                                        player.getDisplayName(),
                                        DtmUtil.getText("general", "team", team.display()).formatted(team.formatting())));

                        this.gameSpace.getPlayers().sendMessage(text);
                        this.maybeEliminate(team, regions);
                        this.gameSpace.getPlayers().sendPacket(new ExplosionS2CPacket((double) blockPos.getX() + 0.5, (double) blockPos.getY() + 0.5, (double) blockPos.getZ() + 0.5, 1f, new ArrayList<>(), new Vec3d(0.0, 0.0, 0.0)));
                        playerData.brokenMonuments += 1;
                        return ActionResult.SUCCESS;
                    }
                }
            }
        }

        return super.onPlayerBreakBlock(player, world, blockPos);
    }

    @Override
    protected void setPlayerSidebar(ServerPlayerEntity player, PlayerData playerData) {
        playerData.sidebar = this.globalSidebar;
        this.globalSidebar.addPlayer(player);
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
    protected void buildSidebar() {
        List<GameTeam> teamList = this.config.teams;

        boolean compact = teamList.size() > 4;
        this.globalSidebar.setTitle(DtmUtil.getText("sidebar", "standard_title").setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true)));

        this.globalSidebar.set(b -> {

            b.add(LiteralText.EMPTY);
            for (GameTeam team : teamList) {
                if (compact) {
                    b.add((x) -> {
                                int monuments = this.teams.teamData.get(team).getMonumentCount();

                                return DtmUtil.getTeamText(team).setStyle(Style.EMPTY.withColor(team.color()).withBold(true).withStrikethrough(monuments == 0))
                                        .append(new LiteralText(" » ").setStyle(FormattingUtil.PREFIX_STYLE))
                                        .append(new LiteralText("" + monuments).formatted(Formatting.WHITE))
                                        .append(new LiteralText("/").formatted(Formatting.GRAY))
                                        .append(new LiteralText("" + this.teams.teamData.get(team).monumentStartingCount).formatted(Formatting.WHITE));
                            }
                    );
                } else {
                    b.add((x) -> {
                        int monuments = this.teams.teamData.get(team).getMonumentCount();

                        return DtmUtil.getTeamText(team).setStyle(Style.EMPTY.withColor(team.color()).withBold(true).withStrikethrough(monuments == 0));
                    });

                    b.add((x) -> {
                        int monuments = this.teams.teamData.get(team).getMonumentCount();

                        if (monuments != 0) {
                            return FormattingUtil.formatScoreboard(FormattingUtil.GENERAL_PREFIX,
                                    FormattingUtil.GENERAL_STYLE, DtmUtil.getText("sidebar", "left", new LiteralText("" + monuments)
                                            .append(new LiteralText("/").formatted(Formatting.GRAY))
                                            .append(new LiteralText("" + this.teams.teamData.get(team).monumentStartingCount))));
                        } else {
                            return FormattingUtil.formatScoreboard(FormattingUtil.GENERAL_PREFIX, FormattingUtil.GENERAL_STYLE, DtmUtil.getText("sidebar", "eliminated"));
                        }
                    });

                    b.add(LiteralText.EMPTY);
                }
            }

            if (compact) {
                b.add(new LiteralText(""));
            }

            b.add((player) -> {
                if (player != null) {
                    PlayerData data = this.participants.get(PlayerRef.of(player));

                    if (data != null) {
                        return DtmUtil.getText("sidebar", "stats",
                                new LiteralText("" + data.kills).formatted(Formatting.WHITE),
                                new LiteralText("" + data.deaths).formatted(Formatting.WHITE),
                                new LiteralText("" + data.brokenMonuments).formatted(Formatting.WHITE)
                        ).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xf2a600)));
                    }
                }
                return LiteralText.EMPTY;
            });

            b.add((player) -> {
                long seconds = (this.tickTime / 20) % 60;
                long minutes = this.tickTime / (20 * 60);

                return FormattingUtil.formatScoreboard(FormattingUtil.TIME_PREFIX,
                        DtmUtil.getText("sidebar", "time",
                                new LiteralText(String.format("%02d:%02d", minutes, seconds)).formatted(Formatting.WHITE)).formatted(Formatting.GREEN));
            });
        });
    }
}


