package com.riftcompanions.performance;

import com.riftcompanions.config.CompanionConfig;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lightweight, opt-in timing telemetry for the local developer overlay. It
 * keeps a tiny rolling summary in memory only; it never writes player data,
 * sends analytics, or pretends to be a Spark benchmark.
 */
public final class CompanionPerformanceMonitor {
    private static final int ROLLING_SAMPLE_LIMIT = 60;
    private static final Map<UUID, OwnerSamples> SAMPLES = new HashMap<>();

    private CompanionPerformanceMonitor() {}

    public static long start() {
        return enabled() ? System.nanoTime() : 0L;
    }

    public static void record(final UUID owner, final PerformanceWorkType type, final long startedAtNanos) {
        if (!enabled() || owner == null || type == null || startedAtNanos <= 0L) return;
        final long micros = Math.max(0L, Math.min(1_000_000L, (System.nanoTime() - startedAtNanos) / 1_000L));
        SAMPLES.computeIfAbsent(owner, ignored -> new OwnerSamples()).record(type, micros);
    }

    public static Snapshot snapshot(final UUID owner) {
        if (!enabled() || owner == null) return Snapshot.disabled();
        return SAMPLES.getOrDefault(owner, new OwnerSamples()).snapshot();
    }

    public static void clear(final UUID owner) {
        if (owner != null) SAMPLES.remove(owner);
    }

    public static boolean enabled() {
        return CompanionConfig.DEVELOPER_DIAGNOSTICS_ENABLED.get();
    }

    private static final class OwnerSamples {
        private final EnumMap<PerformanceWorkType, RollingSample> entries = new EnumMap<>(PerformanceWorkType.class);

        private void record(final PerformanceWorkType type, final long micros) {
            entries.computeIfAbsent(type, ignored -> new RollingSample()).record(micros);
        }

        private Snapshot snapshot() {
            long latestTotal = 0L;
            long averageTotal = 0L;
            for (final RollingSample sample : entries.values()) {
                latestTotal += sample.latestMicros;
                averageTotal += sample.averageMicros;
            }
            final RollingSample companion = entries.getOrDefault(PerformanceWorkType.COMPANION_AI, new RollingSample());
            final RollingSample navigation = entries.getOrDefault(PerformanceWorkType.NAVIGATION, new RollingSample());
            final RollingSample director = entries.getOrDefault(PerformanceWorkType.TEAM_DIRECTOR, new RollingSample());
            final RollingSample dialogue = entries.getOrDefault(PerformanceWorkType.DIALOGUE, new RollingSample());
            return new Snapshot(true, latestTotal, averageTotal,
                    companion.latestMicros, companion.averageMicros,
                    navigation.latestMicros, navigation.averageMicros,
                    director.latestMicros, director.averageMicros,
                    dialogue.latestMicros, dialogue.averageMicros);
        }
    }

    private static final class RollingSample {
        private long latestMicros;
        private long averageMicros;
        private int count;

        private void record(final long micros) {
            latestMicros = micros;
            if (count < ROLLING_SAMPLE_LIMIT) {
                averageMicros = (averageMicros * count + micros) / (count + 1L);
                count++;
            } else {
                averageMicros = (averageMicros * 7L + micros) / 8L;
            }
        }
    }

    public record Snapshot(
            boolean enabled,
            long sampledTotalMicros,
            long averageTotalMicros,
            long latestCompanionMicros,
            long averageCompanionMicros,
            long latestNavigationMicros,
            long averageNavigationMicros,
            long latestDirectorMicros,
            long averageDirectorMicros,
            long latestDialogueMicros,
            long averageDialogueMicros
    ) {
        public static Snapshot disabled() {
            return new Snapshot(false, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }
    }
}
