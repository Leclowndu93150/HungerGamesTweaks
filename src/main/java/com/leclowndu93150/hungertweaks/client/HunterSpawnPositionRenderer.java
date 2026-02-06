package com.leclowndu93150.hungertweaks.client;

import com.leclowndu93150.hungertweaks.hunter.HunterSpawnWandItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class HunterSpawnPositionRenderer {

    private static List<BlockPos> positions = new ArrayList<>();

    public static void setPositions(List<BlockPos> newPositions) {
        positions = new ArrayList<>(newPositions);
    }

    public static void register() {
        WorldRenderEvents.LAST.register(HunterSpawnPositionRenderer::onRenderLast);
    }

    private static void onRenderLast(WorldRenderContext context) {
        if (positions.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean holdingWand = mc.player.getMainHandItem().getItem() instanceof HunterSpawnWandItem
                || mc.player.getOffhandItem().getItem() instanceof HunterSpawnWandItem;
        if (!holdingWand) return;

        PoseStack poseStack = context.matrixStack();
        if (poseStack == null) return;

        MultiBufferSource consumers = context.consumers();
        if (consumers == null) return;

        VertexConsumer vertexConsumer = consumers.getBuffer(RenderType.lines());
        double camX = context.camera().getPosition().x;
        double camY = context.camera().getPosition().y;
        double camZ = context.camera().getPosition().z;

        for (BlockPos pos : positions) {
            double cx = pos.getX() + 0.5 - camX;
            double cy = pos.getY() + 1.0 - camY;
            double cz = pos.getZ() + 0.5 - camZ;

            double halfWidth = 0.3;
            double height = 1.8;

            poseStack.pushPose();
            ShapeRenderer.renderLineBox(
                    poseStack, vertexConsumer,
                    cx - halfWidth, cy, cz - halfWidth,
                    cx + halfWidth, cy + height, cz + halfWidth,
                    1.0F, 0.3F, 0.3F, 1.0F
            );
            poseStack.popPose();
        }

        if (consumers instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch(RenderType.lines());
        }
    }
}
