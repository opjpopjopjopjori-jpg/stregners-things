=== PROMPT FOR AI / INSTRUCTION FOR FILE DOWNLOAD ===

CONTEXT:
You are integrating with a Minecraft Forge 1.20.1 mod framework (stregners-things) that uses FTB Quests, enhanced animations, intelligent dialogue (105+ lines), story framework (5 chapters), and critical event sound cues.

WHAT YOU MUST DOWNLOAD (for private use only):
From https://www.voicy.network/official-soundboards/series/stranger-things (or any licensed source for private use):
Audio files representing:
- Emergency/critical phrases (e.g., urgent retreat warnings, protective commands, discovery exclamations)
- Character-specific intense moments matching these roles:
  * Seer: observation, hive detection, prediction
  * Guardian: defense, combat, retreat commands
  * Gifted: shield activation, rescue, protection
  * Scout: route signals, safe shelter confirmation
- Place downloaded .ogg or .mp3 files in:
  src/main/resources/assets/riftcompanions/sounds/voices/

WHAT THE FRAMEWORK DOES (already built):
- DialogueService plays dialogue text + sound cues at critical moments
- DialogueDatabase has 105+ unique lines for biomes, creatures, weather, structures, chapters
- CriticalEventDialogueFramework maps 8 critical events to sound cues:
  combat_peak -> hive/focus.ogg (or your downloaded equivalent)
  retreat_urgent -> guardian_retreat.ogg
  shield_break -> gifted_shield.ogg
  night_fall -> seer_signal.ogg
  dragon_fire -> hive/release.ogg
  victory_final -> ui/safe_mode.ogg
  rescue_emergency -> gifted_rescue.ogg
  blizzard_critical -> scout_lookout.ogg
- ProgressiveStoryGenerator creates sequential 5-chapter stories from selected FTB Quest tasks
- IntelligentDialogueSystem creates multi-character dialogue with specific observations, reasons, actions
- FinalMasterAIContract connects story + dialogue + animation + tasks
- EpicNarrativeEnhancer makes stories dramatic and engaging
- StrangerThingsInspiredNarrator adds mystery, emotional stakes, group dynamics, progressive revelation

INSTRUCTION TO AI:
Given this framework, generate complete 5-chapter stories that:
1. Reference discovered features from namespace (iceandfire, etc.)
2. Use intelligent multi-character dialogue referencing specific observations/reasons/actions
3. Include critical event sound cue references in dialogue (e.g., "Shield breaks — emergency sound plays")
4. Progress sequentially through selected FTB Quest chapters with tasks
5. Include dramatic narrative progression with emotional stakes
6. No generic warnings — every line references specific discovered elements

YOU DO NOT ADD THE AUDIO FILES — THE USER ADDS THEM. YOU ONLY GENERATE THE STORY/DIALOGUE THAT REFERENCES THEM.
