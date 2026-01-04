package fun.bm.serverchunksendimporvertm;

import fun.bm.serverchunksendimporvertm.event.TickEvent;
import net.fabricmc.api.ModInitializer;

public class Serverchunksendimporvertm implements ModInitializer {

    @Override
    public void onInitialize() {
        TickEvent.register();
    }
}
