package com.riftcompanions.server;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.story.StoryService;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionLifecycle;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.GiftedCompanionEntity;
import com.riftcompanions.entity.GuardianCompanionEntity;
import com.riftcompanions.entity.ScoutCompanionEntity;
import com.riftcompanions.entity.SeerCompanionEntity;
import com.riftcompanions.registry.ModEntities;
import com.riftcompanions.transaction.ActionPhase;
import com.riftcompanions.transaction.ActionTransaction;
import com.riftcompanions.transaction.ActionType;
import com.riftcompanions.navigation.CompanionNavigationService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.UUID;

/** Central lifecycle transaction service: all summon/recall/dismiss commands use this class. */
public final class CompanionLifecycleService {
    private CompanionLifecycleService() {}

    public static LifecycleResult summonOrRecall(final ServerPlayer owner, final CompanionRole role) {
        final MinecraftServer server = owner.server;
        final TeamSavedData data = TeamSavedData.get(server);
        StoryService.initialize(owner);
        if (CompanionConfig.STORY_UNLOCKS_ENABLED.get() && !StoryService.isRoleUnlocked(owner, role)) {
            return LifecycleResult.failure("ROLE_STORY_LOCKED");
        }
        final Optional<UUID> knownActive = data.activeEntityUuid(owner.getUUID(), role);
        final Optional<CompoundTag> restingSnapshot = data.restingSnapshot(owner.getUUID(), role);
        if (restingSnapshot.isPresent() && !canRestoreRestingSnapshot(owner, data, restingSnapshot.get())) {
            return LifecycleResult.failure("RESTING_COMPANION_REQUIRES_ORIGINAL_SAFE_BASE");
        }
        final long now = owner.level().getGameTime();
        final ActionType lifecycleAction = knownActive.isPresent() ? ActionType.COMPANION_RECALL
                : restingSnapshot.isPresent() ? ActionType.COMPANION_RESTORE : ActionType.COMPANION_SPAWN;
        final var transactionBegin = data.blackboard(owner.getUUID()).actionLedger().begin(
                lifecycleAction, "lifecycle:" + role.id() + ":" + now, role.id(), now, 160L);
        if (transactionBegin.reused()) return LifecycleResult.failure("LIFECYCLE_ACTION_ALREADY_PENDING");
        final ActionTransaction transaction = transactionBegin.transaction();
        if (transaction.phase() == ActionPhase.PREPARED) transaction.reserve("LIFECYCLE_REQUEST_VALIDATED");
        if (knownActive.isPresent()) {
            if (!CompanionConfig.SAFE_RECALL_ENABLED.get()) {
                return lifecycleFailure(data, transaction, now, "SAFE_RECALL_DISABLED");
            }
            final Optional<CompanionEntity> loaded = findLoaded(server, knownActive.get());
            if (loaded.isPresent()) {
                final CompanionEntity companion = loaded.get();
                if (!companion.isOwnedBy(owner)) {
                    return lifecycleFailure(data, transaction, now, "ROSTER_OWNER_MISMATCH");
                }
                if (companion.getCompanionState() == com.riftcompanions.entity.CompanionState.DOWNED) {
                    return lifecycleFailure(data, transaction, now, "DOWNED_COMPANION_REQUIRES_RESCUE");
                }
                if (!SafeTeleport.recallNearPlayer(companion, owner, 5)) {
                    return lifecycleFailure(data, transaction, now, "NO_SAFE_RECALL_POSITION");
                }
                return lifecycleSuccess(data, transaction, now, "RECALLED_EXISTING", companion);
            }
            // Never manufacture a second entity merely because the original is
            // in an unloaded chunk. This is the duplicate-prevention contract.
            return lifecycleFailure(data, transaction, now, "ACTIVE_COMPANION_NOT_CURRENTLY_LOADED");
        }

        final long activeCount = data.snapshot(owner.getUUID()).values().stream()
                .filter(entry -> entry.lifecycle() == CompanionLifecycle.ACTIVE || entry.lifecycle() == CompanionLifecycle.DOWNED)
                .count();
        if (activeCount >= CompanionConfig.MAX_ACTIVE_COMPANIONS.get()) {
            return lifecycleFailure(data, transaction, now, "ACTIVE_SQUAD_LIMIT_REACHED");
        }

        final ServerLevel level = owner.serverLevel();
        final CompanionEntity companion = create(role, level);
        // The reservation is claimed before insertion so a second summon/recall
        // in the same command burst cannot choose the identical safe square.
        final var destination = SafeTeleport.reserveCompanionSpawnSpot(level, companion, owner, owner.blockPosition(), 5);
        if (destination.isEmpty()) {
            return lifecycleFailure(data, transaction, now, "NO_SAFE_SPAWN_POSITION");
        }
        companion.moveTo(destination.get().getX() + 0.5D, destination.get().getY(), destination.get().getZ() + 0.5D, owner.getYRot(), 0.0F);
        companion.setOwner(owner);
        companion.setLastSafeWaypoint(destination.get());
        restingSnapshot.ifPresent(companion::restoreRestingSnapshot);
        if (!level.addFreshEntity(companion)) {
            SafeTeleport.releaseCompanionReservation(owner.getUUID(), companion.getUUID());
            return lifecycleFailure(data, transaction, now, "ENTITY_SPAWN_REJECTED");
        }
        if (restingSnapshot.isPresent()) {
            data.activateResting(owner.getUUID(), role, companion.getUUID());
        } else {
            data.setLifecycle(owner.getUUID(), role, CompanionLifecycle.ACTIVE, companion.getUUID());
        }
        StoryService.onFirstCompanionActive(owner);
        final LifecycleResult result = lifecycleSuccess(data, transaction, now,
                restingSnapshot.isPresent() ? "RESTORED_FROM_RESTING" : "SPAWNED", companion);
        com.riftcompanions.onboarding.OnboardingService.onFirstCompanionActive(owner, role);
        return result;
    }

