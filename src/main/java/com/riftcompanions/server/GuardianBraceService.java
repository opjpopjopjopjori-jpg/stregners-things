package com.riftcompanions.server;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.entity.CompanionAbility;
import com.riftcompanions.entity.CompanionAction;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.safety.SafeModeService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Short explicit Guardian protection stance. It is a local damage reduction,
 * not invulnerability, taunt ownership, projectile deletion, or a farm tool.
 * State is intentionally non-persistent; reload cancels the temporary stance.
 */
public final class GuardianBraceService {
    private static final long DURATION_TICKS = 80L;
    private static final long COOLDOWN_TICKS = 180L;
    private static final float ENERGY_COST = 16.0F;
    private static final Map<UUID, BraceState> ACTIVE = new HashMap<>();

    private GuardianBraceService() {}

    public static BraceResult activate(final ServerPlayer player, final CompanionEntity guardian) {
        if (player == null || guardian == null || guardian.getRole() != CompanionRole.GUARDIAN) {
            return BraceResult.failure("GUARDIAN_BRACE_CONTEXT_INVALID", "Guardian Brace needs an active Guardian.");
        }
        if (SafeModeService.enabled(player)) {
            return BraceResult.failure("SAFE_MODE_POWER_ACTIONS_DISABLED", "Guardian Brace is unavailable while Safe Mode is active.");
        }
        if (guardian.distanceToSqr(player) > 8.0D * 8.0D) {
            return BraceResult.failure("GUARDIAN_BRACE_OUT_OF_RANGE", "Guardian Brace requires the Guardian to remain close to the player.");
        }
        if (!hasImmediateThreat(player, guardian)) {
            return BraceResult.failure("GUARDIAN_BRACE_REQUIRES_THREAT", "Guardian Brace requires an active nearby hostile threat.");
        }
        if (guardian.getEnergy() < ENERGY_COST) {
            return BraceResult.failure("GUARDIAN_BRACE_ENERGY_LOW", "The Guardian does not have enough energy to brace safely.");
        }
        if (!guardian.consumeEnergy(ENERGY_COST)) {
            return BraceResult.failure("GUARDIAN_BRACE_ENERGY_LOW", "The Guardian does not have enough energy to brace safely.");
        }
        final long now = player.level().getGameTime();
        ACTIVE.put(player.getUUID(), new BraceState(guardian.getUUID(), now + DURATION_TICKS));
        guardian.setAbilityCooldown(CompanionAbility.GUARDIAN_BRACE, COOLDOWN_TICKS);
        guardian.setCompanionState(CompanionState.GUARDING, "GUARDIAN_BRACE_ACTIVE");
        guardian.beginVisualAction(CompanionAction.GUARDIAN_BRACE, DURATION_TICKS);
        final int particles = CompanionConfig.LOW_EFFECTS.get() ? 3 : 12;
        player.serverLevel().sendParticles(ParticleTypes.ENCHANT, guardian.getX(), guardian.getY() + guardian.getBbHeight() * 0.55D,
                guardian.getZ(), particles, 0.35D, 0.45D, 0.35D, 0.02D);
        return BraceResult.success("GUARDIAN_BRACE_ACTIVE", "Guardian Brace is active briefly. It reduces incoming player damage but never grants invulnerability.");
    }

    public static boolean protects(final ServerPlayer player) {
        if (player == null || SafeModeService.enabled(player)) return false;
        final BraceState state = ACTIVE.get(player.getUUID());
        if (state == null) return false;
        final long now = player.level().getGameTime();
        final CompanionEntity guardian = CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN).orElse(null);
        if (now >= state.expiresAt()) {
            ACTIVE.remove(player.getUUID());
            clearBraceVisual(guardian, state.guardianUuid());
            return false;
        }
        if (guardian == null || !guardian.getUUID().equals(state.guardianUuid())
                || guardian.getCompanionState() == CompanionState.DOWNED || guardian.distanceToSqr(player) > 8.0D * 8.0D) {
            ACTIVE.remove(player.getUUID());
            clearBraceVisual(guardian, state.guardianUuid());
            return false;
        }
        return true;
    }

    public static void tick(final ServerPlayer player) {
        if (player == null) return;
        protects(player);
    }

    public static void cancelForOwner(final ServerPlayer player, final String reason) {
        if (player == null) return;
        final BraceState removed = ACTIVE.remove(player.getUUID());
        if (removed == null) return;
        CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                .filter(guardian -> guardian.getUUID().equals(removed.guardianUuid()))
                .ifPresent(guardian -> clearBraceVisual(guardian, removed.guardianUuid()));
    }

    public static void clearGuardian(final UUID guardianUuid) {
        if (guardianUuid == null) return;
        ACTIVE.entrySet().removeIf(entry -> guardianUuid.equals(entry.getValue().guardianUuid()));
    }

    private static void clearBraceVisual(final CompanionEntity guardian, final UUID expectedGuardian) {
        if (guardian != null && guardian.getUUID().equals(expectedGuardian)
                && guardian.getVisualAction() == CompanionAction.GUARDIAN_BRACE) {
            guardian.beginVisualAction(CompanionAction.NONE, 1L);
        }
    }

    private static boolean hasImmediateThreat(final ServerPlayer player, final CompanionEntity guardian) {
        return !player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(8.0D), monster -> {
            if (!monster.isAlive()) return false;
            final var target = monster.getTarget();
            return target == player || target == guardian || monster.distanceToSqr(player) <= 4.0D * 4.0D;
        }).isEmpty();
    }

    public record BraceResult(boolean successful, String code, String detail) {
        public static BraceResult success(final String code, final String detail) { return new BraceResult(true, code, detail); }
        public static BraceResult failure(final String code, final String detail) { return new BraceResult(false, code, detail); }
    }

    private record BraceState(UUID guardianUuid, long expiresAt) {}
}
