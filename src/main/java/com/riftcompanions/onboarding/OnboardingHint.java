package com.riftcompanions.onboarding;

/**
 * Short, one-time English guidance moments. These are operational explanations,
 * not quests, rewards, score gates, or forced cutscenes.
 */
public enum OnboardingHint {
    FIRST_COMPANION(
            "First Companion",
            "Use Follow, Hold, Recall, and the Team Journal first. Companions do not break blocks, open private containers, or replace your decisions."),
    FIRST_PLAN_PROPOSAL(
            "Plan Approval",
            "A plan is only a proposal until you accept it. Read Plan A, Plan B, and the abort condition before deciding."),
    FIRST_STUCK_RECOVERY(
            "Stuck Recovery",
            "A companion cancelled an unsafe route. Use Reset Task or Safe Recall instead of expecting block breaking or door use."),
    FIRST_SAFE_MODE(
            "Safe Mode",
            "Stop All Actions keeps Follow, Recall, and the Journal available while it pauses plans, powers, and automatic combat."),
    WORLD_RULE_POLICY(
            "Vanilla Rule Policy",
            "World rules change companion pressure, not player ownership. Peaceful, Creative, and Spectator suppress survival initiative; block edits remain disabled.");

    private final String title;
    private final String detail;

    OnboardingHint(final String title, final String detail) {
        this.title = title;
        this.detail = detail;
    }

    public String title() { return title; }
    public String detail() { return detail; }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
