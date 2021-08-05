package eu.pb4.destroythemonument.game;

import com.google.common.collect.Multimap;
import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.items.DtmItems;
import eu.pb4.destroythemonument.kit.Kit;
import eu.pb4.destroythemonument.kit.KitsRegistry;
import eu.pb4.destroythemonument.map.Map;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.destroythemonument.other.FormattingUtil;
import eu.pb4.destroythemonument.ui.BlockSelectorUI;
import eu.pb4.destroythemonument.ui.ClassSelectorUI;
import eu.pb4.sidebars.api.Sidebar;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.GameMode;
import net.minecraft.world.explosion.Explosion;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.game.common.team.TeamChat;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerSet;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.block.BlockPlaceEvent;
import xyz.nucleoid.stimuli.event.block.BlockPunchEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.item.ItemThrowEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.projectile.ArrowFireEvent;
import xyz.nucleoid.stimuli.event.world.ExplosionDetonatedEvent;

import java.util.ArrayList;


public abstract class BaseGameLogic {
    public final GameConfig config;

    public final GameSpace gameSpace;
    public final Map gameMap;
    public final Object2ObjectMap<PlayerRef, PlayerData> participants;
    public final Object2IntMap<PlayerRef> deadPlayers = new Object2IntArrayMap<>();
    public final Teams teams;
    public final ArrayList<Kit> kits = new ArrayList<>();
    protected final Kit defaultKit;
    protected final SpawnLogic spawnLogic;
    protected final Sidebar globalSidebar = new Sidebar(Sidebar.Priority.MEDIUM);
    public long tickTime = 0;
    public boolean isFinished = false;
    protected TimerBar timerBar = null;
    protected long closeTime = -1;
    protected boolean setSpectator = false;

    public BaseGameLogic(GameSpace gameSpace, Map map, GameConfig config, Multimap<GameTeam, ServerPlayerEntity> playerTeams, Object2ObjectMap<PlayerRef, PlayerData> participants, Teams teams) {
        this.gameSpace = gameSpace;
        this.config = config;
        this.gameMap = map;
        this.participants = participants;
        this.spawnLogic = new SpawnLogic(gameSpace, map, participants, teams);
        this.teams = teams;
        this.defaultKit = KitsRegistry.get(this.config.kits.get(0));
        for (Identifier id : this.config.kits) {
            Kit kit = KitsRegistry.get(id);
            if (kit != null) {
                this.kits.add(kit);
            }
        }

        this.buildSidebar();
        this.globalSidebar.show();

        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            PlayerData data = this.participants.get(PlayerRef.of(player));

            if (data != null) {
                this.setPlayerSidebar(player, data);
            } else {
                this.globalSidebar.addPlayer(player);
            }
        }

        for (GameTeam team : playerTeams.keySet()) {
            for (ServerPlayerEntity player : playerTeams.get(team)) {
                this.participants.get(PlayerRef.of(player)).team = team;
                this.teams.addPlayer(player, team);
            }
        }

