package eu.pb4.destroythemonument.mixin;

import eu.pb4.destroythemonument.other.MarkedPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({EntityEquipmentUpdateS2CPacket.class, BundleS2CPacket.class})
public class PacketMixin implements MarkedPacket {
    @Unique boolean dtmIsMarked = false;

    @Override
    public boolean dtm_isMarked() {
        return this.dtmIsMarked;
    }

    @Override
    public void dtm_mark() {
        this.dtmIsMarked = true;
    }
}
