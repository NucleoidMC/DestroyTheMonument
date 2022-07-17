package eu.pb4.destroythemonument.game;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

public final class TimerBar {
    private final ServerBossBar bar;

    public TimerBar(Collection<ServerPlayerEntity> players, long ticksUntilEnd) {
        this.bar = new ServerBossBar(this.getText(ticksUntilEnd), BossBar.Color.YELLOW, BossBar.Style.NOTCHED_10);
        for (ServerPlayerEntity player : players) {
            this.bar.addPlayer(player);
        }
    }

    public void update(long ticksUntilEnd, long totalTicksUntilEnd) {
        if (ticksUntilEnd % 20 == 0) {
            this.bar.setName(this.getText(ticksUntilEnd));
            this.bar.setPercent((float) ticksUntilEnd / totalTicksUntilEnd);
        }
    }

    private Text getText(long ticksUntilEnd) {
        long secondsUntilEnd = ticksUntilEnd / 20;

        long minutes = secondsUntilEnd / 60;
        long seconds = secondsUntilEnd % 60;
        String time = String.format("%02d:%02d left", minutes, seconds);

        return Text.literal(time);
    }

    public void addPlayer(ServerPlayerEntity player) {
        this.bar.addPlayer(player);
    }

    public void removePlayer(ServerPlayerEntity player) {
        this.bar.removePlayer(player);
    }

    public void remove() {
        this.bar.clearPlayers();
    }
}
