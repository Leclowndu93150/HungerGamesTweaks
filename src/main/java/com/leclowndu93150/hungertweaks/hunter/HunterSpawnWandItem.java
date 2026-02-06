package com.leclowndu93150.hungertweaks.hunter;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

import java.util.function.Consumer;

public class HunterSpawnWandItem extends Item {

    public HunterSpawnWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.PASS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;
        if (!player.hasPermissions(2)) return InteractionResult.PASS;

        HunterSpawnManager manager = HunterSpawnManager.get(player.getServer());
        BlockPos targetPos = context.getClickedPos();

        if (player.isShiftKeyDown()) {
            if (manager.removeNearestSpawnPosition(targetPos)) {
                player.sendSystemMessage(Component.literal("Removed nearest hunter spawn position").withStyle(ChatFormatting.RED));
            } else {
                player.sendSystemMessage(Component.literal("No nearby hunter spawn position found").withStyle(ChatFormatting.YELLOW));
            }
        } else {
            manager.addSpawnPosition(targetPos);
            player.sendSystemMessage(Component.literal("Added hunter spawn position at " + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ()).withStyle(ChatFormatting.GREEN));
        }

        manager.syncPositionsToAll(player.getServer());
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.accept(Component.literal("Right-click: Set hunter spawn position").withStyle(ChatFormatting.GRAY));
        tooltipComponents.accept(Component.literal("Shift + Right-click: Remove nearest position").withStyle(ChatFormatting.GRAY));
        tooltipComponents.accept(Component.literal("Hold to see positions").withStyle(ChatFormatting.DARK_GRAY));
    }
}
