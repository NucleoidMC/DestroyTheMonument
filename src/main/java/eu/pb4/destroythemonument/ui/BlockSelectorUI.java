package eu.pb4.destroythemonument.ui;

import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.game.logic.BaseGameLogic;
import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.sgui.api.GuiHelpers;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class BlockSelectorUI extends SimpleGui {
    private final PlayerData playerData;

    public BlockSelectorUI(ServerPlayerEntity player, PlayerData data, BaseGameLogic game) {
        super(ScreenHandlerType.GENERIC_9X1, player, false);
        this.playerData = data;
        this.setTitle(DtmUtil.getText("ui", "select_block"));

        int pos = 0;
        for (var block : Registries.BLOCK.getEntryList(DTM.BUILDING_BLOCKS).get()) {
            GuiElementBuilder icon = new GuiElementBuilder(block.value().asItem(), 1);
            icon.setCallback((x, clickType, z) -> {
                this.playerData.selectedBlock = block.value();
                this.close();
            });

            this.setSlot(pos, icon);
            pos++;
        }
    }

    public static void openSelector(ServerPlayerEntity player, BaseGameLogic logic) {
        if (GuiHelpers.getCurrentGui(player) instanceof BlockSelectorUI) {
            return;
        }

        new BlockSelectorUI(player, logic.participants.get(PlayerRef.of(player)), logic).open();
    }
}
