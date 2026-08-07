package com.riftcompanions.team;

import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.persistence.SaveVersions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serializable plan contract. A plan has a durable action id and optional
 * player-visible target so recovery can cancel stale work without guessing.
 */
public final class TeamPlan {
    private final UUID actionId;
    private final PlanTarget target;
    private TeamPlanType type;
    private TeamPlanStatus status;
    private CompanionRole leader;
    private long startedAt;
    private long deadline;
    private String objective;
    private String planA;
    private String planB;
    private String abortCondition;
    private boolean requiresApproval;
    private final List<String> reasonCodes;
    private long pendingActiveTimeout;

    private TeamPlan(final UUID actionId, final PlanTarget target, final TeamPlanType type, final TeamPlanStatus status,
                     final CompanionRole leader, final long startedAt, final long deadline, final String objective,
                     final String planA, final String planB, final String abortCondition,
                     final boolean requiresApproval, final List<String> reasonCodes) {
        this.actionId = actionId == null ? UUID.randomUUID() : actionId;
        this.target = target;
        this.type = type;
        this.status = status;
        this.leader = leader;
        this.startedAt = Math.max(0L, startedAt);
        this.deadline = Math.max(0L, deadline);
        this.objective = trim(objective, 180);
        this.planA = trim(planA, 160);
        this.planB = trim(planB, 160);
        this.abortCondition = trim(abortCondition, 160);
        this.requiresApproval = requiresApproval;
        this.reasonCodes = new ArrayList<>(reasonCodes == null ? List.of() : reasonCodes.stream().limit(8).toList());
    }

    public static TeamPlan none() {
        return new TeamPlan(new UUID(0L, 0L), null, TeamPlanType.NONE, TeamPlanStatus.SUCCEEDED, CompanionRole.GUARDIAN,
                0L, 0L, "", "", "", "", false, List.of());
    }

    public static TeamPlan active(final TeamPlanType type, final CompanionRole leader, final long now,
                                  final long timeout, final String objective, final List<String> reasonCodes) {
        return new TeamPlan(UUID.randomUUID(), null, type, TeamPlanStatus.ACTIVE, leader, now, now + Math.max(20L, timeout), objective,
                "Execute the current role layout.", "Fall back to safe regroup.", "Timeout or new P0 emergency.", false, reasonCodes);
    }

    public static TeamPlan awaitingApproval(final TeamPlanType type, final CompanionRole leader, final long now,
                                            final long proposalLifetime, final long activeTimeout,
                                            final String objective, final String planA, final String planB,
                                            final String abortCondition, final List<String> reasonCodes) {
        return awaitingApprovalAt(type, leader, now, proposalLifetime, activeTimeout, objective, planA, planB, abortCondition, reasonCodes, null);
    }

    public static TeamPlan awaitingApprovalAt(final TeamPlanType type, final CompanionRole leader, final long now,
                                              final long proposalLifetime, final long activeTimeout,
                                              final String objective, final String planA, final String planB,
                                              final String abortCondition, final List<String> reasonCodes,
                                              final PlanTarget target) {
        final TeamPlan plan = new TeamPlan(UUID.randomUUID(), target, type, TeamPlanStatus.AWAITING_APPROVAL, leader, now,
                now + Math.max(40L, proposalLifetime), objective, planA, planB, abortCondition, true, reasonCodes);
        plan.pendingActiveTimeout = Math.max(40L, activeTimeout);
        return plan;
    }

    public UUID actionId() { return actionId; }
    public Optional<PlanTarget> target() { return Optional.ofNullable(target); }
    public boolean hasValidTarget(final net.minecraft.server.level.ServerPlayer player) { return target == null || target.isValidFor(player); }
    public boolean isActive() { return status == TeamPlanStatus.ACTIVE; }
    public boolean awaitsApproval() { return status == TeamPlanStatus.AWAITING_APPROVAL; }
    public boolean isOpen() { return isActive() || awaitsApproval() || status == TeamPlanStatus.DRAFT; }
    public boolean timedOut(final long gameTime) { return isOpen() && gameTime >= deadline; }

