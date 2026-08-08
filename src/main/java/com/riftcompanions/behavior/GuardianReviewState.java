package com.riftcompanions.behavior;

import com.riftcompanions.team.TeamPlan;
import com.riftcompanions.team.TeamPlanStatus;
import com.riftcompanions.team.TeamPlanType;
import net.minecraft.nbt.CompoundTag;

/**
 * A compact after-action review for the Guardian role. It is a factual Journal
 * card, not a score, punishment, or forced conversation.
 */
public final class GuardianReviewState {
    private TeamPlanType planType = TeamPlanType.NONE;
    private TeamPlanStatus status = TeamPlanStatus.SUCCEEDED;
    private String summary = "No after-action review has been recorded.";
    private String reasonCode = "NONE";
    private long recordedAt;

    public TeamPlanType planType() { return planType; }
    public TeamPlanStatus status() { return status; }
    public String summary() { return summary; }
    public String reasonCode() { return reasonCode; }
    public long recordedAt() { return recordedAt; }

    public void record(final TeamPlan plan, final TeamPlanStatus outcome, final String nextSummary,
                       final String nextReason, final long now) {
        planType = plan == null ? TeamPlanType.NONE : plan.type();
        status = outcome == null ? TeamPlanStatus.FAILED_SAFE : outcome;
        summary = trim(nextSummary, 180, "The team returned to a safe follow state.");
        reasonCode = trim(nextReason, 96, "REVIEW_UNKNOWN");
        recordedAt = Math.max(0L, now);
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("PlanType", planType.name());
        tag.putString("Status", status.name());
        tag.putString("Summary", summary);
        tag.putString("Reason", reasonCode);
        tag.putLong("RecordedAt", recordedAt);
        return tag;
    }

    public static GuardianReviewState load(final CompoundTag tag) {
        final GuardianReviewState state = new GuardianReviewState();
        if (tag == null) return state;
        try {
            state.planType = TeamPlanType.valueOf(tag.getString("PlanType"));
        } catch (final IllegalArgumentException ignored) {
            state.planType = TeamPlanType.NONE;
        }
        try {
            state.status = TeamPlanStatus.valueOf(tag.getString("Status"));
        } catch (final IllegalArgumentException ignored) {
            state.status = TeamPlanStatus.FAILED_SAFE;
        }
        state.summary = trim(tag.getString("Summary"), 180, "No after-action review has been recorded.");
        state.reasonCode = trim(tag.getString("Reason"), 96, "REVIEW_UNKNOWN");
        state.recordedAt = Math.max(0L, tag.getLong("RecordedAt"));
        return state;
    }

    private static String trim(final String value, final int maximum, final String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
