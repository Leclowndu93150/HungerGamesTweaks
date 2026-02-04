package com.leclowndu93150.hungertweaks.mixin;

import com.leclowndu93150.hungertweaks.HungerGamesManager;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class PlayerMovementMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void onHandleMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (player.getServer() == null) return;
        HungerGamesManager manager = HungerGamesManager.get(player.getServer());
        if (manager.isPlayerFrozen(player.getUUID())) {
            Vec3 pos = manager.getFrozenPosition(player.getUUID());
            if (pos != null) {
                player.teleportTo((ServerLevel) player.level(), pos.x, pos.y, pos.z, Relative.ROTATION, 0F, 0F, false);
            }
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerInput", at = @At("HEAD"), cancellable = true)
    private void onHandlePlayerInput(ServerboundPlayerInputPacket packet, CallbackInfo ci) {
        if (player.getServer() == null) return;
        HungerGamesManager manager = HungerGamesManager.get(player.getServer());
        if (manager.isPlayerFrozen(player.getUUID())) {
            ci.cancel();
        }
    }
}
