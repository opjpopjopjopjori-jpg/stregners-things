package com.riftcompanions.debug;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.riftcompanions.RiftCompanions;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.team.TeamDirector;
import com.riftcompanions.team.events.TeamEventType;
import com.riftcompanions.safety.CompanionFaultService;
import com.riftcompanions.playtest.BehaviorTestCatalog;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Permission-2 developer test harness. It is not needed by ordinary players. */
@Mod.EventBusSubscriber(modid = RiftCompanions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DeveloperCommands {
    private DeveloperCommands() {}

    @SubscribeEvent
    public static void register(final RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("riftcompanions_dev")
                .requires(source -> source.hasPermission(2) && source.getServer().isSingleplayer())
                .then(Commands.literal("dump").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    context.getSource().sendSuccess(() -> Component.literal("[Rift Dev] " + DiagnosticsService.why(player, CompanionRole.GUARDIAN).detail()), false);
                    return 1;
                }))
                .then(Commands.literal("perf").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    final var performance = com.riftcompanions.performance.CompanionPerformanceMonitor.snapshot(player.getUUID());
                    context.getSource().sendSuccess(() -> Component.literal("[Rift Dev] timing=" + performance.enabled()
                            + " sampled_us=" + performance.sampledTotalMicros() + " avg_us=" + performance.averageTotalMicros()
                            + " ai_us=" + performance.latestCompanionMicros() + " nav_us=" + performance.latestNavigationMicros()
                            + " director_us=" + performance.latestDirectorMicros()), false);
                    return 1;
                }))
                .then(Commands.literal("event")
                        .then(Commands.argument("type", StringArgumentType.word()).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            try {
                                TeamEventType type = TeamEventType.valueOf(StringArgumentType.getString(context, "type").toUpperCase(java.util.Locale.ROOT));
                                TeamDirector.submit(player, type, java.util.List.of("DEV_FORCED_EVENT"));
                                context.getSource().sendSuccess(() -> Component.literal("[Rift Dev] queued " + type), false);
                                return 1;
                            } catch (IllegalArgumentException exception) {
                                context.getSource().sendFailure(Component.literal("[Rift Dev] unknown event type"));
                                return 0;
                            }
                        })))
                .then(Commands.literal("resource")
                        .then(Commands.argument("role", StringArgumentType.word())
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, 100.0F)).executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    CompanionRole role = CompanionRole.parse(StringArgumentType.getString(context, "role")).orElse(null);
                                    if (role == null) return 0;
                                    var companion = CompanionLifecycleService.findForOwner(player, role);
                                    if (companion.isEmpty()) return 0;
                                    float value = FloatArgumentType.getFloat(context, "value");
                                    switch (role) {
                                        case SEER -> companion.get().setHiveStrain(value);
                                        case SCOUT -> companion.get().setFocus(value);
                                        default -> companion.get().setEnergy(value);
                                    }
                                    return 1;
                                }))))
                .then(Commands.literal("chaos")
                        .then(Commands.argument("scenario", StringArgumentType.word()).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            try {
                                ChaosScenario scenario = ChaosScenario.valueOf(StringArgumentType.getString(context, "scenario").toUpperCase(java.util.Locale.ROOT));
                                ChaosTestService.ChaosBriefing briefing = ChaosTestService.briefing(player, scenario);
                                context.getSource().sendSuccess(() -> Component.literal("[Rift Dev] " + briefing.id() + ": " + briefing.expected() + " Current: " + briefing.state()), false);
                                return 1;
                            } catch (IllegalArgumentException exception) {
                                context.getSource().sendFailure(Component.literal("[Rift Dev] unknown chaos scenario"));
                                return 0;
                            }
                        })))
                .then(Commands.literal("behavior")
                        .then(Commands.argument("case_id", StringArgumentType.word()).executes(context -> {
                            final String id = StringArgumentType.getString(context, "case_id");
                            final var testCase = BehaviorTestCatalog.find(id).orElse(null);
                            if (testCase == null) {
                                context.getSource().sendFailure(Component.literal("[Rift Dev] unknown behavior case; see docs/BEHAVIOR_ACCEPTANCE_CATALOG.md"));
                                return 0;
                            }
                            context.getSource().sendSuccess(() -> Component.literal("[Rift Dev] " + testCase.id() + " | " + testCase.focus()
                                    + " | Setup: " + testCase.setup()), false);
                            context.getSource().sendSuccess(() -> Component.literal("[Rift Dev] Expected: " + testCase.expected()
                                    + " | Failure boundary: " + testCase.failureBoundary()), false);
                            return 1;
                        })))
                .then(Commands.literal("fault")
                        .then(Commands.argument("role", StringArgumentType.word()).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            CompanionRole role = CompanionRole.parse(StringArgumentType.getString(context, "role")).orElse(null);
                            if (role == null) return 0;
                            CompanionFaultService.recordInvalidWorldState(player, role, "DEV_CONTROLLED_FAULT");
                            context.getSource().sendSuccess(() -> Component.literal("[Rift Dev] recorded controlled fault for " + role.id()), false);
                            return 1;
                        })))
                .then(Commands.literal("force_stuck")
                        .then(Commands.argument("role", StringArgumentType.word()).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            CompanionRole role = CompanionRole.parse(StringArgumentType.getString(context, "role")).orElse(null);
                            if (role == null) return 0;
                            CompanionLifecycleService.findForOwner(player, role)
                                    .ifPresent(companion -> companion.setCompanionState(CompanionState.STUCK_RECOVERY, "DEV_FORCED_STUCK"));
                            return 1;
                        })))
        );
    }
}
