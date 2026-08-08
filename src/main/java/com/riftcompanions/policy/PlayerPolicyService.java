package com.riftcompanions.policy;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.power.PowerPolicy;
import com.riftcompanions.server.TeamSavedData;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Server-authoritative personal policy editor; config remains the safe fallback. */
public final class PlayerPolicyService {
    private PlayerPolicyService() {}

    public static PowerPolicy effectivePowerPolicy(final ServerPlayer player, final CompanionRole role) {
        if (role == null) return PowerPolicy.OFF;
        if (player != null && player.server != null) {
            final PowerPolicy override = TeamSavedData.get(player.server).blackboard(player.getUUID()).playerPolicy().override(role).orElse(null);
            if (override != null && supportsPowerPolicy(role, override)) return override;
        }
        return defaultPolicy(role);
    }

    public static PolicyResult cyclePowerPolicy(final ServerPlayer player, final CompanionRole role) {
        if (player == null || player.server == null || role == null) return PolicyResult.failure("INVALID_POLICY_REQUEST", "Choose a valid companion role.");
        final List<PowerPolicy> allowed = allowedPolicies(role);
        if (allowed.isEmpty()) return PolicyResult.failure("ROLE_HAS_NO_POWER_POLICY", "Guardian does not use a power policy in this milestone.");
        final var board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        final PowerPolicy current = board.playerPolicy().override(role).orElse(defaultPolicy(role));
        final int currentIndex = Math.max(0, allowed.indexOf(current));
        final PowerPolicy next = allowed.get((currentIndex + 1) % allowed.size());
        board.playerPolicy().setOverride(role, next);
        TeamSavedData.get(player.server).markChanged();
        return PolicyResult.success("POWER_POLICY_UPDATED", role.personalName() + " power policy: " + next + ".");
    }

    public static PolicyResult resetPowerPolicy(final ServerPlayer player, final CompanionRole role) {
        if (player == null || player.server == null || role == null) return PolicyResult.failure("INVALID_POLICY_REQUEST", "Choose a valid companion role.");
        if (allowedPolicies(role).isEmpty()) return PolicyResult.failure("ROLE_HAS_NO_POWER_POLICY", "Guardian does not use a power policy in this milestone.");
        TeamSavedData.get(player.server).blackboard(player.getUUID()).playerPolicy().setOverride(role, null);
        TeamSavedData.get(player.server).markChanged();
        return PolicyResult.success("POWER_POLICY_INHERITS_CONFIG", role.personalName() + " now inherits the world policy: " + defaultPolicy(role) + ".");
    }

    public static boolean supportsPowerPolicy(final CompanionRole role, final PowerPolicy policy) {
        return allowedPolicies(role).contains(policy);
    }

    public static List<PowerPolicy> allowedPolicies(final CompanionRole role) {
        if (role == null) return List.of();
        return switch (role) {
            case SEER -> List.of(PowerPolicy.OFF, PowerPolicy.SENSE_ONLY, PowerPolicy.ASK_FIRST, PowerPolicy.EMERGENCY_ONLY, PowerPolicy.ALLOWED);
            case GIFTED -> List.of(PowerPolicy.OFF, PowerPolicy.RESCUE_ONLY, PowerPolicy.ASK_FIRST, PowerPolicy.EMERGENCY_ONLY, PowerPolicy.ALLOWED);
            case SCOUT -> List.of(PowerPolicy.OFF, PowerPolicy.ASK_FIRST, PowerPolicy.EMERGENCY_ONLY, PowerPolicy.ALLOWED);
            case GUARDIAN -> List.of();
        };
    }

    public static PowerPolicy defaultPolicy(final CompanionRole role) {
        return switch (role) {
            case SEER -> CompanionConfig.WILL_POWER_POLICY.get();
            case GIFTED -> CompanionConfig.ELEVEN_POWER_POLICY.get();
            case SCOUT -> CompanionConfig.MAX_POWER_POLICY.get();
            case GUARDIAN -> PowerPolicy.OFF;
        };
    }

    public record PolicyResult(boolean successful, String code, String detail) {
        public static PolicyResult success(final String code, final String detail) { return new PolicyResult(true, code, detail); }
        public static PolicyResult failure(final String code, final String detail) { return new PolicyResult(false, code, detail); }
    }
}
