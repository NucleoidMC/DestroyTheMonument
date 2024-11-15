package eu.pb4.destroythemonument.game.logic;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.entities.DtmTntEntity;
import eu.pb4.destroythemonument.game.*;
import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.destroythemonument.game.data.TeamData;
import eu.pb4.destroythemonument.game.map.GameMap;
import eu.pb4.destroythemonument.items.DtmItems;
import eu.pb4.destroythemonument.other.DtmResetable;
import eu.pb4.destroythemonument.game.playerclass.PlayerClass;
import eu.pb4.destroythemonument.game.playerclass.ClassRegistry;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.destroythemonument.other.FormattingUtil;
import eu.pb4.destroythemonument.ui.BlockSelectorUI;
import eu.pb4.destroythemonument.ui.ClassSelectorUI;
import eu.pb4.destroythemonument.ui.PlayOrSpectateUI;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.sidebars.api.Sidebar;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
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
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import net.minecraft.world.explosion.Explosion;
import org.joml.Vector3f;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceState;
import xyz.nucleoid.plasmid.api.game.common.PlayerLimiter;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamChat;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.*;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.game.stats.GameStatisticBundle;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKeys;
import xyz.nucleoid.plasmid.api.util.PlayerMap;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.block.BlockPlaceEvent;
import xyz.nucleoid.stimuli.event.block.BlockPunchEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.item.ItemThrowEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.player.PlayerS2CPacketEvent;
import xyz.nucleoid.stimuli.event.projectile.ArrowFireEvent;
import xyz.nucleoid.stimuli.event.world.ExplosionDetonatedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public abstract class BaseGameLogic {
    public final GameConfig config;

    public final GameSpace gameSpace;
    public final GameMap gameMap;
    public final PlayerMap<PlayerData> participants;
    public final Object2IntMap<PlayerRef> deadPlayers = new Object2IntArrayMap<>();
    public final Teams teams;
    public final ArrayList<PlayerClass> kits = new ArrayList<>();
    public final GameStatisticBundle statistics;
    public final PlayerClass defaultKit;
    protected final SpawnLogic spawnLogic;
    public final Sidebar globalSidebar = new Sidebar(Sidebar.Priority.MEDIUM);
    public long tickTime = 0;
    public boolean isFinished = false;
    public MapRenderer mapRenderer;
    protected TimerBar timerBar = null;
    protected long closeTime = -1;
    protected boolean setSpectator = false;

    public BaseGameLogic(GameSpace gameSpace, GameMap map, GameConfig config, PlayerMap<PlayerData> participants, Teams teams) {
        this.gameSpace = gameSpace;
        this.config = config;
        this.gameMap = map;
        this.participants = participants;
        this.spawnLogic = new SpawnLogic(gameSpace, map, participants, teams);
        this.teams = teams;
        this.statistics = gameSpace.getStatistics().bundle(DTM.ID);
        this.defaultKit = ClassRegistry.get(this.config.kits().get(0));
        this.mapRenderer = new MapRenderer(this);
        for (Identifier id : this.config.kits()) {
            PlayerClass kit = ClassRegistry.get(id);
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

        gameSpace.setAttachment(DTM.GAME_LOGIC, this);
        map.onGameStart(this);
    }

    public void setupGame(GameActivity game, GameMap map, GameConfig config, Multimap<GameTeamKey, ServerPlayerEntity> playerTeams) {
        PlayerLimiter.addTo(game, config.players().playerConfig());
        game.setRule(GameRuleType.CRAFTING, EventResult.DENY);
        game.setRule(GameRuleType.PORTALS, EventResult.DENY);
        game.setRule(GameRuleType.PVP, EventResult.PASS);
        game.setRule(GameRuleType.HUNGER, EventResult.PASS);
        game.setRule(GameRuleType.FALL_DAMAGE, EventResult.PASS);
        game.setRule(GameRuleType.INTERACTION, EventResult.PASS);
        game.setRule(GameRuleType.BLOCK_DROPS, EventResult.DENY);
        game.setRule(GameRuleType.MODIFY_ARMOR, EventResult.DENY);

        game.listen(GameActivityEvents.CREATE, this::onOpen);
        game.listen(GameActivityEvents.DESTROY, this::onClose);

        game.listen(GamePlayerEvents.ACCEPT, this::handleAccept);
        game.listen(GamePlayerEvents.ADD, this::addPlayer);
        game.listen(GamePlayerEvents.OFFER, this::handleOffer);
        game.listen(GamePlayerEvents.LEAVE, this::removePlayer);

        game.listen(GameActivityEvents.TICK, this::tick);
        game.listen(GameActivityEvents.STATE_UPDATE, this::updateState);
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

        game.listen(PlayerS2CPacketEvent.EVENT, this::onServerPacket);

        this.teams.applyTo(game);

        for (var team : playerTeams.keySet()) {
            for (ServerPlayerEntity player : playerTeams.get(team)) {
                this.participants.get(PlayerRef.of(player)).teamData = this.teams.getData(team);
                this.teams.addPlayer(player, team);
            }
        }

        TeamChat.addTo(game, this.teams.getManager());
    }

    protected EventResult onBlockPunch(ServerPlayerEntity player, Direction direction, BlockPos blockPos) {
        return EventResult.PASS;
    }

    protected EventResult onPlayerDropItem(PlayerEntity player, int i, ItemStack stack) {
        if (this.participants.get(PlayerRef.of(player)) != null && stack != null) {
            if (stack.getItem() == DtmItems.MULTI_BLOCK) {
                if (player.isSneaking()) {
                    var playerData = this.participants.get(PlayerRef.of(player));
                    var list = new ArrayList<Block>();
                    Registries.BLOCK.getOrThrow(DTM.BUILDING_BLOCKS).forEach(x -> list.add(x.value()));
                    playerData.selectedBlock = list.get((list.size() + list.indexOf(playerData.selectedBlock) + 1) % list.size());
                } else {
                    BlockSelectorUI.openSelector((ServerPlayerEntity) player, this);
                }
            } else if (stack.getItem() == DtmItems.CLASS_SELECTOR) {
                ClassSelectorUI.openSelector((ServerPlayerEntity) player, this);
            }
        }

        return EventResult.DENY;
    }

    protected EventResult onArrowShoot(ServerPlayerEntity player, ItemStack itemStack, ArrowItem arrowItem, int i, PersistentProjectileEntity projectile) {
        projectile.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        return EventResult.PASS;
    }

    protected EventResult onExplosion(Explosion explosion, List<BlockPos> destroyedBlocks) {
        for (BlockPos blockPos : destroyedBlocks) {
            if (!this.gameMap.isUnbreakable(blockPos) && !this.gameMap.isActiveMonument(blockPos)) {
                var state = this.gameMap.world.getBlockState(blockPos);
                if (state.isAir()) {
                    continue;
                }
                this.gameMap.world.setBlockState(blockPos, Blocks.AIR.getDefaultState());

                var owner = explosion.getEntity() instanceof DtmTntEntity dtmTnt ? dtmTnt.causingEntity : null;

                if (owner instanceof ServerPlayerEntity player) {
                    var data = this.participants.get(PlayerRef.of(player));

                    if (data != null) {
                        if (state.isIn(DTM.BUILDING_BLOCKS)) {
                            player.giveItemStack(new ItemStack(DtmItems.MULTI_BLOCK));
                            data.brokenPlankBlocks += 1;
                        } else {
                            if (state.calcBlockBreakingDelta(player, player.getWorld(), blockPos) < 1) {
                                data.brokenNonPlankBlocks += 1;

                                if (data.brokenNonPlankBlocks % data.activeClass.blocksToPlanks() == 0) {
                                    player.giveItemStack(new ItemStack(DtmItems.MULTI_BLOCK));
                                }
                            }
                        }
                    }
                }
            }
        }
        destroyedBlocks.clear();
        return EventResult.PASS;
    }

    protected ActionResult onUseItem(ServerPlayerEntity player, Hand hand) {
        PlayerData playerData = this.participants.get(PlayerRef.of(player));

        ItemStack stack = player.getStackInHand(hand);

        if (playerData != null && !stack.isEmpty() && stack.getItem() == DtmItems.CLASS_SELECTOR) {
            ClassSelectorUI.openSelector(player, this);
            return ActionResult.SUCCESS_SERVER;
        }

        return ActionResult.PASS;
    }

    protected ActionResult onUseBlock(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
        if (this.gameMap.isTater(hitResult.getBlockPos())) {
            player.getServerWorld().spawnParticles(ParticleTypes.HEART,
                    hitResult.getBlockPos().getX() + 0.5d, hitResult.getBlockPos().getY() + 0.5d, hitResult.getBlockPos().getZ() + 0.5d,
                    5, 0.5d, 0.5d, 0.5d, 0.1d);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 99999, 0, true, false));
        }

        return ActionResult.PASS;
    }

    private volatile boolean skipPacket = false;

    protected EventResult onServerPacket(ServerPlayerEntity player, Packet<?> packet) {
        if (skipPacket) {
            return EventResult.PASS;
        }

        var x = transformPacket(player, packet);

        if (x == packet) {
            return EventResult.PASS;
        } else {
            if (x != null) {
                skipPacket = true;
                player.networkHandler.sendPacket(x);
                skipPacket = false;
            }
            return EventResult.DENY;
        }
    }

    protected Packet<ClientPlayPacketListener> transformPacket(ServerPlayerEntity player, Packet<?> packet) {
        if (packet instanceof BundleS2CPacket bundleS2CPacket) {
            var list = new ArrayList<Packet<? super ClientPlayPacketListener>>();

            boolean needChanging = false;

            for (var x : bundleS2CPacket.getPackets()) {
                var y = transformPacket(player, x);

                if (y != null) {
                    list.add(y);
                }

                if (x != y) {
                    needChanging = true;
                }
            }

            return needChanging ? new BundleS2CPacket(list) : bundleS2CPacket;
        } else if (packet instanceof EntityEquipmentUpdateS2CPacket equipmentUpdate) {
            var list = new ArrayList<Pair<EquipmentSlot, ItemStack>>();
            boolean cancel = false;

            for (var pair : equipmentUpdate.getEquipmentList()) {
                if (pair.getSecond().getItem() == DtmItems.MAP) {
                    cancel = true;
                    list.add(new Pair<>(pair.getFirst(), ItemStack.EMPTY));
                } else {
                    list.add(pair);
                }
            }

            if (cancel) {
                return new EntityEquipmentUpdateS2CPacket(equipmentUpdate.getEntityId(), list);
            }
        } else if (packet instanceof EntityTrackerUpdateS2CPacket trackerUpdateS2CPacket && PolymerEntityUtils.getEntityContext(packet) instanceof ServerPlayerEntity target) {
            var data = this.participants.get(PlayerRef.of(player));
            var data2 = this.participants.get(PlayerRef.of(target));
            if (data != null && data2 != null && data.teamData.team != data2.teamData.team) {
                trackerUpdateS2CPacket.trackedValues().removeIf(x -> x.id() == 9);
            }
        }
        return (Packet<ClientPlayPacketListener>) packet;
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


    protected GameSpaceState.Builder updateState(GameSpaceState.Builder builder) {
        return builder.canPlay(this.config.allowJoiningInGame() && builder.canPlay());
    }

    protected JoinOfferResult handleOffer(JoinOffer joinOffer) {
        if (joinOffer.intent() == JoinIntent.PLAY && !this.config.allowJoiningInGame()) {
            return joinOffer.reject(Text.translatable("text.destroy_the_monument.cant_join_while_active"));
        }

        return joinOffer.pass();
    }

    protected JoinAcceptorResult handleAccept(JoinAcceptor offer) {
        return offer.teleport(this.gameMap.world, this.gameMap.getRandomSpawnPosAsVec3d());
    }

    protected void addPlayer(ServerPlayerEntity player) {
        if (this.gameSpace.getPlayers().spectators().contains(player)) {
            this.globalSidebar.addPlayer(player);
            this.spawnSpectator(player);
        } else {
            this.globalSidebar.removePlayer(player);
            var playerData = this.participants.computeIfAbsent(PlayerRef.of(player), (ref) -> {
                var data = new PlayerData(this.defaultKit);
                GameTeamKey team = this.teams.getSmallestTeam();
                data.teamData = this.teams.getData(team);
                this.teams.addPlayer(player, team);
                return data;
            });

            this.setPlayerSidebar(player, playerData);
            this.spawnParticipant(player);
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

    protected EventResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        PlayerData dtmPlayer = this.participants.get(PlayerRef.of(player));

        if (dtmPlayer == null || this.deadPlayers.containsKey(PlayerRef.of(player))) {
            return EventResult.DENY;
        }

        if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
            var attackerData = this.participants.get(PlayerRef.of(attacker));
            if (attackerData == null || attacker == player) {
                return EventResult.ALLOW;
            }

            if (attackerData.teamData == dtmPlayer.teamData) {
                return EventResult.DENY;
            }

            dtmPlayer.lastAttackTime = player.getWorld().getTime();
            dtmPlayer.lastAttacker = attacker;
            this.statistics.forPlayer(attacker).increment(StatisticKeys.DAMAGE_DEALT, amount);
            this.statistics.forPlayer(player).increment(StatisticKeys.DAMAGE_TAKEN, amount);
        }

        return EventResult.PASS;
    }

    protected EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        PlayerData dtmPlayer = this.participants.get(PlayerRef.of(player));
        if (dtmPlayer != null) {
            Text deathMes = source.getDeathMessage(player);

            Text text = FormattingUtil.format(FormattingUtil.DEATH_PREFIX, FormattingUtil.DEATH_STYLE, deathMes.copy());
            this.gameSpace.getPlayers().sendMessage(text);

            if (player.getWorld().getTime() - dtmPlayer.lastAttackTime <= 20 * 10 && dtmPlayer.lastAttacker != null) {
                PlayerData attacker = this.participants.get(PlayerRef.of(dtmPlayer.lastAttacker));
                attacker.kills += 1;
                attacker.addToTimers(60);
                this.statistics.forPlayer(dtmPlayer.lastAttacker).increment(StatisticKeys.KILLS, 1);
            }

            dtmPlayer.lastAttackTime = 0;
            dtmPlayer.lastAttacker = null;
            dtmPlayer.deaths += 1;

            this.statistics.forPlayer(player).increment(StatisticKeys.DEATHS, 1);
            this.gameMap.world.sendEntityStatus(player, EntityStatuses.ADD_DEATH_PARTICLES);

            this.startRespawningPlayerSequence(player);
        } else {
            this.spawnLogic.spawnPlayer(player);
        }
        return EventResult.DENY;
    }

    protected void startRespawningPlayerSequence(ServerPlayerEntity player) {
        if (this.config.tickRespawnTime() > 0) {
            this.deadPlayers.put(PlayerRef.of(player), this.config.tickRespawnTime());
            player.teleport(this.gameMap.world, player.getX(), player.getY() + 2000, player.getZ(), Set.of(), 0, 0, false);
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
                    player.networkHandler.sendPacket(new TitleS2CPacket(Text.empty()));
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

    public void spawnParticipant(ServerPlayerEntity player) {
        player.closeHandledScreen();
        PlayerData playerData = this.participants.get(PlayerRef.of(player));
        if (playerData != null && playerData.teamData.aliveMonuments.size() > 0) {
            playerData.activeClass = playerData.selectedClass;
            this.spawnLogic.resetPlayer(player, GameMode.SURVIVAL);
            this.setupPlayerClass(player, playerData);
            player.setHealth(player.getMaxHealth());
        } else {
            this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
        }
        this.spawnLogic.spawnPlayer(player);
    }


    public void setupPlayerClass(ServerPlayerEntity player, PlayerData playerData) {
        player.getInventory().clear();
        ((DtmResetable) player.getAttributes()).dtm$reset();
        playerData.activeClass.setupPlayer(player, playerData.teamData);
        this.mapRenderer.updateMap(player, playerData);

        playerData.resetTimers();

        player.getInventory().setStack(8, new ItemStack(DtmItems.CLASS_SELECTOR));
    }


    protected void spawnSpectator(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
        this.spawnLogic.spawnPlayer(player);
    }

    protected EventResult onPlayerPlaceBlock(ServerPlayerEntity player, ServerWorld world, BlockPos blockPos, BlockState state, ItemUsageContext itemUsageContext) {
        if (this.gameMap.isUnbreakable(blockPos) || !this.gameMap.mapBounds.contains(blockPos) || itemUsageContext.getStack().getItem() == Items.BEACON) {
            // Fixes desync
            int slot;
            if (itemUsageContext.getHand() == Hand.MAIN_HAND) {
                slot = player.getInventory().selectedSlot;
            } else {
                slot = 40; // offhand
            }

            player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, 0, slot, itemUsageContext.getStack()));

            return EventResult.DENY;
        }

        if (itemUsageContext.getStack().getItem() == Items.TNT) {
            itemUsageContext.getStack().decrement(1);
            TntEntity tnt = new TntEntity(player.getWorld(), blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, player);
            player.getWorld().spawnEntity(tnt);
            return EventResult.DENY;
        }

        return EventResult.PASS;
    }

    protected EventResult onPlayerBreakBlock(ServerPlayerEntity player, ServerWorld world, BlockPos blockPos) {
        if (this.gameMap.isUnbreakable(blockPos)) {
            return EventResult.DENY;
        } else if (this.gameMap.isTater(blockPos)) {
            Entity entity = new LightningEntity(EntityType.LIGHTNING_BOLT, player.getWorld());
            entity.updatePosition(player.getX(), player.getY(), player.getZ());
            player.getWorld().spawnEntity(entity);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 6000, 2));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 6000, 2));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 6000, 2));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 6000, 2));
            player.getInventory().clear();
        }

        PlayerData playerData = this.participants.get(PlayerRef.of(player));

        if (playerData == null) {
            return EventResult.PASS;
        }
        var state = player.getWorld().getBlockState(blockPos);

        if (state.isIn(DTM.BUILDING_BLOCKS)) {
            player.giveItemStack(new ItemStack(DtmItems.MULTI_BLOCK));
            playerData.brokenPlankBlocks += 1;
            return EventResult.ALLOW;
        }

        if (state.calcBlockBreakingDelta(player, world, blockPos) < 1) {
            playerData.brokenNonPlankBlocks += 1;

            if (playerData.brokenNonPlankBlocks % playerData.activeClass.blocksToPlanks() == 0) {
                player.giveItemStack(new ItemStack(DtmItems.MULTI_BLOCK));
            }
        }

        return EventResult.PASS;
    }

    protected abstract void maybeEliminate(TeamData regions);

    protected void tick() {
        TickType result = this.getTickType();

        for (var monument : this.gameMap.monuments) {
            if (monument.isAlive()) {
                int color = monument.teamData.getConfig().colors().dyeColor().getRgb();

                float blue = ((float) color % 256) / 256;
                float green = ((float) (color / 256) % 256) / 256;
                float red = ((float) color / 65536) / 256;

                this.gameSpace.getPlayers().sendPacket(new ParticleS2CPacket(new DustParticleEffect(ColorHelper.fromFloats(0, red, green, blue), 0.8f), false, monument.pos.getX() + 0.5d, monument.pos.getY() + 0.5d, monument.pos.getZ() + 0.5d, 0.2f, 0.2f, 0.2f, 0.01f, 5));
            }
        }

        this.tickDeadPlayers();
        this.onTick(result, this.tickTime);

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

        this.mapRenderer.tick();

        for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
            PlayerRef ref = PlayerRef.of(player);
            PlayerData dtmPlayer = this.participants.get(ref);
            if (dtmPlayer != null) {
                dtmPlayer.addToTimers(1);
                dtmPlayer.activeClass.maybeRestockPlayer(player, dtmPlayer);
            }

            if (!this.gameMap.mapDeathBounds.contains(player.getBlockPos()) && !this.deadPlayers.containsKey(ref)) {
                if (player.isSpectator()) {
                    this.spawnLogic.spawnPlayer(player);
                } else {
                    player.kill(this.gameMap.world);
                }
            }

            if (this.tickTime % 4 == 0 && (player.getMainHandStack().getItem() == DtmItems.MAP || player.getOffHandStack().getItem() == DtmItems.MAP)) {
                this.mapRenderer.updateMap(player, dtmPlayer);
            }
        }

        this.tickTime += 1;

        if (this.config.gameTime() > 0) {
            long timeLeft = this.config.gameTime() - this.tickTime;

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
                this.timerBar.update(timeLeft, this.config.gameTime());
            }
        }
    }

    protected abstract void onTick(TickType type, long tick);

    public abstract void setPlayerSidebar(ServerPlayerEntity player, PlayerData playerData);

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
                    DtmUtil.getText("message", "game_end/winner", DtmUtil.getTeamText(this.teams.getData(result.getWinningTeam()))));

            for (var entry : this.participants.entrySet()) {
                if (entry.getValue().teamData.team.equals(result.winningTeam)) {
                    this.statistics.forPlayer(entry.getKey()).increment(StatisticKeys.GAMES_WON, 1);
                } else {
                    this.statistics.forPlayer(entry.getKey()).increment(StatisticKeys.GAMES_LOST, 1);
                }
            }
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

    public record WinResult(GameTeamKey winningTeam, boolean win) {

        public static WinResult no() {
            return new WinResult(null, false);
        }

        public static WinResult win(GameTeamKey team) {
            return new WinResult(team, true);
        }

        public boolean isWin() {
            return this.win;
        }

        public GameTeamKey getWinningTeam() {
            return this.winningTeam;
        }
    }
}
