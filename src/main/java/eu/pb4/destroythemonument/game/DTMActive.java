package eu.pb4.destroythemonument.game;

import com.google.common.collect.Multimap;
import eu.pb4.destroythemonument.game.map.DTMTeamRegions;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.BlockState;

import net.minecraft.block.Blocks;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;

import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.*;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.player.PlayerSet;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;
import xyz.nucleoid.plasmid.util.PlayerRef;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import eu.pb4.destroythemonument.game.map.DTMMap;

import java.util.List;


public class DTMActive {
    public final DTMConfig config;

    public final GameSpace gameSpace;
    public final DTMMap gameMap;

    private final Object2ObjectMap<PlayerRef, DTMPlayer> participants;
    private final DTMSpawnLogic spawnLogic;
    private final DTMStageManager stageManager;
    public final DTMTeams teams;
    private final DTMScoreboard scoreboard;
    private DTMTimerBar timerBar = null;

    public long tickTime = 0;

    private DTMActive(GameSpace gameSpace, DTMMap map, GlobalWidgets widgets, DTMConfig config, Multimap<GameTeam, ServerPlayerEntity> players) {
        this.gameSpace = gameSpace;
        this.config = config;
        this.gameMap = map;
        this.participants = new Object2ObjectOpenHashMap<>();
        this.spawnLogic = new DTMSpawnLogic(gameSpace, map, participants);
        this.teams = gameSpace.addResource(new DTMTeams(gameSpace, map, config));


        this.scoreboard = new DTMScoreboard(widgets, "Destroy The Monument", this);

        for (GameTeam team : players.keySet()) {
            for (ServerPlayerEntity player : players.get(team)) {
                this.participants.put(PlayerRef.of(player), new DTMPlayer(team));
                this.teams.addPlayer(player, team);
            }
        }

        this.stageManager = new DTMStageManager();


        MutableText text = new LiteralText("+--------------------------------------+").formatted(Formatting.DARK_GRAY);
        MutableText text2 = new LiteralText("§6§l           Destroy The Monument").formatted(Formatting.GOLD);
        MutableText text3 = new LiteralText(" Destroy enemy's monuments, while protecting\n your own!\n You can select your class by clicking paper.").formatted(Formatting.WHITE);

        this.gameSpace.getPlayers().sendMessage(text);
        this.gameSpace.getPlayers().sendMessage(text2);
        this.gameSpace.getPlayers().sendMessage(text3);
        this.gameSpace.getPlayers().sendMessage(text);

    }

