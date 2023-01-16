package eu.pb4.destroythemonument.entities;

import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class DtmEntities {
    public static final EntityType<DtmTntEntity> TNT = FabricEntityTypeBuilder.create(SpawnGroup.MISC, DtmTntEntity::new).dimensions(EntityType.TNT.getDimensions()).fireImmune().build();

    public static void register() {
        register("tnt", TNT);
    }

    private static void register(String name, EntityType<?> block) {
        Registry.register(Registries.ENTITY_TYPE, DtmUtil.id(name), block);
        PolymerEntityUtils.registerType(block);
    }

}