    public static LifecycleResult recall(final ServerPlayer owner, final CompanionRole role) {
        final TeamSavedData data = TeamSavedData.get(owner.server);
        final long now = owner.level().getGameTime();
        final var transactionBegin = data.blackboard(owner.getUUID()).actionLedger().begin(ActionType.COMPANION_RECALL,
                "recall:" + role.id() + ":" + now, role.id(), now, 120L);
        if (transactionBegin.reused()) return LifecycleResult.failure("LIFECYCLE_ACTION_ALREADY_PENDING");
        final ActionTransaction transaction = transactionBegin.transaction();
        if (transaction.phase() == ActionPhase.PREPARED) transaction.reserve("SAFE_RECALL_REQUEST_VALIDATED");
        if (!CompanionConfig.SAFE_RECALL_ENABLED.get()) return lifecycleFailure(data, transaction, now, "SAFE_RECALL_DISABLED");
        final Optional<CompanionEntity> companion = findForOwner(owner, role);
        if (companion.isEmpty()) return lifecycleFailure(data, transaction, now, "NO_LOADED_COMPANION");
        if (companion.get().getCompanionState() == com.riftcompanions.entity.CompanionState.DOWNED) {
            return lifecycleFailure(data, transaction, now, "DOWNED_COMPANION_REQUIRES_RESCUE");
        }
        if (!SafeTeleport.recallNearPlayer(companion.get(), owner, 5)) {
            return lifecycleFailure(data, transaction, now, "NO_SAFE_RECALL_POSITION");
        }
        return lifecycleSuccess(data, transaction, now, "RECALLED", companion.get());
    }