    public static void open(GameSpace gameSpace, DTMMap map, DTMConfig config, Multimap<GameTeam, ServerPlayerEntity> players) {
        gameSpace.openGame(game -> {
            GlobalWidgets widgets = new GlobalWidgets(game);
            DTMActive active = new DTMActive(gameSpace, map, widgets, config, players);

            game.setRule(GameRule.CRAFTING, RuleResult.DENY);
            game.setRule(GameRule.PORTALS, RuleResult.DENY);
            game.setRule(GameRule.PVP, RuleResult.ALLOW);
            game.setRule(GameRule.HUNGER, RuleResult.ALLOW);
            game.setRule(GameRule.FALL_DAMAGE, RuleResult.ALLOW);
            game.setRule(GameRule.INTERACTION, RuleResult.ALLOW);
            game.setRule(GameRule.BLOCK_DROPS, RuleResult.DENY);
            game.setRule(GameRule.TEAM_CHAT, RuleResult.ALLOW);
            game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);
            game.setRule(GameRule.UNSTABLE_TNT, RuleResult.ALLOW);

            game.on(GameOpenListener.EVENT, active::onOpen);
            game.on(GameCloseListener.EVENT, active::onClose);

            game.on(OfferPlayerListener.EVENT, player -> JoinResult.ok());
            game.on(PlayerAddListener.EVENT, active::addPlayer);
            game.on(PlayerRemoveListener.EVENT, active::removePlayer);

            game.on(GameTickListener.EVENT, active::tick);
            game.on(BreakBlockListener.EVENT, active::onPlayerBreakBlock);
            game.on(PlaceBlockListener.EVENT, active::onPlayerPlaceBlock);

            game.on(UseItemListener.EVENT, active::onUseItem);
            game.on(PlayerDamageListener.EVENT, active::onPlayerDamage);
            game.on(PlayerDeathListener.EVENT, active::onPlayerDeath);
            game.on(ExplosionListener.EVENT, active::onExplosion);
            game.on(PlayerFireArrowListener.EVENT, active::onArrowShoot);


        });
    }

    private ActionResult onArrowShoot(ServerPlayerEntity player, ItemStack itemStack, ArrowItem arrowItem, int i, PersistentProjectileEntity projectile) {
        projectile.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        return ActionResult.PASS;
    }

    private void onExplosion(List<BlockPos> blockPosList) {
        blockPosList.removeIf(this::isBreakableWithExplosion);
    }

    private boolean isBreakableWithExplosion(BlockPos blockPos) {
        return this.gameSpace.getWorld().getBlockState(blockPos).getBlock() != Blocks.OAK_PLANKS;
    }

    private TypedActionResult<ItemStack> onUseItem(ServerPlayerEntity player, Hand hand) {
        DTMPlayer dtmPlayer = this.participants.get(PlayerRef.of(player));

        if (dtmPlayer != null && player.inventory.getMainHandStack().getItem() == Items.PAPER) {
            DTMClassSelector.openSelector(player, dtmPlayer, this);
        }

        return TypedActionResult.pass(player.getStackInHand(hand));
    }

    private void onOpen() {
        ServerWorld world = this.gameSpace.getWorld();
        for (PlayerRef ref : this.participants.keySet()) {
            ref.ifOnline(world, this::spawnParticipant);
        }
        this.stageManager.onOpen(world.getTime(), this.config);
    }

    private void onClose() {
        this.teams.close();
        if (this.timerBar != null) {
            this.timerBar.remove();
        }
    }

    private void addPlayer(ServerPlayerEntity player) {
        if (!this.participants.containsKey(PlayerRef.of(player))) {
            if (this.config.allowJoiningInGame) {
                GameTeam team = this.teams.getSmallestTeam();
                DTMPlayer dtmPlayer = new DTMPlayer(team);
                this.participants.put(PlayerRef.of(player), dtmPlayer);
                this.teams.addPlayer(player, team);

                this.spawnParticipant(player);
            } else {
                this.spawnSpectator(player);
            }
        }
        if (this.timerBar != null) {
            this.timerBar.addPlayer(player);
        }
    }

    private void removePlayer(ServerPlayerEntity player) {
        DTMPlayer dtmPlayer = this.participants.remove(PlayerRef.of(player));
        if (dtmPlayer != null) {
            this.teams.removePlayer(player, dtmPlayer.team);
            if (this.timerBar != null) {
                this.timerBar.removePlayer(player);
            }
        }
    }

    private ActionResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        DTMPlayer dtmPlayer = this.participants.get(PlayerRef.of(player));

        if (source.getAttacker() instanceof ServerPlayerEntity) {
            dtmPlayer.lastAttackTime = player.world.getTime();
            dtmPlayer.lastAttacker = (ServerPlayerEntity) source.getAttacker();
        }

        return ActionResult.PASS;
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        DTMPlayer dtmPlayer = this.participants.get(PlayerRef.of(player));

        Text deathMes = source.getDeathMessage(player);

        Text text = new LiteralText("☠ ").formatted(Formatting.DARK_GRAY).append(deathMes.shallowCopy().formatted(Formatting.GRAY));
        this.gameSpace.getPlayers().sendMessage(text);

        if (player.world.getTime() - dtmPlayer.lastAttackTime <= 20 * 10 && dtmPlayer.lastAttacker != null) {
            this.participants.get(PlayerRef.of(dtmPlayer.lastAttacker)).kills += 1;
        }

        dtmPlayer.lastAttackTime = 0;
        dtmPlayer.lastAttacker = null;
        dtmPlayer.deaths += 1;

        this.spawnParticipant(player);
        return ActionResult.FAIL;
    }

    private void spawnParticipant(ServerPlayerEntity player) {
        DTMPlayer dtmPlayer = this.participants.get(PlayerRef.of(player));
        if (this.gameMap.teamRegions.get(dtmPlayer.team).getMonumentCount() > 0) {
            this.spawnLogic.resetPlayer(player, GameMode.SURVIVAL);
            dtmPlayer.activeKit = dtmPlayer.selectedKit;

            this.setInventory(player, dtmPlayer);
        } else {
            this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
        }
        this.spawnLogic.spawnPlayer(player);
    }


    public void setInventory(ServerPlayerEntity player, DTMPlayer dtmPlayer) {
        player.inventory.clear();

        DTMKits.equipPlayer(player, dtmPlayer);
        dtmPlayer.resetTimers();

        player.inventory.setStack(8, ItemStackBuilder.of(Items.PAPER)
                .setName(new LiteralText("Change class")
                        .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GOLD))).build());
    }





    private void spawnSpectator(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
        this.spawnLogic.spawnPlayer(player);
    }

    private ActionResult onPlayerPlaceBlock(ServerPlayerEntity player, BlockPos blockPos, BlockState blockState, ItemUsageContext itemUsageContext) {
        if (this.gameMap.isUnbreakable(blockPos) || itemUsageContext.getStack().getItem() == Items.BEACON) {
            // Fixes desync
            int slot;
            if (itemUsageContext.getHand() == Hand.MAIN_HAND) {
                slot = player.inventory.selectedSlot;
            } else {
                slot = 40; // offhand
            }

            player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, slot, itemUsageContext.getStack()));

            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    private ActionResult onPlayerBreakBlock(ServerPlayerEntity player, BlockPos blockPos) {
        DTMPlayer dtmPlayer = this.participants.get(PlayerRef.of(player));

        if (this.gameMap.isUnbreakable(blockPos)) {
            return ActionResult.FAIL;
        }

        if (this.gameMap.teamRegions.get(dtmPlayer.team).isMonument(blockPos)) {
            player.sendMessage(new LiteralText("You can't break your own monument!").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        } else {
            for (GameTeam team : this.config.teams) {
                DTMTeamRegions regions = this.gameMap.teamRegions.get(team);

                if (regions.isMonument(blockPos)) {
                    regions.removeMonument(blockPos);

                    Text text = new LiteralText("⛏ ")
                            .formatted(Formatting.GRAY)
                            .append(player.getDisplayName())
                            .append(new LiteralText(" broke ").formatted(Formatting.WHITE)
                                    .append(new LiteralText(team.getDisplay() + " Team")
                                            .formatted(team.getFormatting())))
                            .append(new LiteralText("'s Monument!").formatted(Formatting.WHITE));

                    this.gameSpace.getPlayers().sendMessage(text);

                    this.maybeEliminate(team, regions);

                    return ActionResult.SUCCESS;
                }
            }
        }

        if (player.world.getBlockState(blockPos).getBlock() == Blocks.OAK_PLANKS) {
            player.giveItemStack(new ItemStack(Items.OAK_PLANKS));
            dtmPlayer.brokenPlankBlocks += 1;
            return ActionResult.SUCCESS;
        }

        dtmPlayer.brokenNonPlankBlocks += 1;

        if (dtmPlayer.activeKit == DTMKits.Kit.CONSTRUCTOR && dtmPlayer.brokenNonPlankBlocks % 2 == 0) {
            player.giveItemStack(new ItemStack(Items.OAK_PLANKS));
        } else if (dtmPlayer.activeKit != DTMKits.Kit.CONSTRUCTOR && dtmPlayer.brokenNonPlankBlocks % 5 == 0) {
            player.giveItemStack(new ItemStack(Items.OAK_PLANKS));
        }

        return ActionResult.PASS;
    }

    private void maybeEliminate(GameTeam team, DTMTeamRegions regions) {
        if (regions.getMonumentCount() <= 0) {
            for (ServerPlayerEntity player : this.gameSpace.getPlayers() ) {
                DTMPlayer dtmPlayer = this.participants.get(PlayerRef.of(player));
                if (dtmPlayer != null && dtmPlayer.team == team) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }
        }
    }

    private void tick() {
        ServerWorld world = this.gameSpace.getWorld();
        long time = world.getTime();

        this.scoreboard.tick();

        DTMStageManager.IdleTickResult result = this.stageManager.tick(time, gameSpace);

        switch (result) {
            case CONTINUE_TICK:
                break;
            case TICK_FINISHED:
                return;
            case GAME_FINISHED:
                this.broadcastWin(this.checkWinResult());
                if (this.timerBar != null) {
                    this.timerBar.remove();
                }
                return;
            case GAME_CLOSED:
                this.gameSpace.close(GameCloseReason.FINISHED);
                return;
        }

        int aliveTeams = 0;

        for (GameTeam team : this.config.teams) {
            int players = 0;

            for (DTMPlayer dtmPlayer : this.participants.values()) {
                if (dtmPlayer.team == team) {
                    players += 1;
                }
            }
            if (this.gameMap.teamRegions.get(team).getMonumentCount() > 0 && players > 0) {
                aliveTeams += 1;
            }
        }

        if (aliveTeams <= 1) {
            this.stageManager.isFinished = true;
        }

        for (ServerPlayerEntity player : this.gameSpace.getPlayers() ) {
           DTMPlayer dtmPlayer = this.participants.get(PlayerRef.of(player));
           if (dtmPlayer != null) {
               dtmPlayer.tickTimers();
               DTMKits.tryToRestockPlayer(player, dtmPlayer);
           }

           if (!this.gameMap.mapBounds.contains(player.getBlockPos())) {
               player.kill();
           }
        }

        this.tickTime += 1;

        if (this.config.gameTime > 0) {
            long timeLeft = this.config.gameTime - this.tickTime;

            if (timeLeft <= 0) {
                if (this.timerBar != null) {
                    this.timerBar.remove();
                }
                this.timerBar = null;
                this.stageManager.isFinished = true;
            }
            else if (timeLeft <= 6000) {
                if (this.timerBar == null) {
                    this.timerBar = new DTMTimerBar(this, this.gameSpace.getWorld().getPlayers(), timeLeft);
                }
                this.timerBar.update(timeLeft, this.config.gameTime);
            }
        }
    }

    private void broadcastWin(WinResult result) {
        Text message;
        if (result.isWin()) {
            message = new LiteralText("» ").formatted(Formatting.GRAY)
                    .append(new LiteralText(result.getWinningTeam().getDisplay() + " Team").formatted(result.getWinningTeam().getFormatting()))
                    .append(new LiteralText(" has won the game!").formatted(Formatting.GOLD));

        } else {
            message = new LiteralText("» ").formatted(Formatting.GRAY)
                    .append(new LiteralText("The game ended, but nobody won!").formatted(Formatting.GOLD));
        }

        PlayerSet players = this.gameSpace.getPlayers();
        players.sendMessage(message);
        players.sendSound(SoundEvents.ENTITY_VILLAGER_YES);
    }

    private WinResult checkWinResult() {
        GameTeam winners = null;
        int monumentsWinner = 0;

        for (GameTeam team : this.config.teams) {
            int monuments = this.gameMap.teamRegions.get(team).getMonumentCount();
            int players = 0;

            for (DTMPlayer dtmPlayer : this.participants.values()) {
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

    static class WinResult {
        final GameTeam winningTeam;
        final boolean win;

        private WinResult(GameTeam winningTeam, boolean win) {
            this.winningTeam = winningTeam;
            this.win = win;
        }

        static WinResult no() {
            return new WinResult(null, false);
        }

        static WinResult win(GameTeam team) {
            return new WinResult(team, true);
        }

        public boolean isWin() {
            return this.win;
        }

        public GameTeam getWinningTeam() {
            return this.winningTeam;
        }
    }
}
