=== STREGNER FIVE CHAPTER STORY FRAMEWORK — SEND THIS TO ANY AI ===

CONTEXT:
This is a Minecraft Forge 1.20.1 mod (Rift Companions) integrated with FTB Quests.
Read the files below, then generate a complete professional English story.

FILES TO READ:
1. config/ftbquests/quests/chapters/784A70C90F6F5A49.snbt (FTB Quests SNBT with 5 chapters, prerequisites, tasks, rewards)
2. src/main/java/com/riftcompanions/story/StregnerChapterStoryFramework.java (5 chapters with roles: Seer/Guardian/Gifted/Scout)
3. src/main/java/com/riftcompanions/story/StregnerFTBTaskMapper.java (task-to-dialogue mapping)

TASK SELECTION:
Before generating, select exactly 5 tasks numbered 5 to 10 using StregnerTaskSelectionInterface.
The AI must only use the selected 5 task numbers in the story.

CHAPTER STRUCTURE:
CH01 Gathering: collect food, scout area. Characters: Seer, Guardian. Event: Seer uses Hive Sense.
CH02 Discovery: find village or search structure. Characters: Seer, Scout. Event: if no village, travel distance; Seer searches again for biome/pyramid.
CH03 Combat: kill 10 zombies, agree arrow/concealment tactics. Characters: Guardian, Seer, Gifted. Dialogue: tactical plan discussion.
CH04 Armor Collection: collect armor, gather food. Characters: Scout, Seer.
CH05 Conclusion: command activates full story (/stregners story activate). Characters: all 4. Resolution dialogue.

PREREQUISITES ENFORCED: CH01 -> CH02 -> CH03 -> CH04 -> CH05
TASK TYPES: item, kill, location, structure, checkmark
DIALOGUE RULES: Every chapter references previous outcomes. Seer uses power for discovery/search. Guardian proposes defense. Gifted offers Shield with checks. Scout marks safe routes.

OUTPUT FROM AI:
Generate complete sequential English dialogue, event descriptions, prerequisite checks, tactical discussions (arrow/concealment for zombies), village discovery logic, and a final command-activated resolution linking all 5 chapters.