    public static LifecycleResult dismiss(final ServerPlayer owner, final CompanionRole role) {
        final TeamSavedData data = TeamSavedData.get(owner.server);
        final long now = owner.level().getGameTime();
        final var transactionBegin = data.blackboard(owner.getUUID()).actionLedger().begin(ActionType.COMPANION_DISMISS,
                "dismiss:" + role.id() + ":" + now, role.id(), now, 160L);
        if (transactionBegin.reused()) return LifecycleResult.failure("LIFECYCLE_ACTION_ALREADY_PENDING");
        final ActionTransaction transaction = transactionBegin.transaction();
        if (transaction.phase() == ActionPhase.PREPARED) transaction.reserve("DISMISS_REQUEST_VALIDATED");
        final Optional<CompanionEntity> loaded = findForOwner(owner, role);
        loaded.ifPresent(companion -> {
            SafeTeleport.releaseCompanionReservation(owner.getUUID(), companion.getUUID());
            CompanionInventoryService.returnAllToPlayer(owner, companion);
            CompanionNavigationService.clearEntity(companion.getUUID());
            com.riftcompanions.presentation.CompanionGaitPresentationService.clearEntity(companion.getUUID());
            CompanionPresentationSoundService.clearEntity(companion.getUUID());
            GuardianBraceService.clearGuardian(companion.getUUID());
            companion.discard();
        });
        final Optional<CompoundTag> restingSnapshot = data.dismiss(owner.getUUID(), role);
        restingSnapshot.ifPresent(snapshot -> returnRestingSnapshotToPlayer(owner, snapshot));
        return lifecycleSuccess(data, transaction, now,
                loaded.isPresent() ? "DISMISSED_ITEMS_RETURNED" : restingSnapshot.isPresent() ? "DISMISSED_RESTING_ITEMS_RETURNED" : "DISMISSED_UNLOADED_RECORD",
                loaded.orElse(null));
    }

    /**
     * Safe-base duo switching uses RESTING rather than normal dismissal. The
     * bounded personal state is persisted server-side first, then the live
     * entity is discarded so it cannot keep chunks loaded or act off-screen.
     */
    public static LifecycleResult restAtBase(final ServerPlayer owner, final CompanionRole role) {
        final TeamSavedData data = TeamSavedData.get(owner.server);
        final var board = data.blackboard(owner.getUUID());
        if (!board.isAtSafeBase(owner) || board.plan().isOpen()) {
            return LifecycleResult.failure("RESTING_REQUIRES_SAFE_BASE_WITHOUT_PLAN");
        }
        final long now = owner.level().getGameTime();
        final Optional<CompanionEntity> loaded = findForOwner(owner, role);
        if (loaded.isEmpty()) return LifecycleResult.failure("NO_LOADED_COMPANION_TO_REST");
        final CompanionEntity companion = loaded.get();
        if (companion.getCompanionState() == com.riftcompanions.entity.CompanionState.DOWNED) {
            return LifecycleResult.failure("DOWNED_COMPANION_CANNOT_REST");
        }
        final var transactionBegin = data.blackboard(owner.getUUID()).actionLedger().begin(ActionType.COMPANION_REST,
                "rest:" + role.id() + ":" + now, role.id(), now, 160L);
        if (transactionBegin.reused()) return LifecycleResult.failure("LIFECYCLE_ACTION_ALREADY_PENDING");
        final ActionTransaction transaction = transactionBegin.transaction();
        if (transaction.phase() == ActionPhase.PREPARED) transaction.reserve("RESTING_SNAPSHOT_VALIDATED");
        final CompoundTag snapshot = companion.createRestingSnapshot();
        data.suspendAtBase(owner.getUUID(), role, snapshot);
        companion.getNavigation().stop();
        companion.setTarget(null);
        SafeTeleport.releaseCompanionReservation(owner.getUUID(), companion.getUUID());
        companion.setCompanionState(com.riftcompanions.entity.CompanionState.RESTING, "SAFE_BASE_DUO_REST");
        CompanionNavigationService.clearEntity(companion.getUUID());
        com.riftcompanions.presentation.CompanionGaitPresentationService.clearEntity(companion.getUUID());
        CompanionPresentationSoundService.clearEntity(companion.getUUID());
        GuardianBraceService.clearGuardian(companion.getUUID());
        companion.discard();
        return lifecycleSuccess(data, transaction, now, "RESTING_SNAPSHOT_COMMITTED", companion);
    }

