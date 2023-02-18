package eu.pb4.destroythemonument.ui;

import eu.pb4.destroythemonument.game.logic.BaseGameLogic;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.sgui.api.GuiHelpers;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PlayOrSpectateUI extends SimpleGui {
    private boolean allowClosing = false;

    public PlayOrSpectateUI(ServerPlayerEntity player, BaseGameLogic game) {
        super(ScreenHandlerType.GENERIC_9X3, player, false);
        this.setTitle(DtmUtil.getText("ui", "join_selector.title"));
        this.setSlot(11, new GuiElementBuilder(Items.DIAMOND_SWORD)
                .setName(DtmUtil.getText("ui", "join_selector.play").formatted(Formatting.GOLD))
                .hideFlags()
                .setCallback((x, y, z, p) -> {
                    this.allowClosing = true;
                    this.close();
                    game.addNewParticipant(player);
                }));

        this.setSlot(15, new GuiElementBuilder(Items.ENDER_EYE)
                .hideFlags()
                .setName(DtmUtil.getText("ui", "join_selector.spectate").formatted(Formatting.GOLD))
                .setCallback((x, y, z, p) -> {
                    this.allowClosing = true;
                    this.close();
                }));


        var empty = new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).setName(Text.empty()).asStack();

        for (int x = 0; x < 9; x++) {
            this.setSlot(x, empty);
            this.setSlot(x + 18, empty);
        }
        this.setSlot(9, empty);
        this.setSlot(17, empty);
    }

    @Override
    public boolean canPlayerClose() {
        return this.allowClosing;
    }

    public static void open(ServerPlayerEntity player, BaseGameLogic logic) {
        if (GuiHelpers.getCurrentGui(player) instanceof PlayOrSpectateUI) {
            return;
        }

        new PlayOrSpectateUI(player, logic).open();
    }
}
