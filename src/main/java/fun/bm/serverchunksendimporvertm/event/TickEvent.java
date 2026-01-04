package fun.bm.serverchunksendimporvertm.event;

import fun.bm.serverchunksendimporvertm.util.ConnectionStore;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class TickEvent {
    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            ConnectionStore.clean();
        });
    }
}
