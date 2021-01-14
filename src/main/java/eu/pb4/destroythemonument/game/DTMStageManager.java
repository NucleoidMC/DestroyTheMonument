package eu.pb4.destroythemonument.game;

import xyz.nucleoid.plasmid.game.GameSpace;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.util.Set;

public class DTMStageManager {
    private long closeTime = -1;
    private long startTime = -1;
    private boolean setSpectator = false;
    public boolean isFinished = false;


    public DTMStageManager() {

    }

    public void onOpen(long time, DTMConfig config) {

    }

    public IdleTickResult tick(long time, GameSpace space) {
        // Game has finished. Wait a few seconds before finally closing the game.
        if (this.closeTime > 0) {
            if (time >= this.closeTime) {
                return IdleTickResult.GAME_CLOSED;
            }
            return IdleTickResult.TICK_FINISHED;
        }


        // Game has just finished. Transition to the waiting-before-close state.
        if (space.getPlayers().isEmpty() || this.isFinished) {
            if (!this.setSpectator) {
                this.setSpectator = true;
                for (ServerPlayerEntity player : space.getPlayers()) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }

            this.closeTime = time + (5 * 20);

            return IdleTickResult.GAME_FINISHED;
        }

        return IdleTickResult.CONTINUE_TICK;
    }

    public enum IdleTickResult {
        CONTINUE_TICK,
        TICK_FINISHED,
        GAME_FINISHED,
        GAME_CLOSED,
    }
}
