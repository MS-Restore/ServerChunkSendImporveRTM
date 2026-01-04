package fun.bm.serverchunksendimporvertm.util;

import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class RTMQueuedPacket {
    public final Packet<?> packet;
    @Nullable
    public final PacketCallbacks callbacks;
    public Set<RTMQueuedPacket> packets = null;

    public RTMQueuedPacket(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
        this.packet = packet;
        this.callbacks = callbacks;
    }

    public final void handle(Packet<?> packet, PacketCallbacks callbacks) {
        if (this.packets == null) this.packets = new HashSet<>();
        this.packets.add(new RTMQueuedPacket(packet, callbacks));
    }
}
