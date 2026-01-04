package fun.bm.serverchunksendimporvertm.util;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;

public class ConnectionStore {
    public static final Map<ClientConnection, ServerPlayerEntity> connectionStore = new HashMap<>();

    public static void add(ClientConnection connection, ServerPlayerEntity player) {
        connectionStore.put(connection, player);
    }

    public static void removeConnection(ClientConnection connection) {
        connectionStore.remove(connection);
    }

    public static ServerPlayerEntity getPlayer(ClientConnection connection) {
        return connectionStore.get(connection);
    }

    public static void clean() {
        connectionStore.entrySet().removeIf(entry -> entry.getValue().isDisconnected());
    }
}
