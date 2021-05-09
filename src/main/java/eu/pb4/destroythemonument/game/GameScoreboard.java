package eu.pb4.destroythemonument.game;

import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;
import xyz.nucleoid.plasmid.widget.SidebarWidget;

import java.util.List;

public class GameScoreboard {
    private final SidebarWidget sidebar;
    private final BaseGameLogic game;
    private long ticks = 0;

    public GameScoreboard(GlobalWidgets widgets, String name, BaseGameLogic game) {
        this.sidebar = widgets.addSidebar(
                new LiteralText(name).formatted(Formatting.GOLD, Formatting.BOLD)
        );

        this.game = game;
    }

    public void tick() {
        if (this.ticks % 20 == 0) {
            this.render();
        }
        this.ticks += 1;
    }

    public void render() {
        List<GameTeam> teamList = this.game.config.teams;
        long seconds = (this.ticks / 20) % 60;
        long minutes = this.ticks / (20 * 60);

        boolean compact = teamList.size() > 4;

        this.sidebar.set(content -> {
            content.writeLine("");
            for (GameTeam team : teamList) {
                for (String line : this.game.getTeamScoreboards(team, compact)) {
                    content.writeLine(line);
                }
            }

            if (compact) {
                content.writeLine("");
            }

            content.writeLine(String.format("§7• §aTime: §f%02d:%02d", minutes, seconds));

        });
    }

}