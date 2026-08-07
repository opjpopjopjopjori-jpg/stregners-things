package com.riftcompanions.interrupt;

/** Mirrors team-event urgency without allowing ambient content to interrupt safety work. */
public enum ActionInterruptPriority {
    P0_FATAL,
    P1_IMMEDIATE,
    P2_PLAYER_COMMAND,
    P3_PLAN,
    P4_AMBIENT
}
