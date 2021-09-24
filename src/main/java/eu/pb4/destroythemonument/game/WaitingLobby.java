package eu.pb4.destroythemonument.game;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.destroythemonument.game.map.GeneratedGameMap;
import eu.pb4.destroythemonument.game.map.TemplateGameMap;
import eu.pb4.destroythemonument.game_logic.DebugGameLogic;
import eu.pb4.destroythemonument.game_logic.StandardGameLogic;
import eu.pb4.destroythemonument.items.DtmItems;
import eu.pb4.destroythemonument.kit.Kit;
import eu.pb4.destroythemonument.kit.KitsRegistry;
import eu.pb4.destroythemonument.game.map.GameMap;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.destroythemonument.ui.ClassSelectorUI;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.*;
import xyz.nucleoid.plasmid.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.game.common.team.TeamSelectionLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.List;

public class WaitingLobby {
    private final GameSpace gameSpace;
    private final GameMap map;
    private final GameConfig config;
    private final SpawnLogic spawnLogic;
    private final Teams teams;
    private final Object2ObjectMap<PlayerRef, PlayerData> participants = new Object2ObjectOpenHashMap<>();

    private final TeamSelectionLobby teamSelection;
    private final Kit defaultKit;


    private WaitingLobby(GameSpace gameSpace, GameMap map, GameConfig config, TeamSelectionLobby teamSelection) {
        this.gameSpace = gameSpace;
        this.map = map;
        this.config = config;
        this.spawnLogic = new SpawnLogic(gameSpace, map, null, null);
        this.teams = new Teams(map, config);
        this.defaultKit = KitsRegistry.get(this.config.kits().get(0));

        this.teamSelection = teamSelection;
    }

    public static GameOpenProcedure open(GameOpenContext<GameConfig> context) {
        GameConfig config = context.config();
        GameMap map;

        try {
            if (config.map().id().getNamespace().equals("generated")) {
                map = GeneratedGameMap.create(context.server(), config.map());
            } else {
                map = TemplateGameMap.create(context.server(), config.map());
            }
        } catch (Exception e) {
            throw new GameOpenException(new LiteralText("Map couldn't load! @Patbox pls fix"), e);
        }

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(map.asGenerator(context.server()))
                .setTimeOfDay(config.map().time())
                .setGameRule(GameRules.DO_DAYLIGHT_CYCLE, false);

        return context.openWithWorld(worldConfig, (game, world) -> {
            map.world = world;
            GameWaitingLobby.addTo(game, config.players());

            TeamSelectionLobby teamSelection = TeamSelectionLobby.addTo(game, config.teams());

            WaitingLobby waiting = new WaitingLobby(game.getGameSpace(), map, context.config(), teamSelection);

            game.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);
            game.listen(GamePlayerEvents.OFFER, offer -> offer.accept(world, map.getRandomSpawnPosAsVec3d()));
            game.listen(GamePlayerEvents.JOIN, waiting::addPlayer);
            game.listen(GamePlayerEvents.LEAVE, waiting::removePlayer);
            game.listen(PlayerDeathEvent.EVENT, waiting::onPlayerDeath);
            game.listen(PlayerDamageEvent.EVENT, waiting::onPlayerDamage);
            game.listen(ItemUseEvent.EVENT, waiting::onUseItem);
        });
    }

    private ActionResult onPlayerDamage(ServerPlayerEntity player, DamageSource damageSource, float v) {
        if (player.getY() < this.map.mapBounds.min().getY()) {
            this.spawnLogic.spawnPlayer(player);
        }

        return ActionResult.FAIL;
    }

    private TypedActionResult<ItemStack> onUseItem(ServerPlayerEntity player, Hand hand) {
        PlayerData playerData = this.participants.get(PlayerRef.of(player));

        if (playerData != null && player.getInventory().getMainHandStack().getItem() == DtmItems.CLASS_SELECTOR) {
            ClassSelectorUI.openSelector(player, playerData, this.config.kits());
        }

        return TypedActionResult.pass(player.getStackInHand(hand));
    }

    private GameResult requestStart() {
        Multimap<GameTeamKey, ServerPlayerEntity> playerTeams = HashMultimap.create();
        this.teamSelection.allocate(this.gameSpace.getPlayers(), playerTeams::put);
        switch (this.config.gamemode()) {
            case "standard":
                StandardGameLogic.open(this.gameSpace, this.map, this.config, playerTeams, this.participants, this.teams);
                break;
            case "debug":
                DebugGameLogic.open(this.gameSpace, this.map, this.config, playerTeams, this.participants, this.teams);
                break;
            default:
                return GameResult.error(DtmUtil.getText("message", "invalid_game_type"));
        }

        return GameResult.ok();
    }

    private void addPlayer(ServerPlayerEntity player) {
        this.participants.put(PlayerRef.of(player), new PlayerData(this.defaultKit));
        this.spawnPlayer(player);
    }

    private void removePlayer(ServerPlayerEntity player) {
        this.participants.remove(PlayerRef.of(player));
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        player.setHealth(20.0f);
        this.spawnPlayer(player);
        return ActionResult.FAIL;
    }

    private void spawnPlayer(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE, false);
        this.spawnLogic.spawnPlayer(player);
        player.getInventory().setStack(8, new ItemStack(DtmItems.CLASS_SELECTOR));
    }
}
