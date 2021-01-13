package eu.pb4.destroythemonument.game;

import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.plasmid.game.player.GameTeam;

public class DTMPlayer {
    public final GameTeam team;
    public DTMKits.Kit activeKit = DTMKits.Kit.WARRIOR;
    public DTMKits.Kit selectedKit = DTMKits.Kit.WARRIOR;
    public int deaths = 0;
    public int kills = 0;
    public int brokenMonuments = 0;
    public int brokenNonPlankBlocks = 0;
    public int brokenPlankBlocks = 0;

    public int baseItemTimer = 0;
    public int specialItemTimer = 0;
    public int specialPowerTimer = 0;
    public int buildBlockTimer = 0;


    public ServerPlayerEntity lastAttacker;
    public long lastAttackTime;

    public DTMPlayer(GameTeam team) {
        this.team = team;
    }

    public void resetTimers() {
        this.baseItemTimer = 0;
        this.specialItemTimer = 0;
        this.specialPowerTimer = 0;
        this.buildBlockTimer = 0;
    }

    public void tickTimers() {
        this.baseItemTimer += 1;
        this.specialItemTimer += 1;
        this.specialPowerTimer += 1;
        this.buildBlockTimer += 1;
    }
}
