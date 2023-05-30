package eu.pb4.destroythemonument.other;

import net.minecraft.network.packet.Packet;

public interface MarkedPacket {
    boolean dtm_isMarked();

    void dtm_mark();


    static boolean is(Packet<?> packet) {
        if (packet instanceof MarkedPacket markedPacket) {
            return markedPacket.dtm_isMarked();
        }
        return true;
    }

    static <T extends Packet<?>> T mark(T packet) {
        ((MarkedPacket) packet).dtm_mark();
        return packet;
    }
}
