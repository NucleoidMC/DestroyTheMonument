package eu.pb4.destroythemonument.game.data;

import eu.pb4.destroythemonument.game.playerclass.PlayerClass;
import eu.pb4.sidebars.api.Sidebar;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class PlayerData {
    public TeamData teamData = null;
    public PlayerClass activeClass;
    public PlayerClass selectedClass;
    public int deaths = 0;
    public int kills = 0;
    public int brokenMonuments = 0;
    public int brokenNonPlankBlocks = 0;
    public int brokenPlankBlocks = 0;

    public Object2IntArrayMap<PlayerClass.RestockableItem> restockTimers = new Object2IntArrayMap<>();

    public ServerPlayerEntity lastAttacker;
    public long lastAttackTime;
    public Block selectedBlock = Blocks.OAK_PLANKS;
    public Sidebar sidebar;
    public BlockPos nextSpawnPos;

    public PlayerData(PlayerClass defaultKit) {
        this.selectedClass = defaultKit;
        this.activeClass = defaultKit;
    }

    public void resetTimers() {
        for (PlayerClass.RestockableItem key : this.activeClass.restockableItems()) {
            this.restockTimers.putIfAbsent(key, key.startingOffset);
        }
    }

    public void addToTimers(int x) {
        for (PlayerClass.RestockableItem key : this.activeClass.restockableItems()) {
            this.restockTimers.put(key, this.restockTimers.getInt(key) + x);
        }
    }
}
