package com.riftcompanions.client;

import com.riftcompanions.network.TeamStatusSnapshot;

/** Client cache is display-only; every decision remains server-authoritative. */
public final class ClientTeamState {
    private static volatile TeamStatusSnapshot snapshot = TeamStatusSnapshot.empty();
    private static volatile Feedback feedback = Feedback.none();
    private static volatile HiveLinkIndicator hiveLink = HiveLinkIndicator.none();

    private ClientTeamState() {}

    public static TeamStatusSnapshot snapshot() {
        return snapshot;
    }

    public static Feedback feedback() {
        final Feedback current = feedback;
        return current.expiresAtMillis() > System.currentTimeMillis() ? current : Feedback.none();
    }

    public static HiveLinkIndicator hiveLink() {
        final HiveLinkIndicator current = hiveLink;
        return current.expiresAtMillis() > System.currentTimeMillis() ? current : HiveLinkIndicator.none();
    }

    public static void acceptHiveLink(final int targetEntityId, final int modeId, final int remainingTicks,
                                      final int targetCount, final int targetLimit,
                                      final int controlLoadHearts, final int controlCapacityHearts,
                                      final String message) {
        final long duration = Math.max(1000L, remainingTicks * 50L + 500L);
        hiveLink = new HiveLinkIndicator(targetEntityId, modeId, Math.max(0, targetCount), Math.max(0, targetLimit),
                Math.max(0, controlLoadHearts), Math.max(0, controlCapacityHearts), remainingTicks,
                message == null ? "" : message, System.currentTimeMillis() + duration);
    }

    public static void acceptSnapshot(final TeamStatusSnapshot update) {
        snapshot = update == null ? TeamStatusSnapshot.empty() : update;
    }

    public static void acceptFeedback(final boolean success, final String code, final String detail) {
        feedback = new Feedback(success, code == null ? "" : code, detail == null ? "" : detail,
                System.currentTimeMillis() + 4200L);
    }

    public static void clear() {
        snapshot = TeamStatusSnapshot.empty();
        feedback = Feedback.none();
        hiveLink = HiveLinkIndicator.none();
    }

    public record Feedback(boolean success, String code, String detail, long expiresAtMillis) {
        static Feedback none() { return new Feedback(false, "", "", 0L); }
        public boolean active() { return expiresAtMillis > System.currentTimeMillis() && !detail.isBlank(); }
    }

    public record HiveLinkIndicator(int targetEntityId, int modeId, int targetCount, int targetLimit,
                                    int controlLoadHearts, int controlCapacityHearts, int remainingTicks,
                                    String message, long expiresAtMillis) {
        static HiveLinkIndicator none() { return new HiveLinkIndicator(-1, -1, 0, 0, 0, 0, 0, "", 0L); }
        public boolean active() { return expiresAtMillis > System.currentTimeMillis() && !message.isBlank(); }
        public int remainingTicksNow() { return Math.max(0, (int) Math.ceil((expiresAtMillis - System.currentTimeMillis()) / 50.0D)); }
        public int controlLoadPercent() {
            return controlCapacityHearts <= 0 ? 0 : Math.round(controlLoadHearts * 100.0F / controlCapacityHearts);
        }
    }
}
