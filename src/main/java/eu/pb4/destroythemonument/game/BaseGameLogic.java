package eu.pb4.destroythemonument.game;

import com.google.common.collect.Multimap;
import eu.pb4.destroythemonument.map.TeamRegions;
import eu.pb4.destroythemonument.kit.Kit;
import eu.pb4.destroythemonument.kit.KitsRegistry;
import eu.pb4.destroythemonument.other.ClassSelectorUI;
import eu.pb4.destroythemonument.other.DtmUtil;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.BlockState;

import net.minecraft.block.Blocks;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;

import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.text.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameLogic;
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
import net.minecraft.world.GameMode;
import eu.pb4.destroythemonument.map.Map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public abstract class BaseGameLogic {
    public final GameConfig config;

    public final GameSpace gameSpace;
    public final Map gameMap;
    protected final Kit defaultKit;

    public final Object2ObjectMap<PlayerRef, PlayerData> participants;
    public final Object2IntMap<PlayerRef> deadPlayers = new Object2IntArrayMap<>();

    protected final SpawnLogic spawnLogic;
    public final Teams teams;
    protected final GameScoreboard scoreboard;
    protected TimerBar timerBar = null;
    public final ArrayList<Kit> kits = new ArrayList<>();

    public long tickTime = 0;

    protected long closeTime = -1;
    protected boolean setSpectator = false;
    public boolean isFinished = false;

    public BaseGameLogic(GameSpace gameSpace, Map map, GlobalWidgets widgets, GameConfig config, Multimap<GameTeam, ServerPlayerEntity> players, Teams teams) {
        this.gameSpace = gameSpace;
        this.config = config;
        this.gameMap = map;
        this.participants = new Object2ObjectOpenHashMap<>();
        this.spawnLogic = new SpawnLogic(gameSpace, map, participants);
        this.teams = teams;
        this.defaultKit = KitsRegistry.get(this.config.kits.get(0));
        for (Identifier id : this.config.kits) {
            Kit kit = KitsRegistry.get(id);
            if (kit != null) {
                this.kits.add(kit);
            }
        }

        this.scoreboard = new GameScoreboard(widgets, "Destroy The Monument", this);

        for (GameTeam team : players.keySet()) {
            for (ServerPlayerEntity player : players.get(team)) {
                this.participants.put(PlayerRef.of(player), new PlayerData(team, this.defaultKit));
                this.teams.addPlayer(player, team);
            }
        }
    }

    public void setupGame(GameLogic game, GameSpace gameSpace, Map map, GameConfig config) {
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

        game.on(GameOpenListener.EVENT, this::onOpen);
        game.on(GameCloseListener.EVENT, this::onClose);

        game.on(OfferPlayerListener.EVENT, player -> JoinResult.ok());
        game.on(PlayerAddListener.EVENT, this::addPlayer);
        game.on(PlayerRemoveListener.EVENT, this::removePlayer);

        game.on(GameTickListener.EVENT, this::tick);
        game.on(BreakBlockListener.EVENT, this::onPlayerBreakBlock);
        game.on(PlaceBlockListener.EVENT, this::onPlayerPlaceBlock);

        game.on(UseItemListener.EVENT, this::onUseItem);
        game.on(PlayerDamageListener.EVENT, this::onPlayerDamage);
        game.on(PlayerDeathListener.EVENT, this::onPlayerDeath);
        game.on(ExplosionListener.EVENT, this::onExplosion);
        game.on(PlayerFireArrowListener.EVENT, this::onArrowShoot);
    }

    protected ActionResult onArrowShoot(ServerPlayerEntity player, ItemStack itemStack, ArrowItem arrowItem, int i, PersistentProjectileEntity projectile) {
        projectile.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        return ActionResult.PASS;
    }

    protected void onExplosion(List<BlockPos> blockPosList) {
        blockPosList.removeIf(this::isBreakableWithExplosion);
    }

    protected boolean isBreakableWithExplosion(BlockPos blockPos) {
        return this.gameSpace.getWorld().getBlockState(blockPos).getBlock() != Blocks.OAK_PLANKS;
    }

    protected TypedActionResult<ItemStack> onUseItem(ServerPlayerEntity player, Hand hand) {
        PlayerData dtmPlayer = this.participants.get(PlayerRef.of(player));

        if (dtmPlayer != null && player.inventory.getMainHandStack().getItem() == Items.PAPER) {
            ClassSelectorUI.openSelector(player, this);
        }

        return TypedActionResult.pass(player.getStackInHand(hand));
    }

    protected void onOpen() {
        ServerWorld world = this.gameSpace.getWorld();
        for (PlayerRef ref : this.participants.keySet()) {
            ref.ifOnline(world, this::spawnParticipant);
        }
    }

    protected void onClose() {
        this.teams.close();
        if (this.timerBar != null) {
            this.timerBar.remove();
        }
    }

    protected void addPlayer(ServerPlayerEntity player) {
        if (!this.participants.containsKey(PlayerRef.of(player))) {
            if (this.config.allowJoiningInGame) {
                GameTeam team = this.teams.getSmallestTeam();
                PlayerData dtmPlayer = new PlayerData(team, this.defaultKit);
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

    protected void removePlayer(ServerPlayerEntity player) {
        PlayerData dtmPlayer = this.participants.remove(PlayerRef.of(player));
        if (dtmPlayer != null) {
            this.teams.removePlayer(player, dtmPlayer.team);
            if (this.timerBar != null) {
                this.timerBar.removePlayer(player);
            }
        }
    }

    protected ActionResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        PlayerData dtmPlayer = this.participants.get(PlayerRef.of(player));

        if (source.getAttacker() instanceof ServerPlayerEntity) {
            dtmPlayer.lastAttackTime = player.world.getTime();
            dtmPlayer.lastAttacker = (ServerPlayerEntity) source.getAttacker();
        }

        return ActionResult.PASS;
    }

    protected ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        PlayerData dtmPlayer = this.participants.get(PlayerRef.of(player));

        Text deathMes = source.getDeathMessage(player);

        Text text = DtmUtil.getFormatted("☠", deathMes.shallowCopy().setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xbfbfbf))));
        this.gameSpace.getPlayers().sendMessage(text);

        if (player.world.getTime() - dtmPlayer.lastAttackTime <= 20 * 10 && dtmPlayer.lastAttacker != null) {
            PlayerData attacker = this.participants.get(PlayerRef.of(dtmPlayer.lastAttacker));
            attacker.kills += 1;
            attacker.addToTimers(60);

        }

        dtmPlayer.lastAttackTime = 0;
        dtmPlayer.lastAttacker = null;
        dtmPlayer.deaths += 1;

        this.startRespawningPlayerSequence(player);
        return ActionResult.FAIL;
    }

    protected void startRespawningPlayerSequence(ServerPlayerEntity player) {
        if (this.config.tickRespawnTime > 0) {
            this.deadPlayers.put(PlayerRef.of(player), this.config.tickRespawnTime);
            player.teleport(player.getX(), player.getY() + 1000, player.getZ());
            this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
            player.networkHandler.sendPacket(new GameStateChangeS2CPacket(new GameStateChangeS2CPacket.Reason(3), 3));
            PlayerAbilities abilities = new PlayerAbilities();
            abilities.allowFlying = false;
            player.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(abilities));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 120, 1, true, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 120, 10, true, false));
            for (int x = 0; x < 9; x++) {
                player.inventory.setStack(x, new ItemStack(Items.PAPER));
            }
        } else {
            this.spawnParticipant(player);
        }
    }

    protected void tickDeadPlayers() {
        for (PlayerRef ref : this.deadPlayers.keySet()) {
            int ticksLeft = this.deadPlayers.getInt(ref);
            ticksLeft--;
            ServerPlayerEntity player = ref.getEntity(this.gameSpace.getWorld());

            if (player != null) {
                if (ticksLeft <= 0) {
                    this.deadPlayers.removeInt(ref);
                    player.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.TITLE, new LiteralText("")));
                    this.spawnParticipant(player);
                } else {
                    if ((ticksLeft + 1) % 20 == 0) {
                        player.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.TIMES,null,0, 90, 0));
                        player.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.TITLE,
                                DtmUtil.getText("message", "respawn_time", ticksLeft / 20 + 1).formatted(Formatting.GOLD)));
                    }
                    this.deadPlayers.replace(ref, ticksLeft);
                }
            } else {
                this.deadPlayers.removeInt(ref);
            }
        }
    }

    protected void spawnParticipant(ServerPlayerEntity player) {
        PlayerData playerData = this.participants.get(PlayerRef.of(player));
        if (this.gameMap.teamRegions.get(playerData.team).getMonumentCount() > 0) {
            playerData.activeKit = playerData.selectedKit;
            player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(playerData.activeKit.health);
            this.spawnLogic.resetPlayer(player, GameMode.SURVIVAL);

            this.setInventory(player, playerData);
        } else {
            this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
        }
        this.spawnLogic.spawnPlayer(player);
    }


    public void setInventory(ServerPlayerEntity player, PlayerData playerData) {
        player.inventory.clear();

        playerData.activeKit.equipPlayer(player, playerData.team);
        playerData.resetTimers();

        player.inventory.setStack(8, ItemStackBuilder.of(Items.PAPER)
                .setName(DtmUtil.getText("item", "change_class")
                        .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GOLD))).build());
    }



    protected void spawnSpectator(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
        this.spawnLogic.spawnPlayer(player);
    }

    protected ActionResult onPlayerPlaceBlock(ServerPlayerEntity player, BlockPos blockPos, BlockState blockState, ItemUsageContext itemUsageContext) {
        if (this.gameMap.isUnbreakable(blockPos) || !this.gameMap.mapBounds.contains(blockPos) || itemUsageContext.getStack().getItem() == Items.BEACON) {
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

    protected ActionResult onPlayerBreakBlock(ServerPlayerEntity player, BlockPos blockPos) {
        PlayerData dtmPlayer = this.participants.get(PlayerRef.of(player));

        if (this.gameMap.isUnbreakable(blockPos)) {
            return ActionResult.FAIL;
        }

        if (this.gameMap.teamRegions.get(dtmPlayer.team).isMonument(blockPos)) {
            player.sendMessage(DtmUtil.getText("message", "cant_break_own").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        } else {
            for (GameTeam team : this.config.teams) {
                TeamRegions regions = this.gameMap.teamRegions.get(team);

                if (regions.isMonument(blockPos)) {
                    regions.removeMonument(blockPos);

                    Text text = DtmUtil.getFormatted("⛏",
                            DtmUtil.getText("message", "monument_broken",
                                    player.getDisplayName(),
                                    DtmUtil.getText("general", "team", team.getDisplay()).formatted(team.getFormatting())).formatted(Formatting.WHITE));

                    this.gameSpace.getPlayers().sendMessage(text);
                    this.maybeEliminate(team, regions);
                    this.gameSpace.getPlayers().sendPacket(new ExplosionS2CPacket((double) blockPos.getX() + 0.5, (double) blockPos.getY() + 0.5, (double) blockPos.getZ() + 0.5, 1f, new ArrayList<>(), new Vec3d(0.0, 0.0, 0.0)));
                    dtmPlayer.brokenMonuments += 1;
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

        if (dtmPlayer.brokenNonPlankBlocks % dtmPlayer.activeKit.blocksToPlanks == 0) {
            player.giveItemStack(new ItemStack(Items.OAK_PLANKS));
        }

        return ActionResult.PASS;
    }

    protected abstract void maybeEliminate(GameTeam team, TeamRegions regions);

    protected void tick() {
        this.scoreboard.tick();

        TickType result = this.getTickType();

        for (GameTeam team : this.teams.teams.values()) {
            for (BlockPos pos : this.gameMap.teamRegions.get(team).monuments) {
                int color = team.getColor();

                float blue = ((float) color % 256) / 256;
                float green = ((float) (color / 256) % 256) / 256;
                float red = ((float) color / 65536) / 256;

                this.gameSpace.getPlayers().sendPacket(new ParticleS2CPacket(new DustParticleEffect(red, green, blue, 0.8f), false, (float) pos.getX() + 0.5f, (float) pos.getY() + 0.5f, (float) pos.getZ() + 0.5f, 0.2f, 0.2f, 0.2f, 0.01f, 5));
            }
        }
        this.tickDeadPlayers();

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

        if (this.checkIfShouldEnd()) {
            this.isFinished = true;
        }

        for (ServerPlayerEntity player : this.gameSpace.getPlayers() ) {
            PlayerRef ref = PlayerRef.of(player);
            PlayerData dtmPlayer = this.participants.get(ref);
            if (dtmPlayer != null) {
                dtmPlayer.addToTimers(1);
                dtmPlayer.activeKit.maybeRestockPlayer(player, dtmPlayer);
            }

            if (!this.gameMap.mapDeathBounds.contains(player.getBlockPos()) && !this.deadPlayers.containsKey(ref)) {
                if (player.isSpectator()) {
                    this.spawnLogic.spawnPlayer(player);
                } else {
                    player.kill();
                }
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
                this.isFinished = true;
            }
            else if (timeLeft <= 6000) {
                if (this.timerBar == null) {
                    this.timerBar = new TimerBar(this.gameSpace.getWorld().getPlayers(), timeLeft);
                }
                this.timerBar.update(timeLeft, this.config.gameTime);
            }
        }
    }

    protected abstract boolean checkIfShouldEnd();

    protected TickType getTickType() {
        long time = this.gameSpace.getWorld().getTime();
        if (this.closeTime > 0) {
            if (time >= this.closeTime) {
                return TickType.GAME_CLOSED;
            }
            return TickType.TICK_FINISHED;
        }


        // Game has just finished. Transition to the waiting-before-close state.
        if (this.gameSpace.getPlayers().isEmpty() || this.isFinished) {
            if (!this.setSpectator) {
                this.setSpectator = true;
                for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
                this.deadPlayers.clear();
            }

            this.closeTime = time + (5 * 20);

            return TickType.GAME_FINISHED;
        }

        return TickType.CONTINUE_TICK;
    }

    protected void broadcastWin(WinResult result) {
        Text message;
        if (result.isWin()) {
            message = DtmUtil.getFormatted("»",
                    DtmUtil.getText("message", "game_end/winner", DtmUtil.getTeamText(result.getWinningTeam())).formatted(Formatting.GOLD));

        } else {
            message = DtmUtil.getFormatted("»",
                    DtmUtil.getText("message", "game_end/no_winner", DtmUtil.getTeamText(result.getWinningTeam())).formatted(Formatting.GOLD));
        }

        PlayerSet players = this.gameSpace.getPlayers();
        players.sendMessage(message);
        players.sendSound(SoundEvents.ENTITY_VILLAGER_YES);
    }

    public abstract WinResult checkWinResult();
    public abstract Collection<String> getTeamScoreboards(GameTeam team, boolean compact);

    public static class WinResult {
        final GameTeam winningTeam;
        final boolean win;

        protected WinResult(GameTeam winningTeam, boolean win) {
            this.winningTeam = winningTeam;
            this.win = win;
        }

        public static WinResult no() {
            return new WinResult(null, false);
        }

        public static WinResult win(GameTeam team) {
            return new WinResult(team, true);
        }

        public boolean isWin() {
            return this.win;
        }

        public GameTeam getWinningTeam() {
            return this.winningTeam;
        }
    }

    public enum TickType {
        CONTINUE_TICK,
        TICK_FINISHED,
        GAME_FINISHED,
        GAME_CLOSED,
    }
}
