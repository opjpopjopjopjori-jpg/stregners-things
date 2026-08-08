package com.riftcompanions.arc;

import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.feature.FeatureFlag;
import com.riftcompanions.feature.FeatureFlags;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.team.TeamBlackboard;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.Set;

/** Converts real milestones into optional arc reflection without quest railroading. */
public final class ArcService {
    private ArcService() {}

    public static void observe(final ServerPlayer player, final ArcMilestone milestone) {
        if (!FeatureFlags.enabled(FeatureFlag.STORY_GRAPH) || milestone == null) return;
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        boolean changed = false;
        for (final CompanionArc arc : arcsFor(milestone)) {
            if (board.arcs().add(arc, milestone)) {
                changed = true;
                final long day = player.level().getDayTime() / 24000L;
                board.addMemory(new MemoryRecord(MemoryType.ARC, day, arc.title() + " recorded: " + human(milestone), 76));
                CompanionLifecycleService.findForOwner(player, arc.role())
                        .ifPresent(companion -> DialogueService.get().speak(companion, "arc_reflection", 4));
            }
        }
        if (changed) data.markChanged();
    }

    public static String summary(final TeamBlackboard board) {
        final CompanionArc hook = board.arcs().activeHook();
        if (hook == null) return "No optional companion arc hook is active.";
        return hook.title() + " — " + board.arcs().count(hook) + "/4 observed milestones; it can be deferred at any time.";
    }

    private static Set<CompanionArc> arcsFor(final ArcMilestone milestone) {
        return switch (milestone) {
            case RETREAT_SUCCEEDED, TEAM_SAFE_RETURN, PLAN_DECLINED_SAFELY -> EnumSet.of(CompanionArc.HOPPER_SAFE_WAY_BACK);
            case SAFE_ROUTE_MARKED -> EnumSet.of(CompanionArc.HOPPER_SAFE_WAY_BACK, CompanionArc.MAX_ROUTE_WORTH_TAKING);
            case ANOMALY_DOCUMENTED, EVIDENCE_COMPARED, CAUTIOUS_OBSERVATION, WARNING_SUPPORTED_DECISION -> EnumSet.of(CompanionArc.WILL_PATTERN_CHANGES);
            case RESCUE_SUCCEEDED, POWER_POLICY_RESPECTED, SHIELD_SAFE_EXIT, NONPOWER_PLAN_SUCCEEDED -> EnumSet.of(CompanionArc.ELEVEN_CHOICE_NOT_WEAPON);
            case SCOUT_SUCCEEDED, ROUTE_ANNOTATION_HELPED, SAFER_ROUTE_CHOSEN, ROUTE_PAIR_PLAN_SUCCEEDED -> EnumSet.of(CompanionArc.MAX_ROUTE_WORTH_TAKING);
        };
    }

    private static String human(final ArcMilestone milestone) {
        return milestone.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }
}
