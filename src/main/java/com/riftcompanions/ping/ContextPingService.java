package com.riftcompanions.ping;

import com.riftcompanions.server.FocusTargetService;
import com.riftcompanions.team.TeamDirector;
import com.riftcompanions.world.WorldAnnotationService;
import com.riftcompanions.world.WorldAnnotationType;
import net.minecraft.server.level.ServerPlayer;

/** Server-derived contextual pings. No client block position or entity UUID is trusted. */
public final class ContextPingService {
    private ContextPingService() {}

    public static PingResult ping(final ServerPlayer player, final PingType type) {
        return switch (type) {
            case TARGET -> fromFocus(FocusTargetService.focusLookTarget(player));
            case ROUTE -> fromAnnotation(WorldAnnotationService.markLookTarget(player, WorldAnnotationType.SAFE_ROUTE));
            case DANGER -> fromAnnotation(WorldAnnotationService.markLookTarget(player, WorldAnnotationType.DANGER));
            case INVESTIGATE -> fromPlan(TeamDirector.requestStructureEntry(player));
            case ITEM -> fromAnnotation(WorldAnnotationService.markLookTarget(player, WorldAnnotationType.INVESTIGATE));
        };
    }

    private static PingResult fromFocus(final FocusTargetService.FocusResult result) {
        return result.successful() ? PingResult.success(result.code(), result.detail()) : PingResult.failure(result.code(), result.detail());
    }

    private static PingResult fromAnnotation(final WorldAnnotationService.AnnotationResult result) {
        return result.successful() ? PingResult.success(result.code(), result.detail()) : PingResult.failure(result.code(), result.detail());
    }

    private static PingResult fromPlan(final com.riftcompanions.team.TeamPlanService.PlanResult result) {
        return result.successful() ? PingResult.success(result.code(), result.detail()) : PingResult.failure(result.code(), result.detail());
    }

    public record PingResult(boolean successful, String code, String detail) {
        public static PingResult success(String c,String d){return new PingResult(true,c,d);}
        public static PingResult failure(String c,String d){return new PingResult(false,c,d);}
    }
}
