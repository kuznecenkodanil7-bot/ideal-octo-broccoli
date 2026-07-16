package dev.raidmine.stafftool.mixin;

import dev.raidmine.stafftool.RaidMineStaffMod;
import dev.raidmine.stafftool.util.AuthManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tracks every outgoing command, including commands sent by SmartChat. */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "sendChatCommand(Ljava/lang/String;)V", at = @At("HEAD"))
    private void rmtools$observeOutgoingCommand(String command, CallbackInfo ci) {
        if (!AuthManager.canUseMod() || command == null || command.isBlank()) return;
        RaidMineStaffMod.stats().observeManualCommand(command.startsWith("/") ? command : "/" + command);
    }
}
