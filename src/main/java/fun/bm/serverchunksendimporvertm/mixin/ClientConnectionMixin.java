package fun.bm.serverchunksendimporvertm.mixin;

import com.google.common.collect.Queues;
import fun.bm.serverchunksendimporvertm.util.QueuedPacketOuter;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Shadow
    private void sendQueuedPackets() {
    }

    @Shadow
    private void sendImmediately(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
    }

    @Shadow
    @Final
    private Queue<Objects> packetQueue;

    @Unique
    private AtomicBoolean isInQueue;

    @Unique
    private Queue<QueuedPacketOuter> chunkPacketQueue = Queues.newConcurrentLinkedQueue();

    @Unique
    private final static long MAX_WAITING_TIME = 100; // 20ticks, 0.1s

    @Unique
    private long lastTickTime;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        lastTickTime = System.currentTimeMillis();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        this.isInQueue = new AtomicBoolean(false);
        this.chunkPacketQueue = Queues.newConcurrentLinkedQueue();
        this.lastTickTime = System.currentTimeMillis();
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V",
            at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, PacketCallbacks callbacks, CallbackInfo ci) {
        if (packet instanceof ChunkDataS2CPacket) { // always queue chunk packets
            ci.cancel();
            QueuedPacketOuter queuedPacket = new QueuedPacketOuter(packet, callbacks);
            this.chunkPacketQueue.add(queuedPacket);
        }
    }


    @Inject(method = "sendQueuedPackets", at = @At("TAIL"))
    private void sendQueuedPacketsTail(CallbackInfo ci) {
        this.queueAndSendPackets();
    }

    @Unique
    private void queueAndSendPackets() {
        if (isInQueue.get()) return;
        isInQueue.set(true);
        boolean firstSend = true;
        while ((firstSend || System.currentTimeMillis() < lastTickTime + MAX_WAITING_TIME) && !this.chunkPacketQueue.isEmpty()) {
            if (!this.packetQueue.isEmpty()) {
                sendQueuedPackets();
            }
            if (firstSend || System.currentTimeMillis() < lastTickTime + MAX_WAITING_TIME) {
                QueuedPacketOuter queuedPacket = this.chunkPacketQueue.poll();
                this.sendImmediately(queuedPacket.packet, queuedPacket.callbacks);
                firstSend = false;
            }
        }
        isInQueue.set(false);
    }
}
