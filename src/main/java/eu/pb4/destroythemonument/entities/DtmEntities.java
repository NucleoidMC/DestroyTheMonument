package eu.pb4.destroythemonument.entities;

import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public class DtmEntities {
    public static final EntityType<DtmTntEntity> TNT = register("tnt", EntityType.Builder.create(DtmTntEntity::new, SpawnGroup.MISC).dimensions(1, 1).makeFireImmune());

    public static void register() {
    }

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> func) {
        var id = DtmUtil.id(name);
        var block = func.build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, id));
        Registry.register(Registries.ENTITY_TYPE, id, block);
        PolymerEntityUtils.registerType(block);
        return block;
    }

}