    /** Prevents resting snapshots from becoming distance or cross-dimension storage. */
    private static boolean canRestoreRestingSnapshot(final ServerPlayer owner, final TeamSavedData data, final CompoundTag snapshot) {
        if (owner == null || data == null || snapshot == null || !snapshot.contains("RestingDimension")) return false;
        if (!owner.level().dimension().location().toString().equals(snapshot.getString("RestingDimension"))) return false;
        return data.blackboard(owner.getUUID()).isAtSafeBase(owner);
    }

    private static void returnRestingSnapshotToPlayer(final ServerPlayer owner, final CompoundTag snapshot) {
        for (final net.minecraft.world.item.ItemStack stack : com.riftcompanions.resource.CompanionInventory.drainRestingSnapshot(snapshot)) {
            if (!owner.getInventory().add(stack)) owner.drop(stack, false);
        }
    }

    private static LifecycleResult lifecycleSuccess(final TeamSavedData data, final ActionTransaction transaction,
                                                    final long now, final String code, final CompanionEntity companion) {
        if (transaction.phase() == ActionPhase.RESERVED) transaction.applied("LIFECYCLE_EFFECT_CONFIRMED");
        if (transaction.phase() == ActionPhase.APPLIED) transaction.commit(now, code);
        data.markChanged();
        return LifecycleResult.success(code, companion);
    }

    private static LifecycleResult lifecycleFailure(final TeamSavedData data, final ActionTransaction transaction,
                                                    final long now, final String code) {
        transaction.rollback(now, code);
        data.markChanged();
        return LifecycleResult.failure(code);
    }

    public static Optional<CompanionEntity> findForOwner(final ServerPlayer owner, final CompanionRole role) {
        final Optional<UUID> uuid = TeamSavedData.get(owner.server).activeEntityUuid(owner.getUUID(), role);
        return uuid.flatMap(id -> findLoaded(owner.server, id)).filter(companion -> companion.isOwnedBy(owner));
    }

    public static Optional<CompanionEntity> findLoaded(final MinecraftServer server, final UUID entityUuid) {
        for (final ServerLevel level : server.getAllLevels()) {
            final Entity entity = level.getEntity(entityUuid);
            if (entity instanceof CompanionEntity companion) {
                return Optional.of(companion);
            }
        }
        return Optional.empty();
    }

    public static void acceptEntityLoad(final CompanionEntity companion) {
        if (!(companion.level() instanceof ServerLevel level)) {
            return;
        }
        companion.getOwnerUuid().ifPresent(owner -> {
            final boolean accepted = TeamSavedData.get(level.getServer()).acceptLoadedEntity(owner, companion.getRole(), companion.getUUID());
            if (!accepted) {
                // A different active UUID already exists. Discard this duplicate
                // instead of overwriting a valid roster record.
                companion.discard();
            }
        });
    }

    private static CompanionEntity create(final CompanionRole role, final ServerLevel level) {
        return switch (role) {
            case SEER -> new SeerCompanionEntity(ModEntities.SEER.get(), level);
            case GUARDIAN -> new GuardianCompanionEntity(ModEntities.GUARDIAN.get(), level);
            case GIFTED -> new GiftedCompanionEntity(ModEntities.GIFTED.get(), level);
            case SCOUT -> new ScoutCompanionEntity(ModEntities.SCOUT.get(), level);
        };
    }

    public record LifecycleResult(boolean successful, String code, CompanionEntity companion) {
        public static LifecycleResult success(final String code, final CompanionEntity companion) {
            return new LifecycleResult(true, code, companion);
        }

        public static LifecycleResult failure(final String code) {
            return new LifecycleResult(false, code, null);
        }
    }
}
