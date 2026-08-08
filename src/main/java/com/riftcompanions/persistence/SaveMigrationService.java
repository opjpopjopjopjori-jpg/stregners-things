package com.riftcompanions.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Conservative in-place migrations for source versions known to this mod. A
 * newer unknown save is never force-parsed as current data; callers enter Safe
 * Mode and present a readable warning instead.
 */
public final class SaveMigrationService {
    private SaveMigrationService() {}

    public static MigrationResult migrateTeamRoot(final CompoundTag source) {
        final CompoundTag tag = source == null ? new CompoundTag() : source.copy();
        final int from = tag.contains("DataVersion", Tag.TAG_INT) ? tag.getInt("DataVersion") : 1;
        final List<String> warnings = new ArrayList<>();
        if (from > SaveVersions.TEAM_DATA) {
            warnings.add("Save data is newer than this source version (" + from + ").");
            return new MigrationResult(false, tag, from, SaveVersions.TEAM_DATA, List.copyOf(warnings));
        }
        try {
            if (!tag.contains("Owners", Tag.TAG_LIST)) {
                tag.put("Owners", new ListTag());
                warnings.add("Missing owner roster was replaced with an empty safe roster.");
            }
            final ListTag owners = tag.getList("Owners", Tag.TAG_COMPOUND);
            for (int i = 0; i < owners.size(); i++) {
                final CompoundTag owner = owners.getCompound(i);
                if (!owner.contains("Companions", Tag.TAG_LIST)) owner.put("Companions", new ListTag());
                if (!owner.contains("Blackboard", Tag.TAG_COMPOUND)) owner.put("Blackboard", new CompoundTag());
                final CompoundTag board = owner.getCompound("Blackboard");
                if (!board.contains("BlackboardDataVersion", Tag.TAG_INT)) board.putInt("BlackboardDataVersion", 1);
                // Version 17 added durable transactions/reservations. Missing lists intentionally mean empty.
                if (!board.contains("ActionLedger", Tag.TAG_COMPOUND)) board.put("ActionLedger", new CompoundTag());
                if (!board.contains("Reservations", Tag.TAG_COMPOUND)) board.put("Reservations", new CompoundTag());
                // Version 18 added readable Safe Mode state. Missing means disabled, not guessed enabled.
                if (!board.contains("SafeMode", Tag.TAG_COMPOUND)) board.put("SafeMode", new CompoundTag());
                // Version 19 adds fault circuit-breaker records. Missing means no historical fault evidence.
                if (!board.contains("FaultRecords", Tag.TAG_LIST)) board.put("FaultRecords", new ListTag());
                if (!board.contains("PlayerPolicy", Tag.TAG_COMPOUND)) board.put("PlayerPolicy", new CompoundTag());
                if (!board.contains("PlayerIdentity", Tag.TAG_COMPOUND)) board.put("PlayerIdentity", new CompoundTag());
                if (!board.contains("Consequences", Tag.TAG_LIST)) board.put("Consequences", new ListTag());
                if (!board.contains("Perceptions", Tag.TAG_LIST)) board.put("Perceptions", new ListTag());
                if (!board.contains("SceneState", Tag.TAG_COMPOUND)) board.put("SceneState", new CompoundTag());
                if (!board.contains("PlaytestTelemetry", Tag.TAG_COMPOUND)) board.put("PlaytestTelemetry", new CompoundTag());
                if (!board.contains("TeamSupply", Tag.TAG_COMPOUND)) board.put("TeamSupply", new CompoundTag());
                if (!board.contains("MindAnchor", Tag.TAG_COMPOUND)) board.put("MindAnchor", new CompoundTag());
                if (!board.contains("EncounterContext", Tag.TAG_COMPOUND)) board.put("EncounterContext", new CompoundTag());
                if (!board.contains("WillAwareness", Tag.TAG_COMPOUND)) board.put("WillAwareness", new CompoundTag());
                // Version 20 adds bounded Gifted readiness and Guardian review state. Missing data starts neutral.
                if (!board.contains("GiftedBehavior", Tag.TAG_COMPOUND)) board.put("GiftedBehavior", new CompoundTag());
                if (!board.contains("GuardianReview", Tag.TAG_COMPOUND)) board.put("GuardianReview", new CompoundTag());
                // Version 21 adds optional tutorial progress. Missing data means no hint was delivered yet.
                if (!board.contains("Onboarding", Tag.TAG_COMPOUND)) board.put("Onboarding", new CompoundTag());
                if (!board.contains("DecisionState", Tag.TAG_COMPOUND)) board.put("DecisionState", new CompoundTag());
                // Version 23 retires custom Rift Operation encounters. Any legacy operation record is discarded rather than resumed.
                board.remove("RiftOperation");
                final ListTag companions = owner.getList("Companions", Tag.TAG_COMPOUND);
                for (int j = 0; j < companions.size(); j++) {
                    final CompoundTag companion = companions.getCompound(j);
                    if (!companion.contains("RosterDataVersion", Tag.TAG_INT)) companion.putInt("RosterDataVersion", 1);
                    // A missing RESTING snapshot remains an empty, safe resting state; no item is manufactured.
                }
                board.putInt("BlackboardDataVersion", SaveVersions.BLACKBOARD_DATA);
            }
            tag.putInt("DataVersion", SaveVersions.TEAM_DATA);
            return new MigrationResult(true, tag, from, SaveVersions.TEAM_DATA, List.copyOf(warnings));
        } catch (final RuntimeException exception) {
            warnings.add("Migration exception: " + exception.getClass().getSimpleName());
            return new MigrationResult(false, tag, from, SaveVersions.TEAM_DATA, List.copyOf(warnings));
        }
    }

    public static MigrationResult migrateCompanion(final CompoundTag source) {
        final CompoundTag tag = source == null ? new CompoundTag() : source.copy();
        final int from = tag.contains("CompanionDataVersion", Tag.TAG_INT) ? tag.getInt("CompanionDataVersion") : 1;
        final List<String> warnings = new ArrayList<>();
        if (from > SaveVersions.COMPANION_DATA) {
            warnings.add("Companion data is newer than this source version (" + from + ").");
            return new MigrationResult(false, tag, from, SaveVersions.COMPANION_DATA, List.copyOf(warnings));
        }
        try {
            if (!tag.contains("PersonalInventory", Tag.TAG_LIST)) tag.put("PersonalInventory", new ListTag());
            if (!tag.contains("CombatEnabled", Tag.TAG_BYTE)) tag.putBoolean("CombatEnabled", true);
            tag.putInt("CompanionDataVersion", SaveVersions.COMPANION_DATA);
            return new MigrationResult(true, tag, from, SaveVersions.COMPANION_DATA, List.copyOf(warnings));
        } catch (final RuntimeException exception) {
            warnings.add("Companion migration exception: " + exception.getClass().getSimpleName());
            return new MigrationResult(false, tag, from, SaveVersions.COMPANION_DATA, List.copyOf(warnings));
        }
    }
}
