package com.riftcompanions.combat;

/** Bounded adapter result used by powers, plans, and threat messaging. */
public record EncounterProfile(
        String id,
        EncounterIdentity identity,
        boolean known,
        boolean hiveLinked,
        boolean boss,
        float pushMultiplier,
        int disruptTicks,
        int disruptAmplifier
) {
    public static EncounterProfile unknown(final String id) {
        return new EncounterProfile(id, EncounterIdentity.UNKNOWN, false, false, false, 1.0F, 0, 0);
    }

    public EncounterProfile {
        pushMultiplier = Math.max(0.0F, Math.min(2.0F, pushMultiplier));
        disruptTicks = Math.max(0, Math.min(200, disruptTicks));
        disruptAmplifier = Math.max(0, Math.min(4, disruptAmplifier));
    }
}
