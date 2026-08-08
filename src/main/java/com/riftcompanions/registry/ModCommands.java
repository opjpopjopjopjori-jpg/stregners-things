package com.riftcompanions.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.riftcompanions.RiftCompanions;
import com.riftcompanions.debug.DiagnosticsService;
import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.doctrine.DoctrineService;
import com.riftcompanions.doctrine.TeamDoctrine;
import com.riftcompanions.entity.CompanionAbility;
import com.riftcompanions.ping.ContextPingService;
import com.riftcompanions.ping.PingType;
import com.riftcompanions.story.StoryChapter;
import com.riftcompanions.story.StoryService;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.server.AbilityService;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.CompanionInventoryService;
import com.riftcompanions.server.FocusTargetService;
import com.riftcompanions.server.SafeTeleport;
import com.riftcompanions.server.RescueService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.server.TeamSafetyControlService;
import com.riftcompanions.team.TeamDirector;
import com.riftcompanions.team.TeamPlanService;
import com.riftcompanions.server.BaseAnchorService;
import com.riftcompanions.world.BaseAnchorType;
import com.riftcompanions.world.WorldAnnotationService;
import com.riftcompanions.world.WorldAnnotationType;
import com.riftcompanions.safety.SafeModeReason;
import com.riftcompanions.safety.SafeModeService;
import com.riftcompanions.intention.IntentionService;
import com.riftcompanions.onboarding.OnboardingService;
import com.riftcompanions.duo.DuoDynamicsService;
import com.riftcompanions.policy.PlayerPolicyService;
import com.riftcompanions.identity.PlayerIdentityService;
import com.riftcompanions.identity.PlayerPlayStyle;
import com.riftcompanions.playtest.AcceptanceAxis;
import com.riftcompanions.playtest.PlaytestTelemetryService;
import com.riftcompanions.resource.TeamSupplyService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Explicit, testable command surface for the initial source implementation. */
@Mod.EventBusSubscriber(modid = RiftCompanions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModCommands {
    private static final DynamicCommandExceptionType INVALID_ROLE = new DynamicCommandExceptionType(value -> Component.literal("Unknown companion role: " + value));
    private static final DynamicCommandExceptionType INVALID_ABILITY = new DynamicCommandExceptionType(value -> Component.literal("Unknown companion ability: " + value));
    private static final DynamicCommandExceptionType INVALID_ANCHOR = new DynamicCommandExceptionType(value -> Component.literal("Unknown anchor type: " + value));
    private static final DynamicCommandExceptionType INVALID_ANNOTATION = new DynamicCommandExceptionType(value -> Component.literal("Unknown annotation type: " + value));
    private static final DynamicCommandExceptionType INVALID_DOCTRINE = new DynamicCommandExceptionType(value -> Component.literal("Unknown doctrine: " + value));
    private static final DynamicCommandExceptionType INVALID_STORY_CHAPTER = new DynamicCommandExceptionType(value -> Component.literal("Unknown story chapter: " + value));
    private static final DynamicCommandExceptionType INVALID_PING = new DynamicCommandExceptionType(value -> Component.literal("Unknown ping type: " + value));

    private ModCommands() {}

    @SubscribeEvent
    public static void register(final RegisterCommandsEvent event) {
        final CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("companions")
                .requires(source -> source.hasPermission(2) && source.getServer().isSingleplayer())
                .then(Commands.literal("spawn")
                        .then(Commands.argument("role", StringArgumentType.word()).executes(context -> summon(context, role(context)))))
                .then(Commands.literal("follow").executes(ModCommands::followAll)
                        .then(Commands.argument("role", StringArgumentType.word()).executes(context -> setState(context, role(context), CompanionState.FOLLOWING))))
                .then(Commands.literal("regroup").executes(ModCommands::regroup))
                .then(Commands.literal("hold")
                        .then(Commands.argument("role", StringArgumentType.word()).executes(context -> hold(context, role(context)))))
                .then(Commands.literal("guard")
                        .then(Commands.argument("role", StringArgumentType.word()).executes(context -> setState(context, role(context), CompanionState.GUARDING))))
                .then(Commands.literal("focus").executes(ModCommands::focusTarget))
                .then(Commands.literal("ping")
                        .then(Commands.argument("ping_type", StringArgumentType.word()).executes(context -> contextPing(context, pingType(context)))))
                .then(Commands.literal("retreat").executes(ModCommands::retreatAll))
                .then(Commands.literal("cancel_plan").executes(ModCommands::cancelPlan))
                .then(Commands.literal("plan")
                        .then(Commands.literal("accept").executes(ModCommands::acceptPlan))
                        .then(Commands.literal("decline").executes(ModCommands::declinePlan)))
                .then(Commands.literal("structure").executes(ModCommands::checkStructure))
                .then(Commands.literal("recall").executes(ModCommands::recallAll)
                        .then(Commands.argument("role", StringArgumentType.word()).executes(context -> recall(context, role(context)))))
                .then(Commands.literal("dismiss")
                        .then(Commands.argument("role", StringArgumentType.word()).executes(context -> dismiss(context, role(context)))))
                .then(Commands.literal("dismiss_all").executes(ModCommands::dismissAll))
                .then(Commands.literal("unstuck")
                        .then(Commands.argument("role", StringArgumentType.word()).executes(context -> recall(context, role(context)))))
                .then(Commands.literal("reset_task")
                        .then(Commands.argument("role", StringArgumentType.word()).executes(context -> resetTask(context, role(context)))))
                .then(Commands.literal("stop_all_actions").executes(ModCommands::stopAllActions))
                .then(Commands.literal("ability")
                        .then(Commands.argument("ability", StringArgumentType.word()).executes(context -> ability(context, ability(context)))))
                .then(Commands.literal("inventory")
                        .then(Commands.literal("give")
                                .then(Commands.argument("role", StringArgumentType.word()).executes(context -> inventoryGive(context, role(context)))))
                        .then(Commands.literal("withdraw")
                                .then(Commands.argument("role", StringArgumentType.word())
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(0, 5)).executes(context -> inventoryWithdraw(context, role(context), IntegerArgumentType.getInteger(context, "slot"))))))
                        .then(Commands.literal("status")
                                .then(Commands.argument("role", StringArgumentType.word()).executes(context -> inventoryStatus(context, role(context))))))
                .then(Commands.literal("supply")
                        .then(Commands.literal("bind").executes(ModCommands::supplyBind))
                        .then(Commands.literal("withdraw")
                                .then(Commands.argument("role", StringArgumentType.word()).executes(context -> supplyWithdraw(context, role(context)))))
                        .then(Commands.literal("status")
                                .then(Commands.argument("role", StringArgumentType.word()).executes(context -> supplyStatus(context, role(context))))))
                .then(Commands.literal("combat")
                        .then(Commands.literal("on").executes(context -> combat(context, true)))
                        .then(Commands.literal("off").executes(context -> combat(context, false))))
                .then(Commands.literal("policy")
                        .then(Commands.literal("cycle")
                                .then(Commands.argument("role", StringArgumentType.word()).executes(context -> cyclePolicy(context, role(context)))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("role", StringArgumentType.word()).executes(context -> resetPolicy(context, role(context))))))
                .then(Commands.literal("identity")
                        .then(Commands.literal("nickname")
                                .then(Commands.argument("value", StringArgumentType.string()).executes(ModCommands::identityNickname)))
                        .then(Commands.literal("clear_nickname").executes(ModCommands::identityClearNickname))
                        .then(Commands.literal("style")
                                .then(Commands.argument("style", StringArgumentType.word()).executes(ModCommands::identityStyle)))
                        .then(Commands.literal("status").executes(ModCommands::identityStatus)))
                .then(Commands.literal("playtest")
                        .then(Commands.literal("rate")
                                .then(Commands.argument("axis", StringArgumentType.word())
                                        .then(Commands.argument("score", IntegerArgumentType.integer(1, 5)).executes(ModCommands::playtestRate))))
                        .then(Commands.literal("status").executes(ModCommands::playtestStatus)))
                .then(Commands.literal("safe_mode").executes(ModCommands::safeMode)
                        .then(Commands.literal("clear").executes(ModCommands::clearSafeMode)))
                .then(Commands.literal("tutorial")
                        .then(Commands.literal("status").executes(ModCommands::tutorialStatus))
                        .then(Commands.literal("dismiss").executes(ModCommands::tutorialDismiss))
                        .then(Commands.literal("resume").executes(ModCommands::tutorialResume)))
                .then(Commands.literal("intention")
                        .then(Commands.literal("accept").executes(ModCommands::intentionAccept))
                        .then(Commands.literal("defer").executes(ModCommands::intentionDefer))
                        .then(Commands.literal("status").executes(ModCommands::intentionStatus)))
                .then(Commands.literal("squad")
                        .then(Commands.literal("select")
                                .then(Commands.argument("first", StringArgumentType.word())
                                        .then(Commands.argument("second", StringArgumentType.word()).executes(context ->
                                                selectSquad(context, roleArgument(context, "first"), roleArgument(context, "second")))))))
                .then(Commands.literal("why")
                        .then(Commands.argument("role", StringArgumentType.word()).executes(context -> why(context, role(context)))))
                .then(Commands.literal("export_memory").executes(ModCommands::exportReport))
                .then(Commands.literal("repair_save").executes(ModCommands::repairSave))
                .then(Commands.literal("rescue").executes(ModCommands::rescueNearest))
                .then(Commands.literal("return_home").executes(ModCommands::returnHome))
                .then(Commands.literal("anchor")
                        .then(Commands.literal("set")
                                .then(Commands.argument("anchor_type", StringArgumentType.word()).executes(context -> setAnchor(context, anchorType(context)))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("anchor_type", StringArgumentType.word()).executes(context -> clearAnchor(context, anchorType(context))))))
                .then(Commands.literal("mark")
                        .then(Commands.argument("annotation_type", StringArgumentType.word()).executes(context -> markWorld(context, annotationType(context)))))
                .then(Commands.literal("unmark").executes(ModCommands::unmarkWorld))
                .then(Commands.literal("doctrine")
                        .then(Commands.literal("accept")
                                .then(Commands.argument("doctrine", StringArgumentType.word()).executes(context -> doctrineAccept(context, doctrine(context)))))
                        .then(Commands.literal("decline")
                                .then(Commands.argument("doctrine", StringArgumentType.word()).executes(context -> doctrineDecline(context, doctrine(context)))))
                        .then(Commands.literal("status")
                                .then(Commands.argument("doctrine", StringArgumentType.word()).executes(context -> doctrineStatus(context, doctrine(context))))))
                .then(Commands.literal("story")
                        .then(Commands.literal("start")
                                .then(Commands.argument("chapter", StringArgumentType.word()).executes(context -> storyStart(context, storyChapter(context)))))
                        .then(Commands.literal("defer")
                                .then(Commands.argument("chapter", StringArgumentType.word()).executes(context -> storyDefer(context, storyChapter(context)))))
                        .then(Commands.literal("status").executes(ModCommands::storyStatus)))
                .then(Commands.literal("mystery")
                        .then(Commands.literal("status").executes(ModCommands::mysteryStatus))
                        .then(Commands.literal("defer")
                                .then(Commands.argument("id", StringArgumentType.word()).executes(ModCommands::mysteryDefer))))
                .then(Commands.literal("promise")
                        .then(Commands.literal("accept").executes(ModCommands::promiseAccept))
                        .then(Commands.literal("defer").executes(ModCommands::promiseDefer))
                        .then(Commands.literal("status").executes(ModCommands::promiseStatus)))
                .then(Commands.literal("status").executes(ModCommands::status))
        );
    }

    private static int summon(final CommandContext<CommandSourceStack> context, final CompanionRole role) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final CompanionLifecycleService.LifecycleResult result = CompanionLifecycleService.summonOrRecall(player, role);
        reply(context.getSource(), result.successful(), role.personalName() + ": " + result.code());
        return result.successful() ? 1 : 0;
    }

    private static int recall(final CommandContext<CommandSourceStack> context, final CompanionRole role) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final CompanionLifecycleService.LifecycleResult result = CompanionLifecycleService.recall(player, role);
        reply(context.getSource(), result.successful(), role.personalName() + ": " + result.code());
        return result.successful() ? 1 : 0;
    }

    private static int recallAll(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        int recalled = 0;
        int rejected = 0;
        for (final CompanionRole role : CompanionRole.values()) {
            final CompanionLifecycleService.LifecycleResult result = CompanionLifecycleService.recall(player, role);
            if (result.successful()) recalled++;
            else if (!"NO_LOADED_COMPANION".equals(result.code())) rejected++;
        }
        reply(context.getSource(), recalled > 0, recalled > 0
                ? "RECALL_ALL: recalled=" + recalled + " rejected=" + rejected
                : "RECALL_ALL: no loaded companion had a valid safe recall destination");
        return recalled;
    }

    private static int dismiss(final CommandContext<CommandSourceStack> context, final CompanionRole role) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final CompanionLifecycleService.LifecycleResult result = CompanionLifecycleService.dismiss(player, role);
        reply(context.getSource(), true, role.personalName() + ": " + result.code());
        return 1;
    }

    private static int dismissAll(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final TeamSafetyControlService.Result result = TeamSafetyControlService.dismissAllExisting(context.getSource().getPlayerOrException());
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int resetTask(final CommandContext<CommandSourceStack> context, final CompanionRole role) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final TeamSafetyControlService.Result result = TeamSafetyControlService.resetTask(context.getSource().getPlayerOrException(), role);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int stopAllActions(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final TeamSafetyControlService.Result result = TeamSafetyControlService.stopAllActions(context.getSource().getPlayerOrException());
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int followAll(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        int touched = 0;
        for (final CompanionRole role : CompanionRole.values()) {
            final var companion = CompanionLifecycleService.findForOwner(player, role);
            if (companion.isPresent()) {
                companion.get().setCompanionState(CompanionState.FOLLOWING, "PLAYER_FOLLOW_ALL");
                touched++;
            }
        }
        reply(context.getSource(), touched > 0, "FOLLOW_ALL: " + touched);
        return touched;
    }

    private static int focusTarget(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final FocusTargetService.FocusResult result = FocusTargetService.focusLookTarget(player);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int contextPing(final CommandContext<CommandSourceStack> context, final PingType type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final ContextPingService.PingResult result = ContextPingService.ping(player, type);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int regroup(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        int touched = 0;
        for (final CompanionRole role : CompanionRole.values()) {
            final var companion = CompanionLifecycleService.findForOwner(player, role);
            if (companion.isPresent()) {
                companion.get().setCompanionState(CompanionState.FOLLOWING, "PLAYER_REGROUP_ORDER");
                touched++;
            }
        }
        CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                .ifPresent(companion -> DialogueService.get().speak(companion, "regroup", 1));
        reply(context.getSource(), touched > 0, "REGROUP: " + touched);
        return touched;
    }

    private static int retreatAll(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final boolean started = TeamPlanService.beginRetreat(player, "PLAYER_ORDERED_RETREAT");
        reply(context.getSource(), started, started ? "RETREAT_PLAN_ACTIVE" : "RETREAT_PLAN_ALREADY_ACTIVE_OR_NO_TEAM");
        return started ? 1 : 0;
    }

    private static int cancelPlan(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        TeamPlanService.cancel(player);
        reply(context.getSource(), true, "CURRENT_PLAN_CANCELLED");
        return 1;
    }

    private static int acceptPlan(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final TeamPlanService.PlanResult result = TeamPlanService.acceptCurrentPlan(player);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int declinePlan(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final TeamPlanService.PlanResult result = TeamPlanService.declineCurrentPlan(player);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int checkStructure(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final TeamPlanService.PlanResult result = TeamDirector.requestStructureEntry(player);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int hold(final CommandContext<CommandSourceStack> context, final CompanionRole role) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final var companion = CompanionLifecycleService.findForOwner(player, role);
        if (companion.isEmpty()) {
            reply(context.getSource(), false, role.personalName() + ": NO_LOADED_COMPANION");
            return 0;
        }
        final CompanionEntity entity = companion.get();
        if (!SafeTeleport.isSafeStanding(player.serverLevel(), entity, entity.blockPosition())) {
            entity.setCompanionState(CompanionState.FOLLOWING, "HOLD_LOCATION_UNSAFE");
            reply(context.getSource(), false, role.personalName() + ": HOLD_LOCATION_UNSAFE");
            return 0;
        }
        entity.setCompanionState(CompanionState.HOLDING, "PLAYER_HOLD_ORDER");
        reply(context.getSource(), true, role.personalName() + ": HOLDING");
        return 1;
    }

    private static int setState(final CommandContext<CommandSourceStack> context, final CompanionRole role, final CompanionState state) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final var companion = CompanionLifecycleService.findForOwner(player, role);
        if (companion.isEmpty()) {
            reply(context.getSource(), false, role.personalName() + ": NO_LOADED_COMPANION");
            return 0;
        }
        companion.get().setCompanionState(state, "PLAYER_COMMAND_" + state.name());
        reply(context.getSource(), true, role.personalName() + ": " + state.name());
        return 1;
    }

    private static int inventoryGive(final CommandContext<CommandSourceStack> context, final CompanionRole role) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final CompanionInventoryService.InventoryResult result = CompanionInventoryService.giveOneHeldItem(player, role);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int inventoryWithdraw(final CommandContext<CommandSourceStack> context, final CompanionRole role, final int slot) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final CompanionInventoryService.InventoryResult result = CompanionInventoryService.withdrawSlot(player, role, slot);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int inventoryStatus(final CommandContext<CommandSourceStack> context, final CompanionRole role) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final CompanionInventoryService.InventoryResult result = CompanionInventoryService.status(player, role);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int supplyBind(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final TeamSupplyService.Result result = TeamSupplyService.bindLookingAtContainer(context.getSource().getPlayerOrException());
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int supplyWithdraw(final CommandContext<CommandSourceStack> context, final CompanionRole role) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final TeamSupplyService.Result result = TeamSupplyService.withdrawOne(context.getSource().getPlayerOrException(), role);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int supplyStatus(final CommandContext<CommandSourceStack> context, final CompanionRole role) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final TeamSupplyService.Result result = TeamSupplyService.status(context.getSource().getPlayerOrException(), role);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int ability(final CommandContext<CommandSourceStack> context, final CompanionAbility ability) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final AbilityService.AbilityResult result = AbilityService.cast(player, ability);
        reply(context.getSource(), result.successful(), ability.id() + ": " + result.code());
        return result.successful() ? 1 : 0;
    }

    private static int combat(final CommandContext<CommandSourceStack> context, final boolean enabled) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        int touched = 0;
        for (final CompanionRole role : CompanionRole.values()) {
            final var companion = CompanionLifecycleService.findForOwner(player, role);
            if (companion.isPresent()) {
                companion.get().setCombatEnabled(enabled);
                touched++;
            }
        }
        reply(context.getSource(), touched > 0, "COMBAT_" + (enabled ? "ON" : "OFF") + ": " + touched);
        return touched;
    }

    private static int storyStart(final CommandContext<CommandSourceStack> context, final StoryChapter chapter) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final StoryService.StoryResult result = StoryService.start(player, chapter);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int storyDefer(final CommandContext<CommandSourceStack> context, final StoryChapter chapter) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final StoryService.StoryResult result = StoryService.defer(player, chapter);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int storyStatus(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        reply(context.getSource(), true, "STORY_STATUS: " + TeamSavedData.get(player.server).blackboard(player.getUUID()).story().snapshot());
        return 1;
    }

    private static int mysteryStatus(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        reply(context.getSource(), true, "MYSTERY: " + StoryService.mysterySummary(player));
        return 1;
    }

    private static int mysteryDefer(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final StoryService.StoryResult result = StoryService.deferMysteryClue(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "id"));
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int promiseAccept(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final StoryService.PromiseResult result = StoryService.acceptPromise(context.getSource().getPlayerOrException());
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int promiseDefer(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final StoryService.PromiseResult result = StoryService.deferPromise(context.getSource().getPlayerOrException());
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int promiseStatus(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        reply(context.getSource(), true, "PROMISE: " + StoryService.promiseSummary(player));
        return 1;
    }

    private static int doctrineAccept(final CommandContext<CommandSourceStack> context, final TeamDoctrine doctrine) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final DoctrineService.DoctrineResult result = DoctrineService.accept(player, doctrine);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int doctrineDecline(final CommandContext<CommandSourceStack> context, final TeamDoctrine doctrine) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final DoctrineService.DoctrineResult result = DoctrineService.decline(player, doctrine);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int doctrineStatus(final CommandContext<CommandSourceStack> context, final TeamDoctrine doctrine) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final DoctrineService.DoctrineResult result = DoctrineService.status(player, doctrine);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int markWorld(final CommandContext<CommandSourceStack> context, final WorldAnnotationType type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final WorldAnnotationService.AnnotationResult result = WorldAnnotationService.markLookTarget(player, type);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int unmarkWorld(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final WorldAnnotationService.AnnotationResult result = WorldAnnotationService.removeNearestLookTarget(player);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int setAnchor(final CommandContext<CommandSourceStack> context, final BaseAnchorType type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final BaseAnchorService.AnchorResult result = BaseAnchorService.setAnchor(player, type);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int clearAnchor(final CommandContext<CommandSourceStack> context, final BaseAnchorType type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final BaseAnchorService.AnchorResult result = BaseAnchorService.clearAnchor(player, type);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int returnHome(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final BaseAnchorService.AnchorResult result = BaseAnchorService.returnHome(player);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int why(final CommandContext<CommandSourceStack> context, final CompanionRole role) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final DiagnosticsService.DiagnosticResult result = DiagnosticsService.why(player, role);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int exportReport(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final DiagnosticsService.DiagnosticResult result = DiagnosticsService.exportReport(player);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int repairSave(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final DiagnosticsService.DiagnosticResult result = DiagnosticsService.repairSafeState(player);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int rescueNearest(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final RescueService.RescueResult result = RescueService.beginNearestPlayerRescue(player);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int cyclePolicy(final CommandContext<CommandSourceStack> context, final CompanionRole role) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final PlayerPolicyService.PolicyResult result = PlayerPolicyService.cyclePowerPolicy(context.getSource().getPlayerOrException(), role);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int resetPolicy(final CommandContext<CommandSourceStack> context, final CompanionRole role) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final PlayerPolicyService.PolicyResult result = PlayerPolicyService.resetPowerPolicy(context.getSource().getPlayerOrException(), role);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int identityNickname(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final PlayerIdentityService.IdentityResult result = PlayerIdentityService.setNickname(context.getSource().getPlayerOrException(),
                StringArgumentType.getString(context, "value"));
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int identityClearNickname(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final PlayerIdentityService.IdentityResult result = PlayerIdentityService.clearNickname(context.getSource().getPlayerOrException());
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int identityStyle(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final String raw = StringArgumentType.getString(context, "style");
        try {
            final PlayerIdentityService.IdentityResult result = PlayerIdentityService.setPlayStyle(context.getSource().getPlayerOrException(),
                    PlayerPlayStyle.valueOf(raw.toUpperCase(java.util.Locale.ROOT)));
            reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
            return result.successful() ? 1 : 0;
        } catch (final IllegalArgumentException exception) {
            reply(context.getSource(), false, "PLAY_STYLE_INVALID: explorer, cautious, builder, fighter, protector, or unset.");
            return 0;
        }
    }

    private static int identityStatus(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final var identity = TeamSavedData.get(player.server).blackboard(player.getUUID()).playerIdentity();
        final String label = identity.nicknameEnabled() ? identity.approvedNickname() : player.getGameProfile().getName();
        reply(context.getSource(), true, "IDENTITY: label=" + label + " style=" + identity.playStyle() + " nickname_enabled=" + identity.nicknameEnabled());
        return 1;
    }

    private static int playtestRate(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final String raw = StringArgumentType.getString(context, "axis");
        try {
            final PlaytestTelemetryService.Result result = PlaytestTelemetryService.record(context.getSource().getPlayerOrException(),
                    AcceptanceAxis.valueOf(raw.toUpperCase(java.util.Locale.ROOT)), IntegerArgumentType.getInteger(context, "score"));
            reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
            return result.successful() ? 1 : 0;
        } catch (final IllegalArgumentException exception) {
            reply(context.getSource(), false, "PLAYTEST_AXIS_INVALID: clarity, usefulness, personality, pace, safety, memory, or series_feel.");
            return 0;
        }
    }

    private static int playtestStatus(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final PlaytestTelemetryService.Result result = PlaytestTelemetryService.status(context.getSource().getPlayerOrException());
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int safeMode(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final SafeModeService.SafeModeResult result = SafeModeService.enable(player, SafeModeReason.MANUAL,
                "Player enabled Safe Mode with the command surface.");
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int clearSafeMode(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final SafeModeService.SafeModeResult result = SafeModeService.clear(context.getSource().getPlayerOrException());
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int tutorialStatus(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final OnboardingService.Result result = OnboardingService.status(context.getSource().getPlayerOrException());
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int tutorialDismiss(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final OnboardingService.Result result = OnboardingService.dismiss(context.getSource().getPlayerOrException());
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int tutorialResume(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final OnboardingService.Result result = OnboardingService.resume(context.getSource().getPlayerOrException());
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int intentionAccept(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final IntentionService.IntentionResult result = IntentionService.accept(context.getSource().getPlayerOrException());
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int intentionDefer(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final IntentionService.IntentionResult result = IntentionService.defer(context.getSource().getPlayerOrException());
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int intentionStatus(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        reply(context.getSource(), true, "INTENTION: " + IntentionService.summary(TeamSavedData.get(player.server).blackboard(player.getUUID())));
        return 1;
    }

    private static int selectSquad(final CommandContext<CommandSourceStack> context, final CompanionRole first, final CompanionRole second) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final DuoDynamicsService.DuoResult result = DuoDynamicsService.selectAtBase(context.getSource().getPlayerOrException(), first, second);
        reply(context.getSource(), result.successful(), result.code() + ": " + result.detail());
        return result.successful() ? 1 : 0;
    }

    private static int status(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final TeamSavedData teamData = TeamSavedData.get(player.server);
        final var snapshot = teamData.snapshot(player.getUUID());
        final var blackboard = teamData.blackboard(player.getUUID());
        final StringBuilder message = new StringBuilder("Plan=" + blackboard.plan().type() + "/" + blackboard.plan().status() + " formation=" + blackboard.formation() + " danger=" + blackboard.dangerScore() + " | Companions: ");
        for (final CompanionRole role : CompanionRole.values()) {
            final var loaded = CompanionLifecycleService.findForOwner(player, role);
            if (loaded.isPresent()) {
                final CompanionEntity companion = loaded.get();
                message.append(role.id()).append('=').append(companion.getCompanionState())
                        .append(" energy:").append(Math.round(companion.getEnergy()))
                        .append(" reason:").append(companion.getLastReasonCode()).append(" | ");
            } else if (snapshot.containsKey(role)) {
                message.append(role.id()).append('=').append(snapshot.get(role).lifecycle()).append(" (unloaded) | ");
            }
        }
        reply(context.getSource(), true, message.toString());
        return 1;
    }

    private static CompanionRole role(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final String raw = StringArgumentType.getString(context, "role");
        return CompanionRole.parse(raw).orElseThrow(() -> INVALID_ROLE.create(raw));
    }

    private static CompanionRole roleArgument(final CommandContext<CommandSourceStack> context, final String argument) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final String raw = StringArgumentType.getString(context, argument);
        return CompanionRole.parse(raw).orElseThrow(() -> INVALID_ROLE.create(raw));
    }

    private static CompanionAbility ability(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final String raw = StringArgumentType.getString(context, "ability");
        return CompanionAbility.parse(raw).orElseThrow(() -> INVALID_ABILITY.create(raw));
    }

    private static BaseAnchorType anchorType(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final String raw = StringArgumentType.getString(context, "anchor_type").toUpperCase(java.util.Locale.ROOT);
        try {
            return BaseAnchorType.valueOf(raw);
        } catch (final IllegalArgumentException exception) {
            throw INVALID_ANCHOR.create(raw);
        }
    }

    private static WorldAnnotationType annotationType(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final String raw = StringArgumentType.getString(context, "annotation_type").toUpperCase(java.util.Locale.ROOT);
        try {
            return WorldAnnotationType.valueOf(raw);
        } catch (final IllegalArgumentException exception) {
            throw INVALID_ANNOTATION.create(raw);
        }
    }

    private static TeamDoctrine doctrine(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final String raw = StringArgumentType.getString(context, "doctrine").toUpperCase(java.util.Locale.ROOT);
        try {
            return TeamDoctrine.valueOf(raw);
        } catch (final IllegalArgumentException exception) {
            throw INVALID_DOCTRINE.create(raw);
        }
    }

    private static PingType pingType(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final String raw = StringArgumentType.getString(context, "ping_type").toUpperCase(java.util.Locale.ROOT);
        try {
            return PingType.valueOf(raw);
        } catch (final IllegalArgumentException exception) {
            throw INVALID_PING.create(raw);
        }
    }

    private static StoryChapter storyChapter(final CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final String raw = StringArgumentType.getString(context, "chapter").toUpperCase(java.util.Locale.ROOT);
        try {
            return StoryChapter.valueOf(raw);
        } catch (final IllegalArgumentException exception) {
            throw INVALID_STORY_CHAPTER.create(raw);
        }
    }

    private static void reply(final CommandSourceStack source, final boolean success, final String message) {
        if (success) {
            source.sendSuccess(() -> Component.literal("[Rift Companions] " + message), false);
        } else {
            source.sendFailure(Component.literal("[Rift Companions] " + message));
        }
    }
}