        DTM.ACTIVE_GAMES.put(gameSpace, this);
    }

    public void setupGame(GameActivity game, Map map, GameConfig config) {
        game.setRule(GameRuleType.CRAFTING, ActionResult.FAIL);
        game.setRule(GameRuleType.PORTALS, ActionResult.FAIL);
        game.setRule(GameRuleType.PVP, ActionResult.PASS);
        game.setRule(GameRuleType.HUNGER, ActionResult.PASS);
        game.setRule(GameRuleType.FALL_DAMAGE, ActionResult.PASS);
        game.setRule(GameRuleType.INTERACTION, ActionResult.PASS);
        game.setRule(GameRuleType.BLOCK_DROPS, ActionResult.FAIL);
        game.setRule(GameRuleType.MODIFY_ARMOR, ActionResult.FAIL);

        game.listen(GameActivityEvents.CREATE, this::onOpen);
        game.listen(GameActivityEvents.DESTROY, this::onClose);

        game.listen(GamePlayerEvents.OFFER, offer -> offer.accept(map.world, map.getRandomSpawnPosAsVec3d()));
        game.listen(GamePlayerEvents.ADD, this::addPlayer);
        game.listen(GamePlayerEvents.LEAVE, this::removePlayer);

        game.listen(GameActivityEvents.TICK, this::tick);
        game.listen(BlockBreakEvent.EVENT, this::onPlayerBreakBlock);
        game.listen(BlockPlaceEvent.BEFORE, this::onPlayerPlaceBlock);
        game.listen(BlockPunchEvent.EVENT, this::onBlockPunch);

        game.listen(ItemUseEvent.EVENT, this::onUseItem);
        game.listen(BlockUseEvent.EVENT, this::onUseBlock);

        game.listen(PlayerDamageEvent.EVENT, this::onPlayerDamage);
        game.listen(PlayerDeathEvent.EVENT, this::onPlayerDeath);
        game.listen(ExplosionDetonatedEvent.EVENT, this::onExplosion);
        game.listen(ArrowFireEvent.EVENT, this::onArrowShoot);
        game.listen(ItemThrowEvent.EVENT, this::onPlayerDropItem);

        this.teams.manager.applyTo(game);
        TeamChat.addTo(game, this.teams.manager);
    }

    private ActionResult onBlockPunch(ServerPlayerEntity player, Direction direction, BlockPos blockPos) {
        PlayerData data = this.participants.get(PlayerRef.of(player));
        if (data != null) {
            data.activeKit.updateMainTool(player, player.world.getBlockState(blockPos));
        }
        return ActionResult.PASS;
    }

    private ActionResult onPlayerDropItem(PlayerEntity player, int i, ItemStack stack) {
        if (this.participants.get(PlayerRef.of(player)) != null && stack != null) {
            if (stack.getItem() == DtmItems.MULTI_BLOCK) {
                BlockSelectorUI.openSelector((ServerPlayerEntity) player, this);
            } else if (stack.getItem() == DtmItems.CLASS_SELECTOR) {
                ClassSelectorUI.openSelector((ServerPlayerEntity) player, this);
            }
        }

        return ActionResult.FAIL;
    }

    protected ActionResult onArrowShoot(ServerPlayerEntity player, ItemStack itemStack, ArrowItem arrowItem, int i, PersistentProjectileEntity projectile) {
        projectile.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        return ActionResult.PASS;
    }

    protected void onExplosion(Explosion explosion, boolean b) {
        for (BlockPos blockPos : explosion.getAffectedBlocks()) {
            if (TagRegistry.block(DtmUtil.id("building_blocks")).contains(this.gameMap.world.getBlockState(blockPos).getBlock())) {
                this.gameMap.world.setBlockState(blockPos, Blocks.AIR.getDefaultState());
            }
        }
        explosion.clearAffectedBlocks();
    }

    protected TypedActionResult<ItemStack> onUseItem(ServerPlayerEntity player, Hand hand) {
        PlayerData playerData = this.participants.get(PlayerRef.of(player));

        ItemStack stack = player.getInventory().getMainHandStack();

        if (playerData != null && !stack.isEmpty() && stack.getItem() == DtmItems.CLASS_SELECTOR) {
            ClassSelectorUI.openSelector(player, this);
            return TypedActionResult.success(player.getStackInHand(hand));
        } else if (!stack.isEmpty() && stack.getItem() == Items.TNT) {
            stack.decrement(1);
            TntEntity tnt = new TntEntity(player.world, player.getX(), player.getY(), player.getZ(), player);

            double pitchRad = Math.toRadians(-player.getPitch());
            double yawRad = Math.toRadians(player.getYaw() - 180);

            double horizontal = Math.cos(pitchRad);
            tnt.setVelocity(new Vec3d(
                    Math.sin(yawRad) * horizontal,
                    Math.sin(pitchRad),
                    -Math.cos(yawRad) * horizontal
            ).multiply(0.8));

            player.world.spawnEntity(tnt);
        }

        return TypedActionResult.pass(player.getStackInHand(hand));
    }

    protected ActionResult onUseBlock(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
        if (this.gameMap.isTater(hitResult.getBlockPos())) {
            player.getServerWorld().spawnParticles(ParticleTypes.HEART,
                    hitResult.getBlockPos().getX() + 0.5d, hitResult.getBlockPos().getY() + 0.5d, hitResult.getBlockPos().getZ() + 0.5d,
                    5, 0.5d, 0.5d, 0.5d, 0.1d);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 99999, 0, true, false)); }

        return ActionResult.PASS;
    }

    protected void onOpen() {
        ServerWorld world = this.gameMap.world;
        for (PlayerRef ref : this.participants.keySet()) {
            ref.ifOnline(world, this::spawnParticipant);
        }
    }

    protected void onClose(GameCloseReason reason) {
        this.globalSidebar.hide();
        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            PlayerData data = this.participants.get(PlayerRef.of(player));
            if (data != null) {
                data.sidebar.removePlayer(player);
            } else {
                this.globalSidebar.removePlayer(player);
            }
        }
        if (this.timerBar != null) {
            this.timerBar.remove();
        }
    }

    protected void addPlayer(ServerPlayerEntity player) {
        if (!this.participants.containsKey(PlayerRef.of(player))) {
            if (this.config.allowJoiningInGame) {
                GameTeam team = this.teams.getSmallestTeam();
                PlayerData playerData = new PlayerData(this.defaultKit);
                playerData.team = team;
                this.participants.put(PlayerRef.of(player), playerData);
                this.teams.addPlayer(player, team);
                this.setPlayerSidebar(player, playerData);

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
            this.teams.removePlayer(player);
            if (this.timerBar != null) {
                this.timerBar.removePlayer(player);
            }
            dtmPlayer.sidebar.removePlayer(player);
        } else {
            this.globalSidebar.removePlayer(player);
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
        if (dtmPlayer != null) {
            Text deathMes = source.getDeathMessage(player);

            Text text = FormattingUtil.format(FormattingUtil.DEATH_PREFIX, FormattingUtil.DEATH_STYLE, deathMes.shallowCopy());
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
        } else {
            this.spawnLogic.spawnPlayer(player);
        }
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
                player.getInventory().setStack(x, new ItemStack(DtmItems.CLASS_SELECTOR));
            }
        } else {
            this.spawnParticipant(player);
        }
    }

    protected void tickDeadPlayers() {
        for (PlayerRef ref : this.deadPlayers.keySet()) {
            int ticksLeft = this.deadPlayers.getInt(ref);
            ticksLeft--;
            ServerPlayerEntity player = ref.getEntity(this.gameMap.world);

            if (player != null) {
                if (ticksLeft <= 0) {
                    this.deadPlayers.removeInt(ref);
                    player.networkHandler.sendPacket(new TitleS2CPacket(new LiteralText("")));
                    this.spawnParticipant(player);
                } else {
                    if ((ticksLeft + 1) % 20 == 0) {
                        player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 90, 0));
                        player.networkHandler.sendPacket(new TitleS2CPacket(DtmUtil.getText("message", "respawn_time", ticksLeft / 20 + 1).formatted(Formatting.GOLD)));
                    }
                    this.deadPlayers.replace(ref, ticksLeft);
                }
            } else {
                this.deadPlayers.removeInt(ref);
            }
        }
    }

    protected void spawnParticipant(ServerPlayerEntity player) {
        player.closeHandledScreen();
        PlayerData playerData = this.participants.get(PlayerRef.of(player));
        if (this.teams.teamData.get(playerData.team).getMonumentCount() > 0) {
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
        player.getInventory().clear();

        playerData.activeKit.equipPlayer(player, playerData.team);
        playerData.resetTimers();

        player.getInventory().setStack(8, new ItemStack(DtmItems.CLASS_SELECTOR));
    }


    protected void spawnSpectator(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
        this.spawnLogic.spawnPlayer(player);
    }

    protected ActionResult onPlayerPlaceBlock(ServerPlayerEntity player, ServerWorld world, BlockPos blockPos, BlockState state, ItemUsageContext itemUsageContext) {
        if (this.gameMap.isUnbreakable(blockPos) || !this.gameMap.mapBounds.contains(blockPos) || itemUsageContext.getStack().getItem() == Items.BEACON) {
            // Fixes desync
            int slot;
            if (itemUsageContext.getHand() == Hand.MAIN_HAND) {
                slot = player.getInventory().selectedSlot;
            } else {
                slot = 40; // offhand
            }

            player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, 0, slot, itemUsageContext.getStack()));

            return ActionResult.FAIL;
        }

        if (itemUsageContext.getStack().getItem() == Items.TNT) {
            itemUsageContext.getStack().decrement(1);
            TntEntity tnt = new TntEntity(player.world, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, player);
            player.world.spawnEntity(tnt);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    protected ActionResult onPlayerBreakBlock(ServerPlayerEntity player, ServerWorld world, BlockPos blockPos) {
        if (this.gameMap.isUnbreakable(blockPos)) {
            return ActionResult.FAIL;
        } else if (this.gameMap.isTater(blockPos)) {
            Entity entity = new LightningEntity(EntityType.LIGHTNING_BOLT, player.world);
            entity.updatePosition(player.getX(), player.getY(), player.getZ());
            player.world.spawnEntity(entity);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 6000, 2));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 6000, 2));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 6000, 2));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 6000, 2));
            player.getInventory().clear();
            return ActionResult.PASS;
        }

        PlayerData playerData = this.participants.get(PlayerRef.of(player));

        if (playerData == null) {
            return ActionResult.PASS;
        }

        if (TagRegistry.block(DtmUtil.id("building_blocks")).contains(player.world.getBlockState(blockPos).getBlock())) {
            player.giveItemStack(new ItemStack(DtmItems.MULTI_BLOCK));
            playerData.brokenPlankBlocks += 1;
            return ActionResult.SUCCESS;
        }

        playerData.brokenNonPlankBlocks += 1;

        if (playerData.brokenNonPlankBlocks % playerData.activeKit.blocksToPlanks == 0) {
            player.giveItemStack(new ItemStack(DtmItems.MULTI_BLOCK));
        }

        return ActionResult.PASS;
    }

    protected abstract void maybeEliminate(GameTeam team, TeamData regions);

    protected void tick() {
        TickType result = this.getTickType();

        for (GameTeam team : this.teams.teams.values()) {
            for (BlockPos pos : this.teams.teamData.get(team).monuments) {
                int color = team.color().getRgb();

                float blue = ((float) color % 256) / 256;
                float green = ((float) (color / 256) % 256) / 256;
                float red = ((float) color / 65536) / 256;

                this.gameSpace.getPlayers().sendPacket(new ParticleS2CPacket(new DustParticleEffect(new Vec3f(red, green, blue), 0.8f), false, (float) pos.getX() + 0.5f, (float) pos.getY() + 0.5f, (float) pos.getZ() + 0.5f, 0.2f, 0.2f, 0.2f, 0.01f, 5));
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

        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
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
            } else if (timeLeft <= 6000) {
                if (this.timerBar == null) {
                    this.timerBar = new TimerBar(this.gameMap.world.getPlayers(), timeLeft);
                }
                this.timerBar.update(timeLeft, this.config.gameTime);
            }
        }
    }

    protected abstract void setPlayerSidebar(ServerPlayerEntity player, PlayerData playerData);

    protected abstract void buildSidebar();

    protected abstract boolean checkIfShouldEnd();

    protected TickType getTickType() {
        long time = this.gameMap.world.getTime();
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
                    player.changeGameMode(GameMode.SPECTATOR);
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
            message = FormattingUtil.format(FormattingUtil.STAR_PREFIX,
                    FormattingUtil.WIN_STYLE,
                    DtmUtil.getText("message", "game_end/winner", DtmUtil.getTeamText(result.getWinningTeam())));

        } else {
            message = FormattingUtil.format(FormattingUtil.STAR_PREFIX,
                    FormattingUtil.WIN_STYLE,
                    DtmUtil.getText("message", "game_end/no_winner"));
        }

        PlayerSet players = this.gameSpace.getPlayers();
        players.sendMessage(message);
        players.playSound(SoundEvents.ENTITY_VILLAGER_YES);
    }

    public abstract WinResult checkWinResult();


    public enum TickType {
        CONTINUE_TICK,
        TICK_FINISHED,
        GAME_FINISHED,
        GAME_CLOSED,
    }

    public record WinResult(GameTeam winningTeam, boolean win) {

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
}
