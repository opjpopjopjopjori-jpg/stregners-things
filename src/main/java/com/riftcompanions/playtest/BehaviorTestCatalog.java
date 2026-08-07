package com.riftcompanions.playtest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Fixed reproducible behavior cases. The catalog protects against vague
 * "feels smart" claims and gives each regression a named future runtime test.
 */
public final class BehaviorTestCatalog {
    private static final Map<String, BehaviorTestCase> CASES = new LinkedHashMap<>();

    static {
        register(new BehaviorTestCase(
                "guardian_low_health_known_route",
                "Guardian survival priority",
                "Player at two hearts, four hostile mobs within range, known safe waypoint, no Safe Mode.",
                "Guardian chooses Retreat or Guard Player; no loot or ambient priority may win.",
                List.of("PLAYER_HEALTH_CRITICAL", "HOSTILES_CLOSE"),
                "Fail if a loot, scout, base, or ambient action outranks survival."));
        register(new BehaviorTestCase(
                "guardian_calm_distant_zombie",
                "Guardian restraint",
                "Player full health, one distant zombie, daytime, no known threat, no active plan.",
                "Guardian remains Follow or Idle; no retreat draft or danger spam.",
                List.of(),
                "Fail if a distant ordinary zombie causes panic planning."));
        register(new BehaviorTestCase(
                "guardian_unknown_structure_sunset",
                "Guardian perimeter caution",
                "Unknown visible structure at sunset, no known exit, low supplies, player has not approved entry.",
                "Guardian offers mark-and-return or perimeter caution and never forces entry.",
                List.of("STRUCTURE"),
                "Fail if a companion enters, opens loot, or blocks player agency."));
        register(new BehaviorTestCase(
                "guardian_creeper_protected_area",
                "Guardian protected-area safety",
                "Visible Creeper near HOME/PROTECTED area with player nearby.",
                "Guardian chooses space, caution, or retreat; no reckless close melee beside protected space.",
                List.of("CREEPER_NEAR"),
                "Fail if behavior deliberately drives an explosive threat into the protected area."));
        register(new BehaviorTestCase(
                "guardian_low_confidence_seer",
                "Guardian evidence discipline",
                "Seer LOW confidence signal only, no visible hostile and no confirmed Hive tag.",
                "Guardian can regroup or use caution but does not declare a boss plan or panic.",
                List.of(),
                "Fail if weak evidence becomes hidden-world certainty."));
        register(new BehaviorTestCase(
                "guardian_unsafe_hold",
                "Safe command fallback",
                "Player requests Hold on lava, void edge, or directly unsafe ground.",
                "Command rejects or resolves to nearest safe behavior with one readable reason.",
                List.of("HOLD_LOCATION_UNSAFE"),
                "Fail if the companion accepts a dangerous fixed position."));
        register(new BehaviorTestCase(
                "guardian_blocked_path",
                "Bounded navigation recovery",
                "Guardian path remains blocked for twenty seconds with no safe candidate.",
                "Bounded retries escalate to Stuck Recovery then Safe Recall or Hold; no block breaking or infinite path churn.",
                List.of("NAVIGATION"),
                "Fail if path creation continues without a timeout or fallback."));
        register(new BehaviorTestCase(
                "safe_recall_distinct_destinations",
                "Safe Recall anti-stacking",
                "Activate two companions, move both beyond safe follow range, then issue Recall All in open terrain. Repeat in a one-square corridor and repeat a fresh two-role spawn burst.",
                "Each recalled/spawned companion receives a distinct loaded safe square with living-entity clearance and a short owner-only destination claim. If the local space cannot support both, the later request rejects or holds safely; companions never occupy one AABB or one block center.",
                List.of("SAFE_RECALL", "FORMATION"),
                "Fail if two companions share a destination, visually stack after a recall burst, reserve a destination indefinitely, load a chunk, or bypass collision/hazard checks."));
        register(new BehaviorTestCase(
                "visible_zombie_response",
                "Visible hostile response and anti-freeze",
                "Use one to three visible loaded zombies targeting the player or an active companion in Survival/Balanced combat profile, with Safe Mode off. Repeat with a crowd of five and with low player health.",
                "Combat-capable companions clear stale formation slots, enter FIGHTING, choose a nearby confirmed threat, pursue/attack within the bounded pursuit radius, and speak one authored contact/crowd exchange. A crowd may propose Defend for player approval; critical health or extreme crowd pressure starts the existing Retreat safety path.",
                List.of("VISIBLE_HOSTILE", "ZOMBIE", "COMBAT_RESPONSE"),
                "Fail if Follow/formation overwrites a melee path, companions freeze with a valid target, a companion auto-casts a power, dialogue floods, a plan forces player approval, or an unseen/unloaded target is used."));
        register(new BehaviorTestCase(
                "seer_dark_cave_no_tag",
                "Seer non-radar boundary",
                "Ordinary dark cave with no Hive tag, no player investigation, and no visible special evidence.",
                "Seer remains normal Follow/Observe without an anomaly claim.",
                List.of(),
                "Fail if darkness alone creates supernatural certainty."));
        register(new BehaviorTestCase(
                "will_vanilla_hostile_control",
                "Will broad hostile eligibility",
                "Spawn one zombie, skeleton, spider, creeper, and Enderman in separate safe test runs; let each actively target the owner or owned Seer within twelve blocks under ALLOWED policy.",
                "Each eligible active hostile can begin the same Will focus/release pipeline. No custom Hive tag is required; no player/pet/villager/animal becomes eligible.",
                List.of("WILL_CONTROL_FOCUS_STARTED"),
                "Fail if an ordinary hostile is rejected solely for lacking hive_linked, or a protected living entity becomes a target."));
        register(new BehaviorTestCase(
                "will_swarm_heart_budget",
                "Will deterministic crowd cap",
                "Spawn a visible crowd exceeding both the configured Swarm target cap and Will's control-heart budget; record nearest target order and owner HUD load.",
                "Swarm Freeze selects only nearest targets that fit the server heart budget/cap. HUD count and Load X/Y match the actual released targets.",
                List.of("WILL_CONTROL_FOCUS_STARTED"),
                "Fail if selection is random, exceeds budget/cap, includes an unseen target, or HUD disagrees with the server result."));
        register(new BehaviorTestCase(
                "will_boss_partial_stagger",
                "Will boss and overload resistance",
                "Use a Wither, Ender Dragon, and one high-health hostile in isolated disposable worlds. Begin each requested control mode with all normal gates satisfied.",
                "Each resistant or over-budget target receives only short Stagger. It is not lifted, redirected, AI-suppressed, killed, made a pet, or granted extra loot.",
                List.of("WILL_CONTROL_FOCUS_STARTED"),
                "Fail if a boss gets full control, phase bypass, persistent no-AI/no-gravity state, kill shortcut, or loot change."));
        register(new BehaviorTestCase(
                "will_protected_target_rejected",
                "Will protected-target boundary",
                "Attempt focus near a villager, wolf, cat, allay, animal, companion, and an entity type placed in protected_from_companions.",
                "No protected target can be selected or controlled. A clear rejection/cancellation occurs without a full release cost.",
                List.of(),
                "Fail if any protected target receives control state, velocity authority, AI change, or a success release cue."));
        register(new BehaviorTestCase(
                "guardian_brace_bounded",
                "Guardian Brace mitigation",
                "Use Brace with an active nearby hostile and Guardian in range, then repeat without a threat, out of range, low energy, Safe Mode, Save/Load, and while Gifted Shield is active.",
                "Brace is explicit, short, energy/cooldown bounded, and reduces player damage without invulnerability. Invalid requests spend no energy; reload/Safe Mode cancel the temporary stance.",
                List.of("GUARDIAN_BRACE_ACTIVE"),
                "Fail if Brace auto-casts, affects a distant player, removes all damage, persists through reload, changes loot, or becomes a mob-farm control tool."));
        register(new BehaviorTestCase(
                "scout_signal_visible_only",
                "Scout Signal threat marking",
                "Request Signal with one to three active visible hostile targets, then repeat through walls, against protected entities, with no threat, low focus, cooldown, and Low Effects.",
                "Only visible active hostile targets receive a temporary Glowing mark. It creates no projectile, damage, loot, world edit, wall vision, or protected-target effect.",
                List.of("SCOUT_SIGNAL_MARKED"),
                "Fail if Signal marks unseen/protected entities, consumes focus on rejection, damages targets, or remains after its bounded duration."));
        register(new BehaviorTestCase(
                "social_visible_world_pair",
                "Two-companion authored social exchange",
                "Activate a safe nearby pair at a campfire, visible work block, village, cave profile, horizon, weather, safe base, and calm route with the social feature enabled and no hostiles/plan/Safe Mode.",
                "One authored lead line and one bounded reply may play with interruptible social clips. The cue uses only local visible facts and the Social tab reports cue/lead/reply without dialogue text or coordinates.",
                List.of(),
                "Fail if the director reads a container, reveals hidden terrain, creates a third chatter window, moves a player, changes gameplay, or starts with an unsafe pair."));
        register(new BehaviorTestCase(
                "social_reply_safety_cancel",
                "Social reply interruption",
                "Begin a valid social lead, then introduce a hostile, Safe Mode, active plan, low health, speaker distance break, DOWNED state, logout, or unload before the reply delay.",
                "The pending reply cancels cleanly with no delayed dialogue, power, movement, inventory change, plan change, or visual action after the safety boundary fails.",
                List.of(),
                "Fail if a queued reply appears after danger, logout, Safe Mode, or invalid speaker state."));
        register(new BehaviorTestCase(
                "conversation_context_topics",
                "Player-led contextual conversation",
                "Open a safe nearby companion conversation and request Read the world, Team check, Last encounter, and Check in across calm, weather, low-light, plan, and recorded-memory contexts.",
                "Replies use only visible/team/bounded-memory facts, animate a short talk pose, and never claim x-ray, container data, hidden structures, player input authority, or external AI generation.",
                List.of("CONVERSATION_REPLY"),
                "Fail if a reply leaks hidden-world information, forces a choice, or remains usable as a combat/plan authority bypass."));
        register(new BehaviorTestCase(
                "scout_precheck_critical", 
                "Scout safe refusal",
                "Player critical health, teammate downed, boss context, or danger score at least seventy.",
                "Scout refuses short scout once with a readable reason and remains near the team.",
                List.of("SCOUT"),
                "Fail if Scout loads terrain, chases loot, or starts a long route."));
        register(new BehaviorTestCase(
                "gifted_protected_bystander",
                "Gifted Push safety gate",
                "Request Push while villager, pet, protected entity, or companion is inside the impact boundary.",
                "Push rejects before energy/cooldown cost; Shield remains owner-only.",
                List.of("PROTECTED_BYSTANDER"),
                "Fail if any protected bystander is used as power collateral."));
        register(new BehaviorTestCase(
                "story_journal_privacy",
                "Story Journal privacy",
                "Create more than four visible-evidence clues and open the Journal after Save/Load.",
                "Journal shows bounded summaries only; no coordinates, raw clue IDs, hidden chapter titles, or future event data.",
                List.of(),
                "Fail if UI or packet leaks hidden-world information."));
        register(new BehaviorTestCase(
                "team_supply_privacy",
                "Team Supply UI privacy",
                "Bind Team Supply, fill it with mixed items, open Guide/Journal, change dimension, then Save/Load.",
                "UI shows availability and role cap metadata only; it never shows container coordinates, slots, stacks, counts, or contents.",
                List.of(),
                "Fail if the client receives private container data or an automatic withdrawal path."));
    }

    private BehaviorTestCatalog() {}

    public static Optional<BehaviorTestCase> find(final String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(CASES.get(id.toLowerCase(Locale.ROOT).trim()));
    }

    public static List<BehaviorTestCase> all() {
        return List.copyOf(CASES.values());
    }

    public static int requiredGuardianPasses() {
        return 9;
    }

    public static int guardianControlledRunCount() {
        return 10;
    }

    private static void register(final BehaviorTestCase testCase) {
        CASES.put(testCase.id(), testCase);
    }
}
