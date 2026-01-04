package fun.bm.serverchunksendimporvertm.util;

import net.minecraft.util.math.ChunkPos;
import org.jetbrains.annotations.UnknownNullability;

public class ChunkPriorityUtil {
    public static int calculateChunkPriority(@UnknownNullability ChunkPos chunkPos, int playerChunkX, int playerChunkZ) {
        int distanceX = Math.abs(chunkPos.x - playerChunkX);
        int distanceZ = Math.abs(chunkPos.z - playerChunkZ);
        return distanceX + distanceZ;
    }
}