package eu.pb4.destroythemonument.game.logic;

import com.google.common.collect.Multimap;
import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.game.GameConfig;
import eu.pb4.destroythemonument.game.Teams;
import eu.pb4.destroythemonument.game.data.Monument;
import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.destroythemonument.game.data.TeamData;
import eu.pb4.destroythemonument.game.map.GameMap;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.destroythemonument.other.FormattingUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.util.ColoredBlocks;
import xyz.nucleoid.plasmid.api.util.PlayerMap;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.stimuli.event.EventResult;

import java.util.ArrayList;
import java.util.List;

public class CaptureGameLogic extends BaseGameLogic {
    protected TeamData currentSidebarTeam = null;
    protected int sidebarTeamPos = 0;
    protected List<TeamData> sidebarTeams;

    public CaptureGameLogic(GameSpace gameSpace, GameMap map, GameConfig config, PlayerMap<PlayerData> participants, Teams teams) {
        super(gameSpace, map, config, participants, teams);
        List<Component> texts = new ArrayList<>();

        texts.add(Component.literal("+--------------------------------------+").withStyle(ChatFormatting.DARK_GRAY));
        texts.add(Component.literal("           Capture The Monument").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        texts.add(DtmUtil.getText("message", "about.capture").withStyle(ChatFormatting.WHITE));
        texts.add(Component.literal("+--------------------------------------+").withStyle(ChatFormatting.DARK_GRAY));

        for (Component text : texts) {
            this.gameSpace.getPlayers().sendMessage(text);
        }
    }

    public static void open(GameSpace gameSpace, GameMap map, GameConfig config, Multimap<GameTeamKey, ServerPlayer> playerTeams, PlayerMap<PlayerData> participants, Teams teams) {
        gameSpace.setActivity(game -> {
            BaseGameLogic active = new CaptureGameLogic(gameSpace, map, config, participants, teams);
            active.setupGame(game, map, config, playerTeams);
        });
    }

    public void setupGame(GameActivity game, GameMap map, GameConfig config, Multimap<GameTeamKey, ServerPlayer> playerTeams) {
        super.setupGame(game, map, config, playerTeams);
    }

    protected void maybeEliminate(TeamData teamData) {
        if (teamData.aliveMonuments.size() <= 0) {
            for (ServerPlayer player : this.gameSpace.getPlayers()) {
                PlayerData dtmPlayer = this.participants.get(PlayerRef.of(player));
                if (dtmPlayer != null && dtmPlayer.teamData == teamData) {
                    player.setGameMode(GameType.SPECTATOR);
                }
            }
        }
    }

    @Override
    protected EventResult onPlayerBreakBlock(ServerPlayer player, ServerLevel world, BlockPos blockPos) {
        PlayerData playerData = this.participants.get(PlayerRef.of(player));
        var monument = this.gameMap.getActiveMonument(blockPos);

        if (playerData != null && monument != null) {
            if (monument.teamData == playerData.teamData) {
                player.sendSystemMessage(DtmUtil.getText("message", "cant_capture_own").withStyle(ChatFormatting.RED), true);
                return EventResult.DENY;
            } else {
                var oldTeam = monument.teamData;
                if (oldTeam != null) {
                    oldTeam.removeMonument(monument);
                } else {
                    this.gameMap.teamLessMonuments.remove(monument);
                }

                playerData.teamData.addMonument(monument);
                monument.teamData = playerData.teamData;

                var region = this.gameMap.getMonumentRegionBounds(monument);

                if (region != null) {
                    var dye = this.teams.getConfig(playerData.teamData.team).blockDyeColor();
                    for (var pos : region) {
                        var state = this.gameMap.world.getBlockState(pos);
                        BlockState newState = state;
                        if (state.is(BlockTags.WOOL)) {
                            newState = ColoredBlocks.wool(dye).defaultBlockState();
                        } else if (state.is(BlockTags.WOOL_CARPETS)) {
                            newState = ColoredBlocks.carpet(dye).defaultBlockState();
                        } else if (state.is(BlockTags.CANDLES)) {
                            newState = ColoredBlocks.candle(dye).withPropertiesOf(state);
                        } else if (state.is(BlockTags.TERRACOTTA)) {
                            newState = ColoredBlocks.terracotta(dye).defaultBlockState();
                        } else if (DTM.CONCRETE.contains(state.getBlock())) {
                            newState = ColoredBlocks.concrete(dye).defaultBlockState();
                        } else if (DTM.STAINED_GLASS.contains(state.getBlock())) {
                            newState = ColoredBlocks.glass(dye).defaultBlockState();
                        } else if (DTM.STAINED_GLASS_PANES.contains(state.getBlock())) {
                            newState = ColoredBlocks.glassPane(dye).defaultBlockState();
                        } else if (state.getBlock() instanceof ConcretePowderBlock) {
                            newState = ColoredBlocks.concretePowder(dye).defaultBlockState();
                        }  else if (state.getBlock() instanceof BannerBlock) {
                            newState = ColoredBlocks.banner(dye).withPropertiesOf(state);
                        } else if (state.getBlock() instanceof WallBannerBlock) {
                            newState = ColoredBlocks.wallBanner(dye).withPropertiesOf(state);
                        }

                        if (state != newState) {
                            world.setBlock(pos, state, Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS);
                        }
                    }
                }

                Component text = FormattingUtil.format(FormattingUtil.PICKAXE_PREFIX,
                        FormattingUtil.GENERAL_STYLE,
                        DtmUtil.getText("message", "monument_captured" + (oldTeam != null ? ".takeover" : ""),
                                player.getDisplayName(),
                                oldTeam != null ? DtmUtil.getTeamText(oldTeam) : "",
                                monument.getName()
                        ));

                this.gameSpace.getPlayers().sendMessage(text);
                if (oldTeam != null) {
                    this.maybeEliminate(oldTeam);
                    this.teams.getManager().playersIn(oldTeam.team).playSound(SoundEvents.WITHER_SPAWN, SoundSource.MASTER, 0.6f, 1f);
                }
                //this.gameSpace.getPlayers().sendPacket(new ExplosionS2CPacket((double) blockPos.getX() + 0.5, (double) blockPos.getY() + 0.5, (double) blockPos.getZ() + 0.5, 1f, new ArrayList<>(), new Vec3d(0.0, 0.0, 0.0)));
                playerData.brokenMonuments += 1;
                playerData.addToTimers(20 * 20);
                //this.statistics.forPlayer(player).increment(DtmStatistics.MONUMENTS_DESTROYED, 1);
                return EventResult.DENY;

            }
        }

        return super.onPlayerBreakBlock(player, world, blockPos);
    }

    @Override
    public void setPlayerSidebar(ServerPlayer player, PlayerData playerData) {
        playerData.sidebar = this.globalSidebar;
        this.globalSidebar.addPlayer(player);
    }

    @Override
    protected boolean checkIfShouldEnd() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment() && this.gameSpace.getPlayers().size() == 1) {
            return false;
        }

