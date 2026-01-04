package fun.bm.serverchunksendimporvertm.util;

import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;

public class QueuedPacketOuter {
    public final Packet<?> packet;
    @Nullable
    public final PacketCallbacks callbacks;

    public QueuedPacketOuter(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
        this.packet = packet;
        this.callbacks = callbacks;
    }
}
