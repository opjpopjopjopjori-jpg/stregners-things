package com.riftcompanions.persistence;

/** Central data-version contract. Never delete a legacy field without a migration path. */
public final class SaveVersions {
    public static final int TEAM_DATA = 23;
    public static final int BLACKBOARD_DATA = 19;
    public static final int PLAN_DATA = 4;
    public static final int COMPANION_DATA = 4;
    public static final int INVENTORY_DATA = 2;
    public static final int ROSTER_DATA = 2;
    public static final int RESTING_SNAPSHOT_DATA = 1;
    public static final int MEMORY_DATA = 2;
    public static final int JOURNAL_DATA = 2;
    public static final int DOCTRINE_DATA = 2;
    public static final int CONFIG_VERSION = 16;

    private SaveVersions() {}
}
