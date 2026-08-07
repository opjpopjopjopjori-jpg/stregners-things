#!/usr/bin/env python3
"""Generate an authored, data-driven vanilla contextual interaction library.

The output contains only original English field-guide dialogue and local context
metadata. It creates no entities, quests, loot, blocks, automation, animal
ownership, container access, or runtime-generated text.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src/main/resources/data/riftcompanions/contextual_interactions/vanilla_field_guide.json"
DIALOGUE = ROOT / "src/main/resources/data/riftcompanions/companions_dialogue/core_en_us.json"

ANIMALS = [
    ("cow", "minecraft:cow", "Cow", "Cows can provide leather and food if you choose to hunt; companions will not herd or move them.", "calm grazing keeps the field quiet", "CONTEXT_ANIMAL_GREET"),
    ("sheep", "minecraft:sheep", "Sheep", "Wool supports beds and banners when you collect it yourself.", "its flock is settled", "CONTEXT_ANIMAL_GREET"),
    ("pig", "minecraft:pig", "Pig", "Pigs are simple livestock, not a route objective.", "it is not threatening the path", "CONTEXT_ANIMAL_GREET"),
    ("chicken", "minecraft:chicken", "Chicken", "Eggs and feathers are useful supplies, but there is no reason to chase it.", "it is easy to startle", "CONTEXT_ANIMAL_GREET"),
    ("horse", "minecraft:horse", "Horse", "A horse can help a player travel, but companions never mount or transport automatically.", "it needs room and a calm approach", "CONTEXT_ANIMAL_OBSERVE"),
    ("donkey", "minecraft:donkey", "Donkey", "A donkey can carry player equipment, but companions never access or move storage.", "it is a player choice, not team automation", "CONTEXT_ANIMAL_OBSERVE"),
    ("llama", "minecraft:llama", "Llama", "Llamas can be useful travel animals, but the team will not lead them for you.", "give it space instead of crowding it", "CONTEXT_ANIMAL_OBSERVE"),
    ("camel", "minecraft:camel", "Camel", "Camels are tall travel animals; companions do not ride or transport on them automatically.", "the open ground suits it", "CONTEXT_ANIMAL_OBSERVE"),
    ("wolf", "minecraft:wolf", "Wolf", "A wolf may become a player ally if you choose to tame it; companions never command pets.", "it is watching us carefully", "CONTEXT_ANIMAL_OBSERVE"),
    ("cat", "minecraft:cat", "Cat", "Cats can discourage creepers nearby, but it is still a living animal, not a tool.", "it prefers a quiet approach", "CONTEXT_ANIMAL_GREET"),
    ("fox", "minecraft:fox", "Fox", "Foxes are quick and cautious. Let them keep their own route.", "it notices movement before most animals do", "CONTEXT_ANIMAL_OBSERVE"),
    ("rabbit", "minecraft:rabbit", "Rabbit", "Rabbits are fragile and fast. There is no need to disturb them.", "the ground cover is giving it shelter", "CONTEXT_ANIMAL_GREET"),
    ("bee", "minecraft:bee", "Bee", "Bees matter to flowers and crops. Do not provoke a hive without a reason.", "it is working, not looking for trouble", "CONTEXT_ANIMAL_OBSERVE"),
    ("turtle", "minecraft:turtle", "Turtle", "Turtles need a clear shore. Let them reach it without crowding them.", "slow does not mean helpless", "CONTEXT_ANIMAL_OBSERVE"),
    ("frog", "minecraft:frog", "Frog", "Frogs keep to wet ground and react to small nearby prey.", "the waterline is part of its route", "CONTEXT_ANIMAL_GREET"),
    ("axolotl", "minecraft:axolotl", "Axolotl", "Axolotls belong in water. Do not turn a sighting into a capture routine.", "it is safest where it can swim", "CONTEXT_ANIMAL_OBSERVE"),
    ("goat", "minecraft:goat", "Goat", "Goats can knock things back near cliffs. Give them space on high ground.", "the slope makes sudden movement risky", "CONTEXT_ANIMAL_OBSERVE"),
    ("panda", "minecraft:panda", "Panda", "Pandas are passive and tied to bamboo country. Observe, do not crowd.", "it is calmer when the path stays open", "CONTEXT_ANIMAL_GREET"),
    ("dolphin", "minecraft:dolphin", "Dolphin", "Dolphins move quickly through water. Companions will not auto-swim after them.", "the current is their terrain", "CONTEXT_ANIMAL_OBSERVE"),
    ("iron_golem", "minecraft:iron_golem", "Iron Golem", "An iron golem protects nearby villagers. Keep hostile pressure away from its village.", "it is already holding a defensive role", "CONTEXT_ANIMAL_OBSERVE"),
    ("allay", "minecraft:allay", "Allay", "An allay can help a player with items, but companions never direct it or access storage through it.", "it is not a remote inventory path", "CONTEXT_ANIMAL_OBSERVE"),
    ("sniffer", "minecraft:sniffer", "Sniffer", "A sniffer can uncover ancient plant seeds over time. Let the player decide what to collect.", "it is exploring the ground at its own pace", "CONTEXT_ANIMAL_OBSERVE"),
]

HOSTILES = [
    ("zombie", "minecraft:zombie", "Zombie", "close melee pressure", "Rotten flesh is not a safe food plan", "keep a clear exit and do not get cornered"),
    ("skeleton", "minecraft:skeleton", "Skeleton", "ranged arrows punish open ground", "Bones can become bone meal if you choose to collect them", "use cover instead of standing in one line"),
    ("creeper", "minecraft:creeper", "Creeper", "an explosive threat near people and builds", "Gunpowder is never worth risking a protected area", "make space and avoid crowding it"),
    ("spider", "minecraft:spider", "Spider", "fast climbing and close ambush pressure", "String can be useful crafting material", "watch walls and ceilings before chasing"),
    ("enderman", "minecraft:enderman", "Enderman", "teleport movement and dangerous eye contact", "Ender pearls are useful but not a reason to rush", "do not stare at it unless you are ready"),
    ("witch", "minecraft:witch", "Witch", "unpredictable potion pressure", "Its drops vary, but safety matters more than ingredients", "keep distance and break line of sight"),
    ("slime", "minecraft:slime", "Slime", "split pressure in tight ground", "Slimeballs are a sticky crafting component", "leave room for smaller splits"),
    ("drowned", "minecraft:drowned", "Drowned", "water-side pressure and possible ranged attacks", "Rare equipment is not worth drowning for", "fight from safe footing"),
    ("husk", "minecraft:husk", "Husk", "desert melee pressure with hunger risk", "Its drop is not a priority", "keep water and distance in mind"),
    ("stray", "minecraft:stray", "Stray", "ranged slowness pressure in cold ground", "Arrows are useful only after the fight is safe", "use cover and avoid the open"),
    ("pillager", "minecraft:pillager", "Pillager", "crossbow fire and raid pressure", "Crossbows are player loot, not a companion task", "find cover before you trade shots"),
    ("vindicator", "minecraft:vindicator", "Vindicator", "heavy axe damage at close range", "Its weapon is not worth a reckless charge", "keep a defender between it and the team"),
    ("ravager", "minecraft:ravager", "Ravager", "large knockback and raid pressure", "There is no quick safe reward here", "make distance and protect villagers"),
    ("blaze", "minecraft:blaze", "Blaze", "fire from range and vertical movement", "Blaze rods matter for brewing, but only after a safe fight", "use cover and watch above you"),
    ("ghast", "minecraft:ghast", "Ghast", "long-range fireball pressure", "Ghast tears are useful, but do not chase into unsafe air", "watch the fireball path and keep solid cover"),
    ("hoglin", "minecraft:hoglin", "Hoglin", "strong knockback in cramped terrain", "Food is never worth being knocked into danger", "leave room to sidestep"),
    ("piglin_brute", "minecraft:piglin_brute", "Piglin Brute", "high close-range damage around bastions", "There is no harmless shortcut through a bastion", "do not split the team around it"),
    ("phantom", "minecraft:phantom", "Phantom", "diving attacks from above", "Membranes have uses, but sky awareness comes first", "watch the angle of the next dive"),
    ("guardian", "minecraft:guardian", "Guardian", "water laser pressure", "Prismarine rewards do not remove the water risk", "fight from a planned water position"),
    ("shulker", "minecraft:shulker", "Shulker", "projectile levitation in enclosed places", "Shells can make storage boxes, but companions never exploit storage", "watch your landing space"),
    ("silverfish", "minecraft:silverfish", "Silverfish", "small swarm pressure near stone structures", "They are a warning about the structure, not a loot target", "keep the exit visible"),
    ("magma_cube", "minecraft:magma_cube", "Magma Cube", "jumping pressure and hot terrain", "Slimeballs are useful only if the ground stays safe", "do not back into lava"),
    ("warden", "minecraft:warden", "Warden", "extreme sound-driven danger", "There is no loot reason to force this encounter", "leave quietly and keep the route simple"),
]

BIOMES = [
    ("plains", "minecraft:plains", "Plains", "open ground gives good visibility but little cover", "watch the horizon before night closes in"),
    ("forest", "minecraft:forest", "Forest", "trees break sightlines and can hide a route turn", "keep track of the way back"),
    ("taiga", "minecraft:taiga", "Taiga", "cold trees make visibility uneven", "keep the group close near snow and water"),
    ("desert", "minecraft:desert", "Desert", "open sand exposes the team at long range", "pick cover before a fight picks the place"),
    ("savanna", "minecraft:savanna", "Savanna", "dry slopes can separate the team quickly", "keep the next safe ridge in view"),
    ("swamp", "minecraft:swamp", "Swamp", "water and low visibility slow safe movement", "do not rush through shallow water"),
    ("jungle", "minecraft:jungle", "Jungle", "dense leaves hide turns and drops", "mark the exit with memory, not reckless speed"),
    ("badlands", "minecraft:badlands", "Badlands", "steep clay ridges create sudden drop risks", "take the lower safe route when possible"),
    ("stony_peaks", "minecraft:stony_peaks", "Stony Peaks", "thin ledges punish knockback", "leave room for a safe retreat line"),
    ("meadow", "minecraft:meadow", "Meadow", "open hills give visibility but few walls", "watch high ground before crossing"),
    ("snowy_slopes", "minecraft:snowy_slopes", "Snowy Slopes", "snow and cliffs make footing the first problem", "move together near an edge"),
    ("beach", "minecraft:beach", "Beach", "open shore leaves little cover from ranged threats", "keep water behind you only if you planned it"),
    ("ocean", "minecraft:ocean", "Ocean", "water changes every escape route", "companions will not auto-swim away from the player"),
    ("river", "minecraft:river", "River", "a river is a route boundary, not an automatic crossing", "choose the crossing yourself"),
    ("dripstone_caves", "minecraft:dripstone_caves", "Dripstone Caves", "spikes above and below make vertical space dangerous", "check the ceiling before a fight"),
    ("lush_caves", "minecraft:lush_caves", "Lush Caves", "water and plants make paths look safer than they are", "keep the return route visible"),
    ("deep_dark", "minecraft:deep_dark", "Deep Dark", "sound and darkness make caution more valuable than loot", "move quietly and keep the exit simple"),
    ("nether_wastes", "minecraft:nether_wastes", "Nether Wastes", "lava and open drops punish rushed decisions", "keep a solid route in mind"),
    ("crimson_forest", "minecraft:crimson_forest", "Crimson Forest", "dense hostile ground limits clean exits", "do not turn every path into a chase"),
    ("warped_forest", "minecraft:warped_forest", "Warped Forest", "the terrain is strange but an exit still matters", "keep the team in visual range"),
    ("soul_sand_valley", "minecraft:soul_sand_valley", "Soul Sand Valley", "slow ground and open sightlines invite ranged danger", "cover matters more than speed"),
    ("basalt_deltas", "minecraft:basalt_deltas", "Basalt Deltas", "uneven hot ground makes every step a route decision", "do not backpedal without checking lava"),
    ("end_highlands", "minecraft:end_highlands", "End Highlands", "void edges turn small mistakes into permanent ones", "keep the path wide and deliberate"),
]

STRUCTURES = [
    ("village", "Village", "villagers are not part of a combat plan", "keep hostile pressure away from homes"),
    ("dungeon", "Dungeon", "a compact room can turn one threat into a crowd", "check the exit before any reward"),
    ("mineshaft", "Mineshaft", "turns and cave-ins make a return route important", "do not split for rails or loot"),
    ("stronghold", "Stronghold", "deep corridors punish rushing without a clear return", "hold the team together at each junction"),
    ("ruined_portal", "Ruined Portal", "it is a landmark, not an order to change dimension", "the player decides whether it matters"),
    ("ancient_city", "Ancient City", "sound and darkness make restraint the real objective", "do not treat it like a normal loot room"),
    ("desert_pyramid", "Desert Pyramid", "old structures can hide simple traps", "check footing before opening anything"),
    ("jungle_temple", "Jungle Temple", "dense cover can hide switches and threats", "keep the route back visible"),
    ("ocean_monument", "Ocean Monument", "water pressure changes every escape route", "do not enter without a plan"),
    ("nether_fortress", "Nether Fortress", "open bridges and blaze fire reward patience", "keep cover and an exit in mind"),
    ("bastion", "Bastion", "tight gold-lined corridors can turn hostile fast", "do not split the team for a chest"),
]

ROLES = ("guardian", "seer", "gifted", "scout")


BLOCKS = [
    ("crafting_table", "minecraft:crafting_table", "Crafting Table", "A crafting table turns gathered materials into deliberate choices", "CONTEXT_FIELD_NOTE"),
    ("furnace", "minecraft:furnace", "Furnace", "A furnace is useful for food and ores, but companions never operate it automatically", "CONTEXT_FIELD_NOTE"),
    ("smoker", "minecraft:smoker", "Smoker", "A smoker is a faster cooking station when the player chooses to use it", "CONTEXT_FIELD_NOTE"),
    ("blast_furnace", "minecraft:blast_furnace", "Blast Furnace", "A blast furnace is specialized equipment, not a reason to rush resources", "CONTEXT_FIELD_NOTE"),
    ("anvil", "minecraft:anvil", "Anvil", "An anvil can repair and rename player equipment; preserve it instead of wasting durability", "CONTEXT_LOOT_NOTE"),
    ("grindstone", "minecraft:grindstone", "Grindstone", "A grindstone can remove enchantments and recover some experience; decide carefully", "CONTEXT_LOOT_NOTE"),
    ("loom", "minecraft:loom", "Loom", "A loom is for player banner work and does not change team combat authority", "CONTEXT_FIELD_NOTE"),
    ("cartography_table", "minecraft:cartography_table", "Cartography Table", "A cartography table helps player map work; companions never inspect maps for hidden terrain", "CONTEXT_ROUTE_NOTE"),
    ("stonecutter", "minecraft:stonecutter", "Stonecutter", "A stonecutter makes building choices efficient, but companions do not build automatically", "CONTEXT_FIELD_NOTE"),
    ("smithing_table", "minecraft:smithing_table", "Smithing Table", "A smithing table supports player gear upgrades; save the decision for a safe moment", "CONTEXT_LOOT_NOTE"),
    ("enchanting_table", "minecraft:enchanting_table", "Enchanting Table", "An enchanting table is a player-controlled investment, not a companion storage path", "CONTEXT_LOOT_NOTE"),
    ("brewing_stand", "minecraft:brewing_stand", "Brewing Stand", "A brewing stand can prepare player potions; companions never automate brewing", "CONTEXT_FIELD_NOTE"),
    ("campfire", "minecraft:campfire", "Campfire", "A campfire is a good visible place to pause, cook, and check the route", "CONTEXT_REST_REQUEST"),
    ("bed", "minecraft:red_bed", "Bed", "A bed can set a player respawn and make a safe rest decision meaningful", "CONTEXT_REST_REQUEST"),
]

WEATHER_TIME = [
    ("weather_rain", "WEATHER", "RAIN", "Rain", "wet ground reduces clear sightlines", "keep the team in visual range", "CONTEXT_BIOME_BRIEF"),
    ("weather_thunder", "WEATHER", "THUNDER", "Thunder", "thunder makes the next safe decision more important", "do not let weather turn the route into panic", "CONTEXT_BIOME_BRIEF"),
    ("time_dawn", "TIME", "DAWN", "Dawn", "the sky is opening and the route is easier to read", "use the light to check the way back", "CONTEXT_BIOME_BRIEF"),
    ("time_dusk", "TIME", "DUSK", "Dusk", "visibility will change soon", "pick a safe direction before night adds pressure", "CONTEXT_ROUTE_NOTE"),
    ("time_night", "TIME", "NIGHT", "Night", "hostile pressure can grow quickly after dark", "stay together and keep a retreat route", "CONTEXT_ROUTE_NOTE"),
]


def definition(identifier: str, kind: str, match: str, group: str, stage: str, lead_trigger: str, reply_trigger: str,
               action: str, reply_action: str, priority: int, cooldown: int, focused: bool = False) -> dict:
    return {"id": identifier, "kind": kind, "match": match, "group": group, "stage": stage,
            "lead_trigger": lead_trigger, "reply_trigger": reply_trigger, "action": action,
            "reply_action": reply_action, "priority": priority, "cooldown_ticks": cooldown,
            "requires_focused_monster": focused}


def animal_lines(name: str, fact: str, mood: str, revisit: bool) -> dict[str, tuple[str, str]]:
    if revisit:
        return {
            "guardian": (f"That {name.lower()} is still nearby. {fact}", "We keep the route clear and let it be."),
            "seer": (f"The {name.lower()} has not changed its pattern. {mood.capitalize()}.", "That is useful context, not a reason to interfere."),
            "gifted": (f"The {name.lower()} still seems calm. {mood.capitalize()}.", "I am glad we did not turn it into a problem."),
            "scout": (f"Same {name.lower()}, same route. {mood.capitalize()}.", "No detour needed. We keep moving when you are ready."),
        }
    return {
        "guardian": (f"{name} ahead. {fact}", f"Easy. {mood.capitalize()}. We leave it alone unless you choose otherwise."),
        "seer": (f"{name}. {mood.capitalize()}; that tells us the ground is calm for now.", f"{fact} It is part of this place, not a problem to solve."),
        "gifted": (f"That {name.lower()} is calm. {fact}", f"{mood.capitalize()}. Let it stay safe where it is."),
        "scout": (f"{name} sighting. {mood.capitalize()}.", f"{fact} I will not turn it into a detour."),
    }


def hostile_lines(name: str, danger: str, item: str, counter: str, revisit: bool) -> dict[str, tuple[str, str]]:
    if revisit:
        return {
            "guardian": (f"We have seen this {name.lower()} before. {counter.capitalize()}.", f"{item}. That stays secondary to getting everyone out safely."),
            "seer": (f"The {name.lower()} pattern is familiar now. {danger.capitalize()}.", f"{counter.capitalize()}. Keep the threat visible before deciding anything."),
            "gifted": (f"Same {name.lower()}, same risk: {danger}.", f"{counter.capitalize()}. We use a safe opening, not a reckless one."),
            "scout": (f"I recognize the {name.lower()} pattern. {counter.capitalize()}.", f"{item}. Route safety still comes first."),
        }
    return {
        "guardian": (f"{name}: {danger}. {counter.capitalize()}.", f"{item}. That does not make a careless fight worth it."),
        "seer": (f"{name}. {danger.capitalize()}. {counter.capitalize()}.", f"{item}. Keep the threat visible before deciding anything."),
        "gifted": (f"{name} can bring {danger}. {counter.capitalize()}.", f"{item}. We take only the safe opening, not the risk."),
        "scout": (f"{name}: {danger}. {counter.capitalize()}.", f"{item}. I am watching the route, not promising a chase."),
    }


def biome_lines(name: str, risk: str, advice: str, revisit: bool) -> dict[str, tuple[str, str]]:
    prefix = "We know this ground now" if revisit else name
    return {
        "guardian": (f"{prefix}: {risk}. {advice.capitalize()}.", "Keep an exit before trouble starts."),
        "seer": (f"{prefix} feels consistent. {risk.capitalize()}.", f"{advice.capitalize()}. We learn more by staying together."),
        "gifted": (f"{prefix}: {risk}. {advice.capitalize()}.", "We do not need to prove anything to this place."),
        "scout": (f"{prefix} route note: {risk}. {advice.capitalize()}.", "I will keep the safer line in view."),
    }


def structure_lines(name: str, risk: str, advice: str, revisit: bool) -> dict[str, tuple[str, str]]:
    prefix = f"Back at the {name}" if revisit else name
    return {
        "guardian": (f"{prefix}: {risk}. {advice.capitalize()}.", "We can inspect it, but you decide whether we enter."),
        "seer": (f"{prefix}. {risk.capitalize()}.", f"{advice.capitalize()}. I will stay with what we can actually see."),
        "gifted": (f"{prefix}: {risk}. {advice.capitalize()}.", "No reward is worth trapping the team."),
        "scout": (f"{prefix} note: {risk}. {advice.capitalize()}.", "I will remember the exit, not make a blind route."),
    }


def block_lines(name: str, fact: str, revisit: bool) -> dict[str, tuple[str, str]]:
    prefix = f"This {name.lower()} again" if revisit else name
    return {
        "guardian": (f"{prefix}: {fact}.", "Use it when the area is safe and the choice is yours."),
        "seer": (f"{prefix}. {fact}.", "It tells us what is possible, not what we have to do."),
        "gifted": (f"{prefix}: {fact}.", "We can take a moment if you want to use it."),
        "scout": (f"{prefix} note: {fact}.", "I will keep watch on the route while you decide."),
    }


def weather_lines(name: str, risk: str, advice: str, revisit: bool) -> dict[str, tuple[str, str]]:
    prefix = f"The {name.lower()} is still with us" if revisit else name
    return {
        "guardian": (f"{prefix}: {risk}. {advice.capitalize()}.", "No need to rush, but do not let conditions choose for us."),
        "seer": (f"{prefix}. {risk.capitalize()}.", f"{advice.capitalize()}. The route looks different under this light."),
        "gifted": (f"{prefix}: {risk}. {advice.capitalize()}.", "Stay close enough to read each other."),
        "scout": (f"{prefix} route note: {risk}. {advice.capitalize()}.", "I will keep the safer side in view."),
    }


def add_scene_dialogue(entries: list[dict], lead_trigger: str, reply_trigger: str,
                       lines: dict[str, tuple[str, str]], priority: int, cooldown: int) -> None:
    for role, (lead, reply) in lines.items():
        entries.append({"id": f"context.{lead_trigger}.{role}.lead", "role": role, "trigger": lead_trigger,
                        "priority": priority, "cooldown_ticks": cooldown, "text": lead})
        entries.append({"id": f"context.{reply_trigger}.{role}.reply", "role": role, "trigger": reply_trigger,
                        "priority": priority, "cooldown_ticks": cooldown, "text": reply})


def add_two_stage(definitions: list[dict], entries: list[dict], identifier: str, kind: str, match: str,
                  action: str, reply_action: str, priority: int, cooldown: int, lines_factory, focused: bool = False) -> None:
    group = identifier
    for stage, revisit in (("DISCOVERY", False), ("REVISIT", True)):
        suffix = "discovery" if not revisit else "revisit"
        lead = f"context_{identifier}_{suffix}"
        reply = f"context_{identifier}_{suffix}_reply"
        definitions.append(definition(f"{identifier}_{suffix}", kind, match, group, stage, lead, reply,
                                      action, reply_action, priority, cooldown, focused))
        add_scene_dialogue(entries, lead, reply, lines_factory(revisit), priority, cooldown)


def main() -> None:
    definitions: list[dict] = []
    context_entries: list[dict] = []

    for short, entity_id, name, fact, mood, action in ANIMALS:
        add_two_stage(definitions, context_entries, f"animal_{short}", "ENTITY", entity_id, action,
                      "CONTEXT_ANIMAL_OBSERVE", 3, 7200,
                      lambda revisit, n=name, f=fact, m=mood: animal_lines(n, f, m, revisit))

    for short, entity_id, name, danger, item, counter in HOSTILES:
        action = "CONTEXT_THREAT_BRIEF" if short not in {"skeleton", "spider", "slime", "blaze", "shulker"} else "CONTEXT_LOOT_NOTE"
        add_two_stage(definitions, context_entries, f"mob_{short}", "FOCUSED_HOSTILE", entity_id, action,
                      "CONTEXT_ROUTE_NOTE", 2, 9600,
                      lambda revisit, n=name, d=danger, i=item, c=counter: hostile_lines(n, d, i, c, revisit), True)

    for short, biome_id, name, risk, advice in BIOMES:
        add_two_stage(definitions, context_entries, f"biome_{short}", "BIOME", biome_id, "CONTEXT_BIOME_BRIEF",
                      "CONTEXT_ROUTE_NOTE", 3, 9600,
                      lambda revisit, n=name, r=risk, a=advice: biome_lines(n, r, a, revisit))

    for short, name, risk, advice in STRUCTURES:
        add_two_stage(definitions, context_entries, f"structure_{short}", "STRUCTURE", short, "CONTEXT_STRUCTURE_BRIEF",
                      "CONTEXT_ROUTE_NOTE", 3, 9600,
                      lambda revisit, n=name, r=risk, a=advice: structure_lines(n, r, a, revisit))

    for short, block_id, name, fact, action in BLOCKS:
        add_two_stage(definitions, context_entries, f"block_{short}", "PLAYER_BLOCK", block_id, action,
                      "CONTEXT_FIELD_NOTE", 3, 7200,
                      lambda revisit, n=name, f=fact: block_lines(n, f, revisit))

    for short, kind, match, name, risk, advice, action in WEATHER_TIME:
        add_two_stage(definitions, context_entries, short, kind, match, action, "CONTEXT_ROUTE_NOTE", 3, 7200,
                      lambda revisit, n=name, r=risk, a=advice: weather_lines(n, r, a, revisit))

    for stage, revisit in (("DISCOVERY", False), ("REVISIT", True)):
        suffix = "discovery" if not revisit else "revisit"
        lead = f"context_fatigue_low_energy_{suffix}"
        reply = f"context_fatigue_low_energy_{suffix}_reply"
        definitions.append(definition(f"fatigue_low_energy_{suffix}", "FATIGUE", "LOW_ENERGY", "fatigue_low_energy", stage,
                                      lead, reply, "CONTEXT_REST_REQUEST", "CONTEXT_REST_REQUEST", 2, 3600))
        fatigue = {
            "guardian": ("I am running low. A short safe rest would help more than forcing the next fight.", "I can keep moving if you choose, but the team will be stronger after a safe pause."),
            "seer": ("I am tired. Can we stop somewhere safe before the next decision?", "I can continue, but I would rather think clearly than push until I miss something."),
            "gifted": ("I need a short safe rest. I do not want exhaustion to make choices for us.", "Give me a moment when the area is clear, then I can be useful again."),
            "scout": ("I am tired. A short safe pause now will keep the route honest later.", "I can walk on, but I would rather rest before I make a bad call."),
        }
        if revisit:
            fatigue = {
                "guardian": ("I am still low on energy. The safe pause is still the sensible choice.", "We can move when you decide, but the warning has not changed."),
                "seer": ("The fatigue is still there. I would rather rest than turn it into a mistake.", "A clear head is part of staying safe."),
                "gifted": ("I have not recovered enough yet. Please keep the next decision simple.", "A little time in a safe place would help."),
                "scout": ("I am still tired. The route will be safer after a real pause.", "I can keep up, but I should not pretend I am fresh."),
            }
        add_scene_dialogue(context_entries, lead, reply, fatigue, 2, 3600)

    DATA.parent.mkdir(parents=True, exist_ok=True)
    DATA.write_text(json.dumps({"schema_version": 2, "locale": "en_us", "entries": definitions}, indent=2) + "\n", encoding="utf-8")

    dialogue = json.loads(DIALOGUE.read_text(encoding="utf-8"))
    dialogue["entries"] = [entry for entry in dialogue["entries"] if not entry["id"].startswith("context.")]
    dialogue["entries"].extend(context_entries)
    DIALOGUE.write_text(json.dumps(dialogue, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Generated {len(definitions)} multi-stage contextual definitions and {len(context_entries)} authored English contextual dialogue entries.")


if __name__ == "__main__":
    main()
