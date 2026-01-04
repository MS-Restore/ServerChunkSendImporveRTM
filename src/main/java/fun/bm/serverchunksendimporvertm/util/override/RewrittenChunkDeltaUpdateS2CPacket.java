package fun.bm.serverchunksendimporvertm.util.override;

import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkSection;

public class RewrittenChunkDeltaUpdateS2CPacket extends ChunkDeltaUpdateS2CPacket {
    public ChunkPos pos;
    public ChunkDeltaUpdateS2CPacket original;

    public RewrittenChunkDeltaUpdateS2CPacket(ChunkSectionPos sectionPos, ShortSet positions, ChunkSection section) {
        super(sectionPos, positions, section);
        this.original = new ChunkDeltaUpdateS2CPacket(sectionPos, positions, section);
        this.pos = sectionPos.toChunkPos();
    }
}