    public boolean activate(final long now) {
        if (!awaitsApproval()) return false;
        this.status = TeamPlanStatus.ACTIVE;
        this.startedAt = Math.max(0L, now);
        this.deadline = now + Math.max(40L, pendingActiveTimeout == 0L ? 240L : pendingActiveTimeout);
        return true;
    }

    /** Terminal transitions are idempotent: a completed plan cannot be finished twice. */
    public boolean finish(final TeamPlanStatus finalStatus) {
        if (finalStatus == null || finalStatus == TeamPlanStatus.ACTIVE || finalStatus == TeamPlanStatus.DRAFT
                || finalStatus == TeamPlanStatus.AWAITING_APPROVAL || !isOpen()) return false;
        this.status = finalStatus;
        return true;
    }

    public TeamPlanType type() { return type; }
    public TeamPlanStatus status() { return status; }
    public CompanionRole leader() { return leader; }
    public long startedAt() { return startedAt; }
    public long deadline() { return deadline; }
    public String objective() { return objective; }
    public String planA() { return planA; }
    public String planB() { return planB; }
    public String abortCondition() { return abortCondition; }
    public boolean requiresApproval() { return requiresApproval; }
    public List<String> reasonCodes() { return List.copyOf(reasonCodes); }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("PlanDataVersion", SaveVersions.PLAN_DATA);
        tag.putUUID("ActionId", actionId);
        tag.putString("Type", type.name());
        tag.putString("Status", status.name());
        tag.putString("Leader", leader.name());
        tag.putLong("StartedAt", startedAt);
        tag.putLong("Deadline", deadline);
        tag.putLong("PendingActiveTimeout", pendingActiveTimeout);
        tag.putString("Objective", objective);
        tag.putString("PlanA", planA);
        tag.putString("PlanB", planB);
        tag.putString("AbortCondition", abortCondition);
        tag.putBoolean("RequiresApproval", requiresApproval);
        if (target != null) tag.put("Target", target.save());
        final ListTag reasons = new ListTag();
        reasonCodes.stream().limit(8).forEach(reason -> reasons.add(StringTag.valueOf(reason)));
        tag.put("Reasons", reasons);
        return tag;
    }

    public static TeamPlan load(final CompoundTag tag) {
        try {
            final TeamPlanType type = TeamPlanType.valueOf(tag.getString("Type"));
            final TeamPlanStatus status = TeamPlanStatus.valueOf(tag.getString("Status"));
            final CompanionRole leader = CompanionRole.valueOf(tag.getString("Leader"));
            final List<String> reasons = new ArrayList<>();
            final ListTag list = tag.getList("Reasons", Tag.TAG_STRING);
            for (int i = 0; i < list.size() && i < 8; i++) reasons.add(list.getString(i));
            final UUID actionId = tag.hasUUID("ActionId") ? tag.getUUID("ActionId") : legacyActionId(type, status, tag.getLong("StartedAt"), tag.getLong("Deadline"), tag.getString("Objective"));
            final PlanTarget target = tag.contains("Target", Tag.TAG_COMPOUND) ? PlanTarget.load(tag.getCompound("Target")).orElse(null) : null;
            final TeamPlan plan = new TeamPlan(actionId, target, type, status, leader, tag.getLong("StartedAt"), tag.getLong("Deadline"),
                    tag.getString("Objective"), tag.getString("PlanA"), tag.getString("PlanB"), tag.getString("AbortCondition"),
                    tag.getBoolean("RequiresApproval"), reasons);
            plan.pendingActiveTimeout = tag.getLong("PendingActiveTimeout");
            return plan;
        } catch (final IllegalArgumentException exception) {
            return none();
        }
    }

    private static UUID legacyActionId(final TeamPlanType type, final TeamPlanStatus status, final long start,
                                       final long deadline, final String objective) {
        final String seed = "legacy-plan|" + type + '|' + status + '|' + start + '|' + deadline + '|' + objective;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private static String trim(final String value, final int maximum) {
        if (value == null) return "";
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
