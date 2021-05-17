package eu.pb4.destroythemonument.game;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import eu.pb4.destroythemonument.game_logic.StandardGameLogic;
import eu.pb4.destroythemonument.kit.Kit;
import eu.pb4.destroythemonument.kit.KitsRegistry;
import eu.pb4.destroythemonument.other.ClassSelectorUI;
import eu.pb4.destroythemonument.other.DtmItems;
import eu.pb4.destroythemonument.other.DtmUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import xyz.nucleoid.plasmid.game.*;
import xyz.nucleoid.plasmid.game.event.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import eu.pb4.destroythemonument.map.Map;
import eu.pb4.destroythemonument.map.MapBuilder;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.PlayerRef;

import java.util.List;

public class WaitingLobby {
    private final GameSpace gameSpace;
    private final Map map;
    private final GameConfig config;
    private final SpawnLogic spawnLogic;
    private final Teams teams;
    private final Object2ObjectMap<PlayerRef, PlayerData> participants = new Object2ObjectOpenHashMap<>();

    private final TeamSelectionLobby teamSelection;
    private final Kit defaultKit;


    private WaitingLobby(GameSpace gameSpace, Map map, GameConfig config, TeamSelectionLobby teamSelection) {
        this.gameSpace = gameSpace;
        this.map = map;
        this.config = config;
        this.spawnLogic = new SpawnLogic(gameSpace, map, null);
        this.teams = gameSpace.addResource(new Teams(gameSpace, map, config));
        this.defaultKit = KitsRegistry.get(this.config.kits.get(0));

        this.teamSelection = teamSelection;
    }

    public static GameOpenProcedure open(GameOpenContext<GameConfig> context) {
        GameConfig config = context.getConfig();
        MapBuilder generator = new MapBuilder(config.mapConfig);
        Map map = generator.create();

        BubbleWorldConfig worldConfig = new BubbleWorldConfig()
                .setGenerator(map.asGenerator(context.getServer()))
                .setDefaultGameMode(GameMode.SPECTATOR);


        return context.createOpenProcedure(worldConfig, game -> {

            GameWaitingLobby.applyTo(game, config.playerConfig);

            List<GameTeam> teams = context.getConfig().teams;
            TeamSelectionLobby teamSelection = TeamSelectionLobby.applyTo(game, teams);

            WaitingLobby waiting = new WaitingLobby(game.getSpace(), map, context.getConfig(), teamSelection);


            game.on(RequestStartListener.EVENT, waiting::requestStart);
            game.on(PlayerAddListener.EVENT, waiting::addPlayer);
            game.on(PlayerRemoveListener.EVENT, waiting::removePlayer);
            game.on(PlayerDeathListener.EVENT, waiting::onPlayerDeath);
            game.on(UseItemListener.EVENT, waiting::onUseItem);
        });
    }

    private TypedActionResult<ItemStack> onUseItem(ServerPlayerEntity player, Hand hand) {
        PlayerData playerData = this.participants.get(PlayerRef.of(player));

        if (playerData != null && player.inventory.getMainHandStack().getItem() == DtmItems.CLASS_SELECTOR) {
            ClassSelectorUI.openSelector(player, playerData, this.config.kits);
        }

        return TypedActionResult.pass(player.getStackInHand(hand));
    }

    private StartResult requestStart() {
        Multimap<GameTeam, ServerPlayerEntity> playerTeams = HashMultimap.create();
        this.teamSelection.allocate(playerTeams::put);

        switch (this.config.gamemode) {
            case "standard":
                StandardGameLogic.open(this.gameSpace, this.map, this.config, playerTeams, this.participants, this.teams);
                break;
            default:
                return StartResult.error(DtmUtil.getText("message", "invalid_game_type"));
        }

        return StartResult.OK;
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
        player.inventory.setStack(8, new ItemStack(DtmItems.CLASS_SELECTOR));
    }
}
