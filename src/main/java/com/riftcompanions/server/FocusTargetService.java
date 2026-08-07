package com.riftcompanions.server;

import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.team.TeamBlackboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

/** Server-derived target ping from the player's actual view vector; no client entity ID is trusted. */
public final class FocusTargetService {
    private FocusTargetService() {}

    public static FocusResult focusLookTarget(final ServerPlayer player) {
        final Vec3 eye = player.getEyePosition();
        final Vec3 look = player.getViewVector(1.0F).normalize();
        Monster best = null;
        double bestScore = Double.MAX_VALUE;
        for (final Monster candidate : player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(18.0D), player::hasLineOfSight)) {
            final Vec3 delta = candidate.getEyePosition().subtract(eye);
            final double distance = delta.length();
            if (distance < 0.01D || distance > 18.0D) continue;
            final double facing = look.dot(delta.scale(1.0D / distance));
            if (facing < 0.78D) continue;
            final double score = distance + (1.0D - facing) * 20.0D;
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        if (best == null) {
            return FocusResult.failure("NO_VALID_FOCUS_TARGET", "Look directly at a nearby visible monster to focus the team on it.");
        }
        final Monster focus = best;
        final TeamBlackboard board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        board.setFocusTarget(focus.getUUID(), player.level().getGameTime() + 200L);
        for (final CompanionRole role : new CompanionRole[]{CompanionRole.GUARDIAN, CompanionRole.GIFTED, CompanionRole.SCOUT}) {
            CompanionLifecycleService.findForOwner(player, role).ifPresent(companion -> {
                if (companion.allowsCombatAction()) {
                    companion.setTarget(focus);
                    if (role == CompanionRole.GUARDIAN) {
                        companion.setCompanionState(CompanionState.GUARDING, "PLAYER_FOCUS_TARGET");
                    } else {
                        companion.setCompanionState(CompanionState.FIGHTING, "PLAYER_FOCUS_TARGET");
                    }
                }
            });
        }
        CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                .ifPresent(companion -> DialogueService.get().speak(companion, "guard_order", 1));
        TeamSavedData.get(player.server).markChanged();
        return FocusResult.success("FOCUS_TARGET_SET", "The team focused the nearby threat within safety limits.");
    }

    public record FocusResult(boolean successful, String code, String detail) {
        public static FocusResult success(final String code, final String detail) { return new FocusResult(true, code, detail); }
        public static FocusResult failure(final String code, final String detail) { return new FocusResult(false, code, detail); }
    }
}
