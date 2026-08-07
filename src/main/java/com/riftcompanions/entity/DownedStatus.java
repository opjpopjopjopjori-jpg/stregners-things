package com.riftcompanions.entity;

/** Player-visible rescue context; DOWNED state remains the authoritative AI state. */
public enum DownedStatus {
    STABLE,
    DANGER,
    UNREACHABLE,
    RESCUING,
    RECOVERING
}
