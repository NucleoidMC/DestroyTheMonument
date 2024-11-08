package eu.pb4.destroythemonument.mixin;

import eu.pb4.destroythemonument.other.DtmResetable;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(AttributeContainer.class)
public class AttributeContainerMixin implements DtmResetable {

    @Shadow @Final private DefaultAttributeContainer fallback;

    @Shadow @Final private Map<RegistryEntry<EntityAttribute>, EntityAttributeInstance> custom;

    @Override
    public void dtm$reset() {
        for (var x : this.custom.entrySet()) {
            x.getValue().setBaseValue(this.fallback.getBaseValue(x.getKey()));
        }
    }
}
