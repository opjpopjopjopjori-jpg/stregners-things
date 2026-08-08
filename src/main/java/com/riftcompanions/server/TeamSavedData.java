package com.riftcompanions.server;

import com.riftcompanions.entity.CompanionLifecycle;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.persistence.MigrationResult;
import com.riftcompanions.persistence.SaveMigrationService;
import com.riftcompanions.persistence.SaveVersions;
import com.riftcompanions.safety.SafeModeReason;
import com.riftcompanions.team.TeamBlackboard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * World-level roster. Entity NBT owns live state; this data owns the one
 * authoritative record per owner + role so commands cannot spawn duplicates.
 * RESTING snapshots are server-owned and exist only while an entity has been
 * deliberately suspended at a safe base during a duo change.
 */
public final class TeamSavedData extends SavedData {
    public static final String DATA_NAME = "riftcompanions_team";
    private static final int DATA_VERSION = SaveVersions.TEAM_DATA;

    private final Map<UUID, EnumMap<CompanionRole, RosterEntry>> rosters = new HashMap<>();
    private final Map<UUID, TeamBlackboard> blackboards = new HashMap<>();
    private boolean migrationFailure;
    private String migrationFailureDetail = "";
    private final Set<UUID> migrationSafetyApplied = new HashSet<>();

