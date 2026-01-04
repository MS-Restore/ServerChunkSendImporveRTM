package fun.bm.serverchunksendimporvertm.mixin;

import fun.bm.serverchunksendimporvertm.util.ChunkPriorityUtil;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

import static net.minecraft.server.world.ThreadedAnvilChunkStorage.isWithinDistance;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorageMixin {
    @Final
    @Shadow
    private PlayerChunkWatchingManager playerChunkWatchingManager;

    @Shadow
    int watchDistance;

    @Shadow
    private boolean doesNotGenerateChunks(ServerPlayerEntity player) {
        throw new AssertionError();
    }

    @Shadow
    private ChunkSectionPos updateWatchedSection(ServerPlayerEntity player) {
        throw new AssertionError();
    }

    @Shadow
    protected void sendWatchPackets(ServerPlayerEntity player, ChunkPos pos, MutableObject<ChunkDataS2CPacket> packet, boolean oldWithinViewDistance, boolean newWithinViewDistance) {
        throw new AssertionError();
    }

    @Inject(method = "handlePlayerAddedOrRemoved", at = @At("HEAD"), cancellable = true)
    void handlePlayerAddedOrRemoved(ServerPlayerEntity player, boolean added, CallbackInfo ci) {
        ci.cancel();
        boolean bl = this.doesNotGenerateChunks(player);
        boolean bl2 = this.playerChunkWatchingManager.isWatchInactive(player);
        int i = ChunkSectionPos.getSectionCoord(player.getBlockX());
        int j = ChunkSectionPos.getSectionCoord(player.getBlockZ());
        if (added) {
            this.playerChunkWatchingManager.add(ChunkPos.toLong(i, j), player, bl);
            this.updateWatchedSection(player);
            if (!bl) {
                ((ThreadedAnvilChunkStorage) (Object) this).getTicketManager().handleChunkEnter(ChunkSectionPos.from(player), player);
            }
        } else {
            ChunkSectionPos chunkSectionPos = player.getWatchedSection();
            this.playerChunkWatchingManager.remove(chunkSectionPos.toChunkPos().toLong(), player);
            if (!bl2) {
                ((ThreadedAnvilChunkStorage) (Object) this).getTicketManager().handleChunkLeave(chunkSectionPos, player);
            }
        }

        // first we list all chunks we need to send to client

        Map<Integer, Set<ChunkPos>> posOrigins = new HashMap<>();

        for (int k = i - this.watchDistance - 1; k <= i + this.watchDistance + 1; ++k) {
            for (int l = j - this.watchDistance - 1; l <= j + this.watchDistance + 1; ++l) {
                if (isWithinDistance(k, l, i, j, this.watchDistance)) {
                    ChunkPos chunkPos = new ChunkPos(k, l);
                    // we won't send chunks packet here, we will send it later, we only need to list them
                    // this.sendWatchPackets(player, chunkPos, new MutableObject(), !added, added);
                    int priority = ChunkPriorityUtil.calculateChunkPriority(chunkPos, player.getBlockX(), player.getBlockZ());
                    Set<ChunkPos> chunkPosArray = posOrigins.get(priority);
                    if (chunkPosArray == null) chunkPosArray = new HashSet<>();
                    chunkPosArray.add(chunkPos);
                    posOrigins.put(priority, chunkPosArray);
                }
            }
        }

        // then we need to sort them by priority
        List<ChunkPos> posFinal = new ArrayList<>();
        posOrigins.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            posFinal.addAll(entry.getValue());
        });

        // send all of them to packet list, we will send them on client connection one by one later
        while (!posFinal.isEmpty()) {
            ChunkPos prepareForSend = posFinal.removeFirst();
            this.sendWatchPackets(player, prepareForSend, new MutableObject(), !added, added);
        }
    }
}
