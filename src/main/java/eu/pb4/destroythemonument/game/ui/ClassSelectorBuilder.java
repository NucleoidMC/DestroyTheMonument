

package eu.pb4.destroythemonument.game.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;

public final class ClassSelectorBuilder {
    final List<ClassSelectorEntry> elements = new ArrayList();

    public ClassSelectorBuilder() {
    }

    public ClassSelectorBuilder addItem(ItemStack stack) {
        return this.add(ClassSelectorEntry.buyItem(stack));
    }

    public ClassSelectorBuilder add(ClassSelectorEntry entry) {
        this.elements.add(entry);
        return this;
    }
}
