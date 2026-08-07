package com.riftcompanions.playtest;

import com.riftcompanions.server.TeamSavedData;
import net.minecraft.server.level.ServerPlayer;

/** Local acceptance data and Go-rule summary for manual playtest campaigns. */
public final class PlaytestTelemetryService {
    private PlaytestTelemetryService() {}

    public static Result record(final ServerPlayer player, final AcceptanceAxis axis, final int score) {
        if (player == null || player.server == null || axis == null || score < 1 || score > 5) {
            return Result.failure("PLAYTEST_RATING_INVALID", "Choose a valid axis and a score from 1 through 5.");
        }
        final var state = TeamSavedData.get(player.server).blackboard(player.getUUID()).playtestTelemetry();
        state.record(axis, score, player.level().getDayTime() / 24000L);
        TeamSavedData.get(player.server).markChanged();
        return Result.success("PLAYTEST_RATING_RECORDED", axis + " recorded as " + score + "/5. " + summary(state));
    }

    public static Result status(final ServerPlayer player) {
        if (player == null || player.server == null) return Result.failure("PLAYTEST_SERVER_UNAVAILABLE", "No logical server is available.");
        return Result.success("PLAYTEST_STATUS", summary(TeamSavedData.get(player.server).blackboard(player.getUUID()).playtestTelemetry()));
    }

    public static String summary(final PlaytestTelemetryState state) {
        final StringBuilder value = new StringBuilder(state.goRulePasses() ? "Go rule: PASS." : "Go rule: HOLD; an axis is below 3/5.");
        for (final AcceptanceAxis axis : AcceptanceAxis.values()) {
            if (state.count(axis) > 0) value.append(' ').append(axis).append('=').append(String.format(java.util.Locale.ROOT, "%.2f", state.average(axis))).append('/').append('5');
        }
        return value.toString();
    }

    public record Result(boolean successful, String code, String detail) {
        public static Result success(final String code, final String detail) { return new Result(true, code, detail); }
        public static Result failure(final String code, final String detail) { return new Result(false, code, detail); }
    }
}
