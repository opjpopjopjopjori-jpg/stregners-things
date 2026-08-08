package com.riftcompanions.team.events;

/** Lower numeric value means more urgent; mirrors P0–P4 in the Design Bible. */
public enum TeamEventPriority {
    P0_FATAL_EMERGENCY(0),
    P1_IMMEDIATE_COMBAT(1),
    P2_PLAN_STATE(2),
    P3_DISCOVERY(3),
    P4_MEMORY_AMBIENT(4);

    private final int weight;

    TeamEventPriority(final int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
