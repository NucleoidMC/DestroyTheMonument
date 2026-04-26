package eu.pb4.destroythemonument.entities;

import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class DtmEntities {
    public static final EntityType<DtmTntEntity> TNT = register("tnt", EntityType.Builder.of(DtmTntEntity::new, MobCategory.MISC).sized(1, 1).fireImmune());

    public static void register() {
    }

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> func) {
        var id = DtmUtil.id(name);
        var block = func.build(ResourceKey.create(Registries.ENTITY_TYPE, id));
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id, block);
        PolymerEntityUtils.registerType(block);
        return block;
    }

}
