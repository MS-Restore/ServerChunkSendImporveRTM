package fun.bm.serverchunksendimporvertm.mixin;

import fun.bm.serverchunksendimporvertm.util.ChunkPriorityUtil;
import fun.bm.serverchunksendimporvertm.util.ConnectionStore;
import fun.bm.serverchunksendimporvertm.util.RTMQueuedPacket;
import fun.bm.serverchunksendimporvertm.util.override.RewrittenChunkDeltaUpdateS2CPacket;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.PriorityBlockingQueue;
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
    private java.util.Queue<Objects> packetQueue;

    @Shadow
    public void send(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
    }

    @Unique
    private AtomicBoolean isInQueue;

    @Unique
    private PriorityBlockingQueue<RTMQueuedPacket> chunkPacketQueue;

    @Unique
    private final static long MAX_WAITING_TIME = 100; // 2ticks, 0.1s

    @Unique
    private long lastTickTime;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        lastTickTime = System.currentTimeMillis();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        this.isInQueue = new AtomicBoolean(false);
        this.chunkPacketQueue = new PriorityBlockingQueue<>(11, (a, b) -> {
            int distA = getChunkDistance((ChunkDataS2CPacket) a.packet);
            int distB = getChunkDistance((ChunkDataS2CPacket) b.packet);
            return Integer.compare(distA, distB);
        });
        this.lastTickTime = System.currentTimeMillis();
    }

    @Unique
    private int getChunkDistance(ChunkDataS2CPacket packet) {
        ChunkPos chunkPos = new ChunkPos(packet.getX(), packet.getZ());
        BlockPos pos;
        try {
            pos = ConnectionStore.getPlayer((ClientConnection) (Object) this).getBlockPos();
        } catch (Exception ignored) {
            return Integer.MAX_VALUE;
        }
        return ChunkPriorityUtil.calculateChunkPriority(chunkPos, pos.getX(), pos.getZ());
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V",
            at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, PacketCallbacks callbacks, CallbackInfo ci) {
        if (packet instanceof ChunkDataS2CPacket packet1) { // always queue chunk packets
            ci.cancel();
            RTMQueuedPacket queuedPacket = new RTMQueuedPacket(packet1, callbacks);
            this.chunkPacketQueue.add(queuedPacket);
        } else if (packet instanceof BlockUpdateS2CPacket p1) {
            ChunkPos chunkPos = new ChunkPos(p1.getPos());
            List<RTMQueuedPacket> snapshot = new ArrayList<>(this.chunkPacketQueue);
            for (RTMQueuedPacket queuedPacket : snapshot) {
                if (queuedPacket == null) continue;
                ChunkDataS2CPacket chunkData = (ChunkDataS2CPacket) queuedPacket.packet;
                if (chunkPos.x == chunkData.getX() && chunkPos.z == chunkData.getZ()) {
                    queuedPacket.handle(packet, callbacks);
                    ci.cancel();
                }
            }
        } else if (packet instanceof RewrittenChunkDeltaUpdateS2CPacket p2) {
            ChunkPos chunkPos = p2.pos;
            List<RTMQueuedPacket> snapshot = new ArrayList<>(this.chunkPacketQueue);
            for (RTMQueuedPacket queuedPacket : snapshot) {
                if (queuedPacket == null) continue;
                ChunkDataS2CPacket chunkData = (ChunkDataS2CPacket) queuedPacket.packet;
                if (chunkPos.x == chunkData.getX() && chunkPos.z == chunkData.getZ()) {
                    queuedPacket.handle(p2.original, callbacks);
                    ci.cancel();
                }
            }
            ci.cancel();
            this.send(p2.original, callbacks);
        } else if (packet instanceof LightUpdateS2CPacket p3) {
            ChunkPos chunkPos = new ChunkPos(p3.getChunkX(), p3.getChunkZ());
            List<RTMQueuedPacket> snapshot = new ArrayList<>(this.chunkPacketQueue);
            for (RTMQueuedPacket queuedPacket : snapshot) {
                if (queuedPacket == null) continue;
                ChunkDataS2CPacket chunkData = (ChunkDataS2CPacket) queuedPacket.packet;
                if (chunkPos.x == chunkData.getX() && chunkPos.z == chunkData.getZ()) {
                    queuedPacket.handle(packet, callbacks);
                    ci.cancel();
                }
            }
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
        boolean empty = this.chunkPacketQueue.isEmpty();

        while ((firstSend || System.currentTimeMillis() < lastTickTime + MAX_WAITING_TIME) && !empty) {
            if (!this.packetQueue.isEmpty()) {
                sendQueuedPackets();
            }
            if (firstSend || System.currentTimeMillis() < lastTickTime + MAX_WAITING_TIME) {
                RTMQueuedPacket queuedPacket = this.chunkPacketQueue.poll();
                if (queuedPacket != null) {
                    sendAll(queuedPacket);
                    firstSend = false;
                    empty = this.chunkPacketQueue.isEmpty();
                } else {
                    empty = true;
                }
            }
        }
        isInQueue.set(false);
    }

    @Unique
    private void sendAll(RTMQueuedPacket queuedPacket) {
        this.sendImmediately(queuedPacket.packet, queuedPacket.callbacks);
        if (queuedPacket.packets != null) queuedPacket.packets.forEach(this::sendAll);
    }
}
