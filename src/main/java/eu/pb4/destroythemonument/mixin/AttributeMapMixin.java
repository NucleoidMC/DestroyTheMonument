package eu.pb4.destroythemonument.mixin;

import eu.pb4.destroythemonument.other.DtmResetable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

@Mixin(AttributeMap.class)
public class AttributeMapMixin implements DtmResetable {

    @Shadow @Final private AttributeSupplier supplier;

    @Shadow @Final private Map<Holder<Attribute>, AttributeInstance> attributes;

    @Override
    public void dtm$reset() {
        for (var x : this.attributes.entrySet()) {
            x.getValue().setBaseValue(this.supplier.getBaseValue(x.getKey()));
        }
    }
}
