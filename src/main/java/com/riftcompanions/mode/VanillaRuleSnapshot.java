package com.riftcompanions.mode;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;

/**
 * Read-only snapshot of relevant Vanilla world rules. It exposes real world
 * facts to companion policy without inventing heat, thirst, ropes, or other
 * mechanics that Vanilla does not provide.
 */
public record VanillaRuleSnapshot(
        Difficulty difficulty,
        boolean mobGriefing,
        boolean keepInventory,
        boolean daylightCycle,
        boolean hardcore,
        boolean playerCreative,
        boolean playerSpectator
) {
    public static VanillaRuleSnapshot from(final ServerPlayer player) {
        if (player == null) {
            return new VanillaRuleSnapshot(Difficulty.PEACEFUL, false, false, true, false, false, true);
        }
        final var rules = player.level().getGameRules();
        final boolean hardcore = player.server != null && player.server.isHardcore();
        return new VanillaRuleSnapshot(player.level().getDifficulty(),
                rules.getBoolean(GameRules.RULE_MOBGRIEFING),
                rules.getBoolean(GameRules.RULE_KEEPINVENTORY),
                rules.getBoolean(GameRules.RULE_DAYLIGHT),
                hardcore,
                player.isCreative(),
                player.isSpectator());
    }

    public boolean peaceful() {
        return difficulty == Difficulty.PEACEFUL;
    }

    /** Survival combat initiative is intentionally quiet outside normal Survival. */
    public boolean survivalPressurePaused() {
        return playerCreative || playerSpectator || peaceful();
    }

    /** Companion block edits remain prohibited even when Vanilla permits mob griefing. */
    public boolean companionWorldEditsAllowed() {
        return false;
    }

    public String operationalSummary() {
        if (playerSpectator) return "Spectator policy: companions hold safely and do not execute plans.";
        if (playerCreative) return "Creative policy: survival pressure and automatic combat planning are paused.";
        if (peaceful()) return "Peaceful policy: combat planning is paused; exploration and base guidance remain available.";
        if (hardcore) return "Hardcore policy: Story Downed remains active; no companion permadeath is silently enabled.";
        if (!mobGriefing) return "Mob Griefing is off; companion block edits remain disabled by design.";
        if (!daylightCycle) return "Daylight cycle is off; day-based ambient opportunities remain conservative.";
        if (keepInventory) return "Keep Inventory is on; companion Downed rules remain unchanged.";
        return "Vanilla rule policy is active with bounded companion safety behavior.";
    }
}
