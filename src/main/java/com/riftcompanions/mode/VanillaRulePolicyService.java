package com.riftcompanions.mode;

import net.minecraft.server.level.ServerPlayer;

/**
 * Centralizes Vanilla rule interpretation so plans do not each guess at game
 * mode, Peaceful, or world-rule behavior. It never grants world-edit powers.
 */
public final class VanillaRulePolicyService {
    private VanillaRulePolicyService() {}

    public static VanillaRuleSnapshot snapshot(final ServerPlayer player) {
        return VanillaRuleSnapshot.from(player);
    }

    public static boolean allowsCombatPlanning(final ServerPlayer player) {
        return !snapshot(player).survivalPressurePaused();
    }

    public static String combatPlanningBlockCode(final ServerPlayer player) {
        final VanillaRuleSnapshot rules = snapshot(player);
        if (rules.playerSpectator()) return "SPECTATOR_COMBAT_PLAN_PAUSED";
        if (rules.playerCreative()) return "CREATIVE_COMBAT_PLAN_PAUSED";
        if (rules.peaceful()) return "PEACEFUL_COMBAT_PLAN_PAUSED";
        return "COMBAT_PLAN_ALLOWED";
    }

    /** Explicit invariant for all current and future companion world actions. */
    public static boolean allowsCompanionWorldEdit(final ServerPlayer player) {
        return false;
    }
}
