package com.leclowndu93150.hungertweaks.hunter;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class HuntPlayerGoal extends Goal {

    private final Mob mob;
    private final ServerPlayer fixedTarget;
    private Player currentTarget;
    private int recalculatePathTicks;

    public HuntPlayerGoal(Mob mob, ServerPlayer fixedTarget) {
        this.mob = mob;
        this.fixedTarget = fixedTarget;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        updateTarget();
        return currentTarget != null && currentTarget.isAlive() && !currentTarget.isSpectator();
    }

    @Override
    public boolean canContinueToUse() {
        updateTarget();
        return currentTarget != null && currentTarget.isAlive() && !currentTarget.isSpectator();
    }

    @Override
    public void start() {
        recalculatePathTicks = 0;
    }

    @Override
    public void stop() {
        currentTarget = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (currentTarget == null) return;

        mob.getLookControl().setLookAt(currentTarget, 30.0F, 30.0F);

        double distSq = mob.distanceToSqr(currentTarget);

        if (--recalculatePathTicks <= 0) {
            recalculatePathTicks = 4 + mob.getRandom().nextInt(7);

            mob.getNavigation().moveTo(currentTarget, 1.2);
        }

        if (distSq < 4.0 && mob.getSensing().hasLineOfSight(currentTarget)) {
            mob.doHurtTarget((ServerLevel) mob.level(), currentTarget);
        }
    }

    private void updateTarget() {
        if (fixedTarget != null && fixedTarget.isAlive() && !fixedTarget.isSpectator()) {
            currentTarget = fixedTarget;
            mob.setTarget(fixedTarget);
            return;
        }

        if (fixedTarget != null) {
            currentTarget = null;
            mob.setTarget(null);
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) return;

        List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers().stream()
                .filter(p -> p.isAlive() && !p.isSpectator() && !p.isCreative())
                .toList();

        if (players.isEmpty()) {
            currentTarget = null;
            mob.setTarget(null);
            return;
        }

        ServerPlayer closest = players.stream()
                .min(Comparator.comparingDouble(p -> mob.distanceToSqr(p)))
                .orElse(null);

        currentTarget = closest;
        mob.setTarget(closest);
    }

}
