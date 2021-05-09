package eu.pb4.destroythemonument.game;

import eu.pb4.destroythemonument.kit.Kit;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.plasmid.game.player.GameTeam;

public class PlayerData {
    public final GameTeam team;
    public Kit activeKit;
    public Kit selectedKit;
    public int deaths = 0;
    public int kills = 0;
    public int brokenMonuments = 0;
    public int brokenNonPlankBlocks = 0;
    public int brokenPlankBlocks = 0;

    public int foodItemTimer = 0;
    public int additionalItemTimer = 0;
    public int additionalPowerTimer = 0;
    public int buildBlockTimer = 0;


    public ServerPlayerEntity lastAttacker;
    public long lastAttackTime;

    public PlayerData(GameTeam team, Kit defaultKit) {
        this.selectedKit = defaultKit;
        this.activeKit = defaultKit;
        this.team = team;
    }

    public void resetTimers() {
        this.foodItemTimer = 0;
        this.additionalItemTimer = 0;
        this.additionalPowerTimer = 0;
        this.buildBlockTimer = 0;
    }

    public void addToTimers(int x) {
        this.foodItemTimer += x;
        this.additionalItemTimer += x;
        this.additionalPowerTimer += x;
        this.buildBlockTimer += x;
    }
}
