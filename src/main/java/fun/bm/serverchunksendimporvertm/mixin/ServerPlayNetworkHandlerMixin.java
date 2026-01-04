package fun.bm.serverchunksendimporvertm.mixin;

import fun.bm.serverchunksendimporvertm.util.ConnectionStore;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow
    @Final
    private ClientConnection connection;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(MinecraftServer server, ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        ConnectionStore.add(this.connection, player);
    }

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    private void onDisconnected(CallbackInfo ci) {
        ConnectionStore.removeConnection(this.connection);
    }
}