        int aliveTeams = 0;

        for (var teamData : this.teams) {
            int players = 0;

            for (PlayerData dtmPlayer : this.participants.values()) {
                if (dtmPlayer.teamData == teamData) {
                    players += 1;
                }
            }
            if (teamData.aliveMonuments.size() > 0 && players > 0) {
                aliveTeams += 1;
            }
        }

        return aliveTeams <= 1;
    }

    public WinResult checkWinResult() {
        GameTeamKey winners = null;
        int monumentsWinner = 0;

        for (var teamData : this.teams) {
            int monuments = teamData.aliveMonuments.size();
            int players = 0;

            for (PlayerData dtmPlayer : this.participants.values()) {
                if (dtmPlayer.teamData == teamData) {
                    players += 1;
                }
            }

            if (monuments > 0 && players > 0) {
                if (winners != null) {
                    if (monuments > monumentsWinner) {
                        winners = teamData.team;
                        monumentsWinner = monuments;
                    } else if (monuments == monumentsWinner) {
                        return WinResult.no();
                    }
                } else {
                    winners = teamData.team;
                    monumentsWinner = monuments;
                }
            }
        }

        return (winners != null) ? WinResult.win(winners) : WinResult.no();
    }

    @Override
    protected void onTick(TickType type, long tick) {
        if (type == TickType.CONTINUE_TICK) {
            if (tick % 20 == 0) {
                this.buildSidebar();
            }

            /*if (tick % 60 == 0) {
                this.sidebarTeamPos++;
                if (this.sidebarTeamPos >= this.sidebarTeams.size()) {
                    this.sidebarTeamPos = 0;
                }
                this.currentSidebarTeam = this.sidebarTeams.get(this.sidebarTeamPos);
            }*/
        }
    }

    @Override
    protected void buildSidebar() {
        int monumentsSize = 0;

        /*if (this.sidebarTeams == null) {
            this.sidebarTeams = new ArrayList<>();

            for (var teamData : this.teams) {
                this.sidebarTeams.add(teamData);
            }

            this.currentSidebarTeam = this.sidebarTeams.get(0);
        }

        for (var data : this.sidebarTeams) {
            if (data.monuments.size() > monumentsSize) {
                monumentsSize = data.monuments.size();
            }
        }*/

        boolean miniCompact = false;//(monumentsSize + 2) * this.sidebarTeams.size() > 11;
        boolean compact = false;//(monumentsSize + 1) * this.sidebarTeams.size() > 11;

        this.globalSidebar.setTitle(DtmUtil.getText("sidebar", "capture_title").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)));

        this.globalSidebar.set(b -> {
            b.add(Component.empty());

            /*if (compact) {
                for (var teamData : this.sidebarTeams) {
                    b.add((x) -> generateSidebarTitleForTeam(teamData));
                    if (this.currentSidebarTeam == teamData) {
                        for (var monument : teamData.monuments) {
                            b.add((x) -> generateSidebarTitleForMonument(monument));
                        }
                    }
                }
            } else {
                for (var teamData : this.sidebarTeams) {
                    b.add((x) -> generateSidebarTitleForTeam(teamData));
                    for (var monument : teamData.monuments) {
                        //b.add((x) -> generateSidebarTitleForMonument(monument));
                    }
                    if (!miniCompact) {
                        b.add(Text.empty());
                    }
                }
            }*/

            for (var monument : this.gameMap.monuments) {
                b.add((x) -> generateSidebarTitleForMonument(monument));
            }

            b.add(Component.empty());
            b.add((player) -> {
                if (player != null) {
                    PlayerData data = this.participants.get(PlayerRef.of(player));

                    if (data != null) {
                        return DtmUtil.getText("sidebar", "stats",
                                Component.literal("" + data.kills).withStyle(ChatFormatting.WHITE),
                                Component.literal("" + data.deaths).withStyle(ChatFormatting.WHITE),
                                Component.literal("" + data.brokenMonuments).withStyle(ChatFormatting.WHITE)
                        ).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xf2a600)));
                    }
                }
                return Component.empty();
            });

            b.add((player) -> {
                long seconds = (this.tickTime / 20) % 60;
                long minutes = this.tickTime / (20 * 60);

                return FormattingUtil.formatScoreboard(FormattingUtil.TIME_PREFIX,
                        DtmUtil.getText("sidebar", "time",
                                Component.literal(String.format("%02d:%02d", minutes, seconds)).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GREEN));
            });
        });
    }

    private Component generateSidebarTitleForTeam(TeamData data) {
        if (data == null) {
            return Component.empty();
        }

        int monuments = data.aliveMonuments.size();
        return Component.literal("").append(DtmUtil.getTeamText(data).setStyle(Style.EMPTY.withColor(data.getConfig().chatFormatting()).withBold(true).withStrikethrough(monuments == 0)))
                .append(Component.literal(" (").setStyle(FormattingUtil.PREFIX_STYLE))
                .append(Component.literal("" + monuments).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("/").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("" + data.monumentStartingCount).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(")").setStyle(FormattingUtil.PREFIX_STYLE));
    }

    private Component generateSidebarTitleForMonument(Monument monument) {
        return Component.literal("").append(Component.literal("» ").setStyle(FormattingUtil.PREFIX_SCOREBOARD_STYLE))
                .append(monument.getName())
                .setStyle(Style.EMPTY.withColor(monument.teamData != null ? monument.teamData.getConfig().chatFormatting() : ChatFormatting.WHITE));
    }
}


