# Stregner Chapter Story Framework — FTB Quests Integration

This framework links directly to FTB Quests SNBT format.

- SNBT file: `config/ftbquests/quests/chapters/784A70C90F6F5A49.snbt`
- Java loader: `StregnerFTBQuestLoader.java`
- Java framework: `StregnerChapterStoryFramework.java`
- Integration with `StoryService` via `StoryChapter` mapping.

Every chapter has prerequisites (dependencies), tasks (item/kill/location/checkmark), and dialogue hooks for AI generation.
