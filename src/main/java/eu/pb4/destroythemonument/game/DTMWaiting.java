package eu.pb4.destroythemonument.game;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.util.ActionResult;
import xyz.nucleoid.plasmid.game.*;
import xyz.nucleoid.plasmid.game.event.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import eu.pb4.destroythemonument.game.map.DTMMap;
import eu.pb4.destroythemonument.game.map.DTMMapBuilder;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.game.player.GameTeam;

import java.util.List;

public class DTMWaiting {
    private final GameSpace gameSpace;
    private final DTMMap map;
    private final DTMConfig config;
    private final DTMSpawnLogic spawnLogic;
    private final DTMTeams teams;

    private final TeamSelectionLobby teamSelection;


    private DTMWaiting(GameSpace gameSpace, DTMMap map, DTMConfig config, TeamSelectionLobby teamSelection) {
        this.gameSpace = gameSpace;
        this.map = map;
        this.config = config;
        this.spawnLogic = new DTMSpawnLogic(gameSpace, map, null);
        this.teams = gameSpace.addResource(new DTMTeams(gameSpace, map, config));

        this.teamSelection = teamSelection;
    }

    public static GameOpenProcedure open(GameOpenContext<DTMConfig> context) {
        DTMConfig config = context.getConfig();
        DTMMapBuilder generator = new DTMMapBuilder(config.mapConfig);
        DTMMap map = generator.create();

        BubbleWorldConfig worldConfig = new BubbleWorldConfig()
                .setGenerator(map.asGenerator(context.getServer()))
                .setDefaultGameMode(GameMode.SPECTATOR);


        return context.createOpenProcedure(worldConfig, game -> {

            GameWaitingLobby.applyTo(game, config.playerConfig);

            List<GameTeam> teams = context.getConfig().teams;
            TeamSelectionLobby teamSelection = TeamSelectionLobby.applyTo(game, teams);

            DTMWaiting waiting = new DTMWaiting(game.getSpace(), map, context.getConfig(), teamSelection);


            game.on(RequestStartListener.EVENT, waiting::requestStart);
            game.on(PlayerAddListener.EVENT, waiting::addPlayer);
            game.on(PlayerDeathListener.EVENT, waiting::onPlayerDeath);
        });
    }

    private StartResult requestStart() {
        Multimap<GameTeam, ServerPlayerEntity> players = HashMultimap.create();
        this.teamSelection.allocate(players::put);

        DTMActive.open(this.gameSpace, this.map, this.config, players, this.teams);
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
