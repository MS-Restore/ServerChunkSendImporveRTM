package fun.bm.serverchunksendimporvertm.mixin;

import fun.bm.serverchunksendimporvertm.util.override.RewrittenChunkDeltaUpdateS2CPacket;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.BitSet;
import java.util.List;

@Mixin(ChunkHolder.class)
public class ChunkHolderMixin {
    @Shadow
    private boolean pendingBlockUpdates;

    @Shadow
    @Final
    private BitSet skyLightUpdateBits;

    @Shadow
    @Final
    private BitSet blockLightUpdateBits;

    @Shadow
    @Final
    private ChunkHolder.PlayersWatchingChunkProvider playersWatchingChunkProvider;

    @Shadow
    @Final
    private LightingProvider lightingProvider;

    @Shadow
    private void sendPacketToPlayers(List<ServerPlayerEntity> players, Packet<?> packet) {
    }

    @Shadow
    @Final
    private ShortSet[] blockUpdatesBySection;

    @Shadow
    @Final
    private HeightLimitView world;

    @Shadow
    private void tryUpdateBlockEntityAt(List<ServerPlayerEntity> players, World world, BlockPos pos, BlockState state) {
    }

    @Shadow
    @Final
    ChunkPos pos;

    @Inject(method = "flushUpdates", at = @At("HEAD"), cancellable = true)
    public void flushUpdates(WorldChunk chunk, CallbackInfo ci) {
        ci.cancel();
        if (this.pendingBlockUpdates || !this.skyLightUpdateBits.isEmpty() || !this.blockLightUpdateBits.isEmpty()) {
            World world = chunk.getWorld();
            if (!this.skyLightUpdateBits.isEmpty() || !this.blockLightUpdateBits.isEmpty()) {
                List<ServerPlayerEntity> list = this.playersWatchingChunkProvider.getPlayersWatchingChunk(this.pos, true);
                if (!list.isEmpty()) {
                    LightUpdateS2CPacket lightUpdateS2CPacket = new LightUpdateS2CPacket(
                            chunk.getPos(), this.lightingProvider, this.skyLightUpdateBits, this.blockLightUpdateBits
                    );
                    this.sendPacketToPlayers(list, lightUpdateS2CPacket);
                }

                this.skyLightUpdateBits.clear();
                this.blockLightUpdateBits.clear();
            }

            if (this.pendingBlockUpdates) {
                List<ServerPlayerEntity> list = this.playersWatchingChunkProvider.getPlayersWatchingChunk(this.pos, false);

                for (int i = 0; i < this.blockUpdatesBySection.length; i++) {
                    ShortSet shortSet = this.blockUpdatesBySection[i];
                    if (shortSet != null) {
                        this.blockUpdatesBySection[i] = null;
                        if (!list.isEmpty()) {
                            int j = this.world.sectionIndexToCoord(i);
                            ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(chunk.getPos(), j);
                            if (shortSet.size() == 1) {
                                BlockPos blockPos = chunkSectionPos.unpackBlockPos(shortSet.iterator().nextShort());
                                BlockState blockState = world.getBlockState(blockPos);
                                this.sendPacketToPlayers(list, new BlockUpdateS2CPacket(blockPos, blockState));
                                this.tryUpdateBlockEntityAt(list, world, blockPos, blockState);
                            } else {
                                ChunkSection chunkSection = chunk.getSection(i);
                                ChunkDeltaUpdateS2CPacket chunkDeltaUpdateS2CPacket = new RewrittenChunkDeltaUpdateS2CPacket(chunkSectionPos, shortSet, chunkSection);
                                this.sendPacketToPlayers(list, chunkDeltaUpdateS2CPacket);
                                chunkDeltaUpdateS2CPacket.visitUpdates((pos, state) -> this.tryUpdateBlockEntityAt(list, world, pos, state));
                            }
                        }
                    }
                }

                this.pendingBlockUpdates = false;
            }
        }
    }
}