    public static TeamSavedData get(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TeamSavedData::load, TeamSavedData::new, DATA_NAME);
    }

    public static TeamSavedData load(final CompoundTag tag) {
        final MigrationResult migration = SaveMigrationService.migrateTeamRoot(tag);
        final CompoundTag migrated = migration.tag();
        final TeamSavedData data = new TeamSavedData();
        data.migrationFailure = !migration.successful();
        data.migrationFailureDetail = migration.summary();
        final ListTag owners = migrated.getList("Owners", Tag.TAG_COMPOUND);
        for (int i = 0; i < owners.size(); i++) {
            final CompoundTag ownerTag = owners.getCompound(i);
            if (!ownerTag.hasUUID("Owner")) continue;
            final UUID owner = ownerTag.getUUID("Owner");
            final EnumMap<CompanionRole, RosterEntry> entries = new EnumMap<>(CompanionRole.class);
            final ListTag companionList = ownerTag.getList("Companions", Tag.TAG_COMPOUND);
            for (int j = 0; j < companionList.size(); j++) {
                final CompoundTag entryTag = companionList.getCompound(j);
                try {
                    final CompanionRole role = CompanionRole.valueOf(entryTag.getString("Role"));
                    final CompanionLifecycle lifecycle = CompanionLifecycle.valueOf(entryTag.getString("Lifecycle"));
                    final UUID entity = entryTag.hasUUID("Entity") ? entryTag.getUUID("Entity") : null;
                    final CompoundTag resting = entryTag.contains("RestingSnapshot", Tag.TAG_COMPOUND)
                            ? entryTag.getCompound("RestingSnapshot").copy() : null;
                    entries.put(role, new RosterEntry(lifecycle, entity, resting));
                } catch (final IllegalArgumentException ignored) {
                    // Older or invalid content is skipped, never allowed to
                    // crash a world during loading.
                }
            }
            if (!entries.isEmpty()) data.rosters.put(owner, entries);
            if (ownerTag.contains("Blackboard", Tag.TAG_COMPOUND)) {
                data.blackboards.put(owner, TeamBlackboard.load(ownerTag.getCompound("Blackboard")));
                data.rosters.computeIfAbsent(owner, ignored -> new EnumMap<>(CompanionRole.class));
            }
        }
        return data;
    }

    /** Returns false if a different live ACTIVE or DOWNED UUID already owns this roster slot. */
    public boolean acceptLoadedEntity(final UUID owner, final CompanionRole role, final UUID entity) {
        if (owner == null || entity == null) return true;
        final RosterEntry existing = roster(owner).get(role);
        if (existing != null && (existing.lifecycle == CompanionLifecycle.ACTIVE || existing.lifecycle == CompanionLifecycle.DOWNED)
                && existing.entityUuid != null && !existing.entityUuid.equals(entity)) {
            return false;
        }
        // If a crash happened after a RESTING snapshot was written but before
        // the old entity discarded, the live entity wins and the snapshot is
        // cleared. This prevents a second copy from ever being restored. A
        // matching downed UUID keeps its roster lifecycle so rescue remains
        // addressable after entity reload.
        final CompanionLifecycle loadedLifecycle = existing != null && existing.lifecycle == CompanionLifecycle.DOWNED
                ? CompanionLifecycle.DOWNED : CompanionLifecycle.ACTIVE;
        roster(owner).put(role, new RosterEntry(loadedLifecycle, entity, null));
        setDirty();
        return true;
    }

    public void setLifecycle(final UUID owner, final CompanionRole role, final CompanionLifecycle lifecycle, final UUID entity) {
        if (owner == null || role == null || lifecycle == null) return;
        roster(owner).put(role, new RosterEntry(lifecycle, entity, null));
        setDirty();
    }

    /** Atomically records the server-owned state needed for a safe resting companion. */
    public void suspendAtBase(final UUID owner, final CompanionRole role, final CompoundTag snapshot) {
        if (owner == null || role == null || snapshot == null) return;
        roster(owner).put(role, new RosterEntry(CompanionLifecycle.RESTING, null, snapshot));
        setDirty();
    }

    /** Clears the snapshot only after a restored live entity was accepted. */
    public void activateResting(final UUID owner, final CompanionRole role, final UUID entity) {
        if (owner == null || role == null || entity == null) return;
        roster(owner).put(role, new RosterEntry(CompanionLifecycle.ACTIVE, entity, null));
        setDirty();
    }

    public Optional<CompoundTag> restingSnapshot(final UUID owner, final CompanionRole role) {
        return entry(owner, role).flatMap(RosterEntry::restingSnapshot);
    }

    /**
     * Normal dismissal consumes any resting snapshot so its items can be
     * returned exactly once to the owner. The caller owns the returned copy.
     */
    public Optional<CompoundTag> dismiss(final UUID owner, final CompanionRole role) {
        if (owner == null || role == null) return Optional.empty();
        final RosterEntry old = roster(owner).get(role);
        final CompoundTag snapshot = old == null ? null : old.restingSnapshot.orElse(null);
        roster(owner).put(role, new RosterEntry(CompanionLifecycle.DISMISSED, null, null));
        setDirty();
        return snapshot == null ? Optional.empty() : Optional.of(snapshot.copy());
    }

    public Optional<RosterEntry> entry(final UUID owner, final CompanionRole role) {
        if (owner == null || role == null) return Optional.empty();
        return Optional.ofNullable(roster(owner).get(role));
    }

    /**
     * Returns the one live UUID for an ACTIVE or DOWNED role. A downed entity is
     * still physically present and must remain addressable for player rescue,
     * diagnostics, duplicate prevention, and safe dismissal; it is not a
     * license to spawn a replacement.
     */
    public Optional<UUID> activeEntityUuid(final UUID owner, final CompanionRole role) {
        return entry(owner, role)
                .filter(entry -> (entry.lifecycle == CompanionLifecycle.ACTIVE || entry.lifecycle == CompanionLifecycle.DOWNED)
                        && entry.entityUuid != null)
                .map(entry -> entry.entityUuid);
    }

    public Map<CompanionRole, RosterEntry> snapshot(final UUID owner) {
        return Map.copyOf(roster(owner));
    }

    /** All shared plan/memory state lives in one per-player board, not duplicated in entity NBT. */
    public TeamBlackboard blackboard(final UUID owner) {
        roster(owner); // ensure an otherwise-empty board is persisted with this owner
        final TeamBlackboard board = blackboards.computeIfAbsent(owner, ignored -> new TeamBlackboard());
        if (migrationFailure && migrationSafetyApplied.add(owner) && !board.safeMode().enabled()) {
            board.safeMode().enable(SafeModeReason.DATA_MIGRATION_FAILED, migrationFailureDetail, 0L);
        }
        return board;
    }

    public boolean hasMigrationFailure() { return migrationFailure; }
    public String migrationFailureDetail() { return migrationFailureDetail; }

    public void markChanged() {
        setDirty();
    }

    private EnumMap<CompanionRole, RosterEntry> roster(final UUID owner) {
        return rosters.computeIfAbsent(owner, ignored -> new EnumMap<>(CompanionRole.class));
    }

    @Override
    public CompoundTag save(final CompoundTag tag) {
        tag.putInt("DataVersion", DATA_VERSION);
        if (migrationFailure && !migrationFailureDetail.isBlank()) tag.putString("LastMigrationWarning", migrationFailureDetail);
        final ListTag owners = new ListTag();
        for (final Map.Entry<UUID, EnumMap<CompanionRole, RosterEntry>> ownerEntry : rosters.entrySet()) {
            final CompoundTag ownerTag = new CompoundTag();
            ownerTag.putUUID("Owner", ownerEntry.getKey());
            final ListTag companionList = new ListTag();
            for (final Map.Entry<CompanionRole, RosterEntry> entry : ownerEntry.getValue().entrySet()) {
                final CompoundTag value = new CompoundTag();
                value.putInt("RosterDataVersion", SaveVersions.ROSTER_DATA);
                value.putString("Role", entry.getKey().name());
                value.putString("Lifecycle", entry.getValue().lifecycle.name());
                if (entry.getValue().entityUuid != null) value.putUUID("Entity", entry.getValue().entityUuid);
                entry.getValue().restingSnapshot.ifPresent(snapshot -> value.put("RestingSnapshot", snapshot.copy()));
                companionList.add(value);
            }
            ownerTag.put("Companions", companionList);
            final TeamBlackboard blackboard = blackboards.get(ownerEntry.getKey());
            if (blackboard != null) ownerTag.put("Blackboard", blackboard.save());
            owners.add(ownerTag);
        }
        tag.put("Owners", owners);
        return tag;
    }

    public static final class RosterEntry {
        private final CompanionLifecycle lifecycle;
        private final UUID entityUuid;
        private final Optional<CompoundTag> restingSnapshot;

        public RosterEntry(final CompanionLifecycle lifecycle, final UUID entityUuid, final CompoundTag restingSnapshot) {
            this.lifecycle = lifecycle == null ? CompanionLifecycle.UNAVAILABLE : lifecycle;
            this.entityUuid = entityUuid;
            this.restingSnapshot = restingSnapshot == null || restingSnapshot.isEmpty()
                    ? Optional.empty() : Optional.of(restingSnapshot.copy());
        }

        public CompanionLifecycle lifecycle() { return lifecycle; }
        public UUID entityUuid() { return entityUuid; }
        public Optional<CompoundTag> restingSnapshot() { return restingSnapshot.map(CompoundTag::copy); }
        public boolean hasRestingSnapshot() { return restingSnapshot.isPresent(); }
    }
}
