package com.riftcompanions.world.assessment;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Explainable local environment score, not visual guessing or a full world scan. */
public record EnvironmentRiskAssessment(ResourceLocation biomeId, int score, Band band, List<String> reasonCodes) {
    public enum Band { SAFE, CAUTION, DANGEROUS, HIGH_RISK, CRITICAL }

    public EnvironmentRiskAssessment {
        score = Math.max(0, Math.min(100, score));
        reasonCodes = List.copyOf(reasonCodes == null ? List.of() : reasonCodes.stream().limit(8).toList());
    }

    public static Band bandFor(final int score) {
        if (score >= 80) return Band.CRITICAL;
        if (score >= 60) return Band.HIGH_RISK;
        if (score >= 40) return Band.DANGEROUS;
        if (score >= 20) return Band.CAUTION;
        return Band.SAFE;
    }
}
