package eu.pb4.destroythemonument.ui;

import eu.pb4.destroythemonument.game.logic.BaseGameLogic;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.sgui.api.SguiUtils;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

public class PlayOrSpectateUI extends SimpleGui {
    private boolean allowClosing = false;

    public PlayOrSpectateUI(ServerPlayer player, BaseGameLogic game) {
        super(MenuType.GENERIC_9x3, player, false);
        this.setTitle(DtmUtil.getText("ui", "join_selector.title"));
        this.setSlot(11, new GuiElementBuilder(Items.DIAMOND_SWORD)
                .setName(DtmUtil.getText("ui", "join_selector.play").withStyle(ChatFormatting.GOLD))
                .hideDefaultTooltip()
                .setCallback((x, y, z, p) -> {
                    this.allowClosing = true;
                    this.close();
                    //game.addNewParticipant(player);
                }));

        this.setSlot(15, new GuiElementBuilder(Items.ENDER_EYE)
                .hideDefaultTooltip()
                .setName(DtmUtil.getText("ui", "join_selector.spectate").withStyle(ChatFormatting.GOLD))
                .setCallback((x, y, z, p) -> {
                    this.allowClosing = true;
                    this.close();
                }));


        var empty = new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).setName(Component.empty()).asStack();

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

    public static void open(ServerPlayer player, BaseGameLogic logic) {
        if (SguiUtils.getCurrentGui(player) instanceof PlayOrSpectateUI) {
            return;
        }

        new PlayOrSpectateUI(player, logic).open();
    }
}
