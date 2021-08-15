package eu.pb4.destroythemonument.game.data;

import eu.pb4.destroythemonument.kit.Kit;
import eu.pb4.sidebars.api.Sidebar;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;

public class PlayerData {
    public GameTeam team = null;
    public Kit activeKit;
    public Kit selectedKit;
    public int deaths = 0;
    public int kills = 0;
    public int brokenMonuments = 0;
    public int brokenNonPlankBlocks = 0;
    public int brokenPlankBlocks = 0;

    public Object2IntArrayMap<Kit.RestockableItem> restockTimers = new Object2IntArrayMap<>();

    public ServerPlayerEntity lastAttacker;
    public long lastAttackTime;
    public Block selectedBlock = Blocks.OAK_PLANKS;
    public Sidebar sidebar;
    public BlockPos nextSpawnPos;

    public PlayerData(Kit defaultKit) {
        this.selectedKit = defaultKit;
        this.activeKit = defaultKit;
    }

    public void resetTimers() {
        for (Kit.RestockableItem key : this.activeKit.restockableItems) {
            this.restockTimers.putIfAbsent(key, key.startingOffset);
        }
    }

    public void addToTimers(int x) {
        for (Kit.RestockableItem key : this.activeKit.restockableItems) {
            this.restockTimers.put(key, this.restockTimers.getInt(key) + x);
        }
    }
}
