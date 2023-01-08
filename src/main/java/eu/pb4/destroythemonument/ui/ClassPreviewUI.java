package eu.pb4.destroythemonument.ui;

import eu.pb4.destroythemonument.playerclass.PlayerClass;
import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.sgui.api.elements.AnimatedGuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.DecimalFormat;

public class ClassPreviewUI extends SimpleGui {
    private final ClassSelectorUI selectorUI;
    private final PlayerClass playerClass;
    private static final DecimalFormat df = new DecimalFormat("0.00");

    public ClassPreviewUI(ClassSelectorUI selectorUI, PlayerClass playerClass) {
        super(ScreenHandlerType.GENERIC_9X3, selectorUI.getPlayer(), false);
        this.selectorUI = selectorUI;
        this.playerClass = playerClass;
        this.setTitle(DtmUtil.getText("ui", "class_preview", DtmUtil.getText("class", playerClass.name())));

        int pos = 0;

        for (ItemStack itemStack : this.playerClass.items()) {
            this.setSlot(pos++, itemStack.copy());
        }

        for (PlayerClass.RestockableItem item : this.playerClass.restockableItems()) {
            ItemStack stackA = ItemStack.EMPTY;

            if (item.startingCount > 0) {
                stackA = item.itemStack.copy();
                stackA.setCount(item.startingCount);
            }

            ItemStack stackB = item.itemStack.copy();
            stackB.setCount(item.maxCount);
            this.setSlot(pos++, new AnimatedGuiElement(new ItemStack[]{ stackA, stackB }, 20,false, (x,y,z) -> {}));
        }

        pos = 0;

        var b = new GuiElementBuilder(Items.NAME_TAG)
                .setName(DtmUtil.getText("ui", "stats").setStyle(Style.EMPTY.withBold(true).withFormatting(Formatting.GOLD)))

                .setCallback((x, y, z) -> {
                    this.close();
                });

        for (var x : playerClass.attributes().entrySet()) {
            String num;

            if (x.getKey() == EntityAttributes.GENERIC_MOVEMENT_SPEED) {
                num = df.format(x.getValue() * 20) + " m/s";
            } else if (x.getKey() == EntityAttributes.GENERIC_ARMOR || x.getKey() == EntityAttributes.GENERIC_MAX_HEALTH) {
                num = String.valueOf(x.getValue().intValue());
            } else {
                num = (int) (x.getValue() * 100) + "%";
            }

            b.addLoreLine(Text.empty()
                    .append(Text.translatable(x.getKey().getTranslationKey())
                            .append(Text.literal(": ").formatted(Formatting.GRAY))
                            .append(Text.literal(num))));
        }

        this.setSlot(this.size - 2, b);
        this.setSlot(this.size - 1, new GuiElementBuilder(Items.BARRIER)
                .setName(DtmUtil.getText("ui", "return_selector").setStyle(Style.EMPTY.withItalic(false)))
                .setCallback((x, y, z) -> {
                    this.close();
                })
        );
    }

    @Override
    public void onClose() {
        this.player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.MASTER, 0.5f, 1);
        selectorUI.open();
        super.onClose();
    }
}
