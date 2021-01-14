package eu.pb4.destroythemonument.game;

import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;
import xyz.nucleoid.plasmid.widget.SidebarWidget;

import java.util.List;

public class DTMScoreboard {
    private final SidebarWidget sidebar;
    private final DTMActive game;
    private long ticks = 0;

    public DTMScoreboard(GlobalWidgets widgets, String name, DTMActive game) {
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

        this.sidebar.set(content -> {
            content.writeLine("");
            for (GameTeam team : teamList) {
                int monuments = this.game.gameMap.teamRegions.get(team).getMonumentCount();

                if (monuments != 0) {
                    content.writeLine(team.getFormatting().toString() + Formatting.BOLD.toString() + team.getDisplay() + " Team:");
                    content.writeLine(Formatting.GRAY.toString() + "» " +
                            Formatting.WHITE.toString() + monuments +
                            Formatting.GRAY.toString() + "/" + Formatting.WHITE.toString() +
                            this.game.gameMap.teamRegions.get(team).monumentStartingCount +
                            Formatting.WHITE.toString() + " left"
                    );
                } else {
                    content.writeLine(team.getFormatting().toString() + Formatting.BOLD.toString()
                            + Formatting.STRIKETHROUGH.toString() + team.getDisplay() + " Team:");
                    content.writeLine(Formatting.GRAY.toString() + "» " +
                            Formatting.WHITE.toString() + "Eliminated!"
                    );
                }
                content.writeLine("");
            }

            content.writeLine(String.format("§7• §aTime: §f%02d:%02d", minutes, seconds));

        });
    }

}