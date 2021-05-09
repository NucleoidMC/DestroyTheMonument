package eu.pb4.destroythemonument.game;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import eu.pb4.destroythemonument.game_logic.StandardGameLogic;
import eu.pb4.destroythemonument.other.DtmUtil;
import net.minecraft.util.ActionResult;
import xyz.nucleoid.plasmid.game.*;
import xyz.nucleoid.plasmid.game.event.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import eu.pb4.destroythemonument.map.Map;
import eu.pb4.destroythemonument.map.MapBuilder;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.game.player.GameTeam;

import java.util.List;

public class WaitingLobby {
    private final GameSpace gameSpace;
    private final Map map;
    private final GameConfig config;
    private final SpawnLogic spawnLogic;
    private final Teams teams;

    private final TeamSelectionLobby teamSelection;


    private WaitingLobby(GameSpace gameSpace, Map map, GameConfig config, TeamSelectionLobby teamSelection) {
        this.gameSpace = gameSpace;
        this.map = map;
        this.config = config;
        this.spawnLogic = new SpawnLogic(gameSpace, map, null);
        this.teams = gameSpace.addResource(new Teams(gameSpace, map, config));

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
            game.on(PlayerDeathListener.EVENT, waiting::onPlayerDeath);
        });
    }

    private StartResult requestStart() {
        Multimap<GameTeam, ServerPlayerEntity> players = HashMultimap.create();
        this.teamSelection.allocate(players::put);

        switch (this.config.gamemode) {
            case "standard":
                StandardGameLogic.open(this.gameSpace, this.map, this.config, players, this.teams);
                break;
            default:
                return StartResult.error(DtmUtil.getText("message", "invalid_game_type"));
        }

        return StartResult.OK;
    }

    private void addPlayer(ServerPlayerEntity player) {
        this.spawnPlayer(player);
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        player.setHealth(20.0f);
        this.spawnPlayer(player);
        return ActionResult.FAIL;
    }

    private void spawnPlayer(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        this.spawnLogic.spawnPlayer(player);
    }
}
