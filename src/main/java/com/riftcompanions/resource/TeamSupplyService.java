package com.riftcompanions.resource;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.transaction.ActionPhase;
import com.riftcompanions.transaction.ActionTransaction;
import com.riftcompanions.transaction.ActionType;
import com.riftcompanions.transaction.ReservationType;
import com.riftcompanions.world.BaseAnchorType;
import com.riftcompanions.world.TeamAnchor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Explicit player-bound Team Supply container access. It is disabled by default
 * and cannot discover, open, or withdraw from any arbitrary player/structure chest.
 */
public final class TeamSupplyService {
    private static final double BIND_RANGE = 6.0D;

    private TeamSupplyService() {}

    public static Result bindLookingAtContainer(final ServerPlayer player) {
        if (player == null || player.server == null) return Result.failure("TEAM_SUPPLY_SERVER_UNAVAILABLE", "No logical server is available.");
        if (!CompanionConfig.TEAM_SUPPLY_ENABLED.get()) return Result.failure("TEAM_SUPPLY_DISABLED", "Team Supply is disabled in the world settings.");
        final var board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        if (!board.isAtSafeBase(player)) return Result.failure("TEAM_SUPPLY_REQUIRES_SAFE_BASE", "Bind Team Supply only while standing at a safe HOME or REST anchor.");
        final Vec3 start = player.getEyePosition();
        final Vec3 end = start.add(player.getViewVector(1.0F).scale(BIND_RANGE));
        final BlockHitResult hit = player.serverLevel().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS || !player.serverLevel().hasChunkAt(hit.getBlockPos())) {
            return Result.failure("TEAM_SUPPLY_LOOK_AT_CONTAINER", "Look at a nearby loaded container to bind Team Supply.");
        }
        final BlockEntity entity = player.serverLevel().getBlockEntity(hit.getBlockPos());
        if (!(entity instanceof Container)) {
            return Result.failure("TEAM_SUPPLY_TARGET_NOT_CONTAINER", "That block is not a container eligible for explicit Team Supply binding.");
        }
        final long day = player.level().getDayTime() / 24000L;
        board.teamSupply().bind(player.level().dimension().location(), hit.getBlockPos(), day);
        board.setAnchor(BaseAnchorType.TEAM_SUPPLY, new TeamAnchor(player.level().dimension().location(), hit.getBlockPos(), day));
        TeamSavedData.get(player.server).markChanged();
        return Result.success("TEAM_SUPPLY_BOUND", "Bound Team Supply to the selected container. Only whitelist categories and daily caps can be withdrawn.");
    }

    public static Result withdrawOne(final ServerPlayer player, final CompanionRole role) {
        if (player == null || player.server == null || role == null) return Result.failure("TEAM_SUPPLY_CONTEXT_INVALID", "Choose a valid companion role.");
        if (!CompanionConfig.TEAM_SUPPLY_ENABLED.get()) return Result.failure("TEAM_SUPPLY_DISABLED", "Team Supply is disabled in the world settings.");
        final TeamSavedData data = TeamSavedData.get(player.server);
        final var board = data.blackboard(player.getUUID());
        final TeamSupplyState supply = board.teamSupply();
        if (!board.isAtSafeBase(player) || !supply.isBound()) return Result.failure("TEAM_SUPPLY_NOT_AVAILABLE", "Team Supply requires a bound container at a safe HOME or REST anchor.");
        final BlockPos position = supply.position().orElse(null);
        if (position == null || supply.dimension().isEmpty() || !supply.dimension().get().equals(player.level().dimension().location())
                || !player.serverLevel().hasChunkAt(position) || player.blockPosition().distSqr(position) > 12.0D * 12.0D) {
            return Result.failure("TEAM_SUPPLY_OUT_OF_RANGE_OR_UNLOADED", "The bound Team Supply container is not safely available here.");
        }
        final CompanionEntity companion = CompanionLifecycleService.findForOwner(player, role).orElse(null);
        if (companion == null || companion.distanceToSqr(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D) > 14.0D * 14.0D) {
            return Result.failure("TEAM_SUPPLY_COMPANION_NOT_NEAR_BASE", "The companion must be loaded and near the Team Supply area.");
        }
        final BlockEntity blockEntity = player.serverLevel().getBlockEntity(position);
        if (!(blockEntity instanceof Container container)) return Result.failure("TEAM_SUPPLY_CONTAINER_CHANGED", "The bound Team Supply block is no longer a container.");
        final long now = player.level().getGameTime();
        final long day = player.level().getDayTime() / 24000L;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            final net.minecraft.world.item.ItemStack source = container.getItem(slot);
            final ItemCategory category = ItemValueClassifier.classify(source);
            if (source.isEmpty() || !supply.canWithdraw(role, category, day)) continue;
            final var begun = board.actionLedger().begin(ActionType.TEAM_SUPPLY_WITHDRAW,
                    "team-supply:" + role.id() + ":" + day + ":" + slot, category.name(), now, 120L);
            if (begun.reused()) return Result.failure("TEAM_SUPPLY_ACTION_PENDING", "A Team Supply withdrawal is already pending confirmation.");
            final ActionTransaction transaction = begun.transaction();
            if (transaction.phase() == ActionPhase.PREPARED) transaction.reserve("TEAM_SUPPLY_WHITELIST_VALIDATED");
            if (!board.reservations().claim(ReservationType.ANCHOR, position.asLong() + ":" + role.id(), transaction.actionId(), now, 80L)) {
                transaction.rollback(now, "TEAM_SUPPLY_ANCHOR_RESERVED");
                return Result.failure("TEAM_SUPPLY_ANCHOR_RESERVED", "Another approved supply action is using this anchor briefly.");
            }
            final int before = source.getCount();
            if (!companion.getPersonalInventory().insertOneFrom(source) || source.getCount() != before - 1) {
                board.reservations().release(ReservationType.ANCHOR, position.asLong() + ":" + role.id());
                transaction.rollback(now, "TEAM_SUPPLY_TRANSFER_REJECTED");
                return Result.failure("TEAM_SUPPLY_TRANSFER_REJECTED", "The approved item could not be transferred safely.");
            }
            container.setChanged();
            supply.recordWithdraw(role, day);
            if (transaction.phase() == ActionPhase.RESERVED) transaction.applied("TEAM_SUPPLY_INVENTORY_CONFIRMED");
            if (transaction.phase() == ActionPhase.APPLIED) transaction.commit(now, "TEAM_SUPPLY_WITHDRAW_COMMITTED");
            board.reservations().release(ReservationType.ANCHOR, position.asLong() + ":" + role.id());
            board.addMemory(new MemoryRecord(MemoryType.MILESTONE, day,
                    role.personalName() + " received one approved " + category + " item from Team Supply.", 80));
            data.markChanged();
            return Result.success("TEAM_SUPPLY_WITHDRAWN", "Transferred one approved " + category + " item to " + role.personalName() + ".");
        }
        return Result.failure("TEAM_SUPPLY_NO_APPROVED_ITEM", "No approved, non-rare item within the daily cap is available for this companion.");
    }

    public static Result status(final ServerPlayer player, final CompanionRole role) {
        if (player == null || player.server == null || role == null) return Result.failure("TEAM_SUPPLY_CONTEXT_INVALID", "Choose a valid companion role.");
        final TeamSupplyState supply = TeamSavedData.get(player.server).blackboard(player.getUUID()).teamSupply();
        if (!supply.isBound()) return Result.failure("TEAM_SUPPLY_NOT_BOUND", "No Team Supply container is bound.");
        final long day = player.level().getDayTime() / 24000L;
        return Result.success("TEAM_SUPPLY_STATUS", role.personalName() + " used " + supply.withdrawnToday(role, day)
                + "/" + CompanionResourceProfile.dailyTeamSupplyCap(role) + " daily approved supply item(s).");
    }

    public record Result(boolean successful, String code, String detail) {
        public static Result success(final String code, final String detail) { return new Result(true, code, detail); }
        public static Result failure(final String code, final String detail) { return new Result(false, code, detail); }
    }
}
