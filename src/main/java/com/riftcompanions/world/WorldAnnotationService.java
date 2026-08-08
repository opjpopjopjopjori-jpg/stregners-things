package com.riftcompanions.world;

import com.riftcompanions.doctrine.PlayerHabitService;
import com.riftcompanions.doctrine.TeamDoctrine;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.story.StoryService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.intention.IntentionService;
import com.riftcompanions.arc.ArcMilestone;
import com.riftcompanions.arc.ArcService;
import com.riftcompanions.duo.DuoDynamicsService;
import com.riftcompanions.duo.DuoSynergy;
import com.riftcompanions.consequence.ConsequenceService;
import com.riftcompanions.consequence.ConsequenceType;
import com.riftcompanions.scene.SetPieceService;
import com.riftcompanions.scene.SetPieceType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server-derived look target annotations; a client cannot force arbitrary chunks to load. */
public final class WorldAnnotationService {
    private static final double MARK_RANGE = 12.0D;

    private WorldAnnotationService() {}

    public static AnnotationResult markLookTarget(final ServerPlayer player, final WorldAnnotationType type) {
        final Vec3 start = player.getEyePosition();
        final Vec3 end = start.add(player.getViewVector(1.0F).scale(MARK_RANGE));
        final BlockHitResult hit = player.serverLevel().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        final BlockPos target = hit.getType() == HitResult.Type.MISS ? player.blockPosition() : hit.getBlockPos();
        if (!player.serverLevel().hasChunkAt(target)) {
            return AnnotationResult.failure("ANNOTATION_CHUNK_UNLOADED", "A distant chunk will not be loaded for an annotation.");
        }
        final TeamSavedData data = TeamSavedData.get(player.server);
        final WorldAnnotation annotation = new WorldAnnotation(type, player.level().dimension().location(), target, player.level().getGameTime() / 24000L);
        data.blackboard(player.getUUID()).addAnnotation(annotation);
        data.blackboard(player.getUUID()).addMemory(new MemoryRecord(MemoryType.MILESTONE, annotation.createdDay(),
                "Player marked " + type + " at " + target.getX() + ", " + target.getY() + ", " + target.getZ(), 80));
        data.markChanged();
        final ConsequenceType consequenceType = switch (type) {
            case SAFE_ROUTE, DANGER -> ConsequenceType.TACTICAL;
            case PROTECTED, MACHINE_NO_GO -> ConsequenceType.WORLD;
            case INVESTIGATE -> ConsequenceType.STORY;
        };
        ConsequenceService.record(player, consequenceType, "annotation:" + type.name() + ":" + target.asLong(),
                "Player marked " + type + "; the team will treat this location as a visible shared decision.", 78);
        IntentionService.observeCompletion(player, "ANNOTATION_" + type.name());
        if (type == WorldAnnotationType.SAFE_ROUTE) {
            PlayerHabitService.observe(player, TeamDoctrine.MARK_SAFE_ROUTES);
            StoryService.onSafeRouteMarked(player);
            StoryService.completePromiseByKey(player, "ANNOTATION_SAFE_ROUTE");
            ArcService.observe(player, ArcMilestone.SAFE_ROUTE_MARKED);
            ArcService.observe(player, ArcMilestone.ROUTE_ANNOTATION_HELPED);
            DuoDynamicsService.noteSynergy(player, DuoSynergy.MARKED_RETREAT_PATH);
        }
        if (type == WorldAnnotationType.INVESTIGATE) {
            StoryService.onInvestigationMarked(player, target);
            StoryService.completePromiseByKey(player, "ANNOTATION_INVESTIGATE");
            ArcService.observe(player, ArcMilestone.EVIDENCE_COMPARED);
            SetPieceService.request(player, SetPieceType.LANDMARK_MEMORY, 600L);
        }
        return AnnotationResult.success("ANNOTATION_ADDED", "Added " + type + " at the selected location.");
    }

    public static AnnotationResult removeNearestLookTarget(final ServerPlayer player) {
        final TeamSavedData data = TeamSavedData.get(player.server);
        final WorldAnnotation removed = data.blackboard(player.getUUID()).removeNearestAnnotation(player.level().dimension().location(), player.blockPosition(), 16.0D);
        if (removed == null) {
            return AnnotationResult.failure("NO_NEARBY_ANNOTATION", "No nearby annotation is available to remove.");
        }
        data.markChanged();
        return AnnotationResult.success("ANNOTATION_REMOVED", "Removed " + removed.type() + ".");
    }

    public record AnnotationResult(boolean successful, String code, String detail) {
        public static AnnotationResult success(final String code, final String detail) { return new AnnotationResult(true, code, detail); }
        public static AnnotationResult failure(final String code, final String detail) { return new AnnotationResult(false, code, detail); }
    }
}
