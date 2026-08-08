package com.riftcompanions.sound;

/**
 * Ambient Sound Signature Example — concrete mapping of event to sound file.
 * User must provide the actual .ogg file; framework handles playback.
 */
public final class SoundSignatureExample {
    private SoundSignatureExample() {}

    public static final String EXAMPLE_SIGNATURE = "storm_dark.ogg";
    public static final String EXAMPLE_DESCRIPTION = "Dark storm atmosphere — thunder rolling, rain striking stone, wind howling through empty structures.";
    public static final String EXAMPLE_EVENT = "blizzard_critical"; // From CriticalEventDialogueFramework
    public static final String EXAMPLE_CHAPTER = "CH03"; // Chapter where critical event may occur

    public static String playSignature() {
        return "SIGNATURE: " + EXAMPLE_SIGNATURE + "\n" +
               "EVENT: " + EXAMPLE_EVENT + " (Chapter: " + EXAMPLE_CHAPTER + ")\n" +
               "DESCRIPTION: " + EXAMPLE_DESCRIPTION + "\n" +
               "USAGE: Dialogue triggers this sound at critical moment via DialogueService + CriticalEventDialogueFramework.\n" +
               "NOTE: User must place storm_dark.ogg in sounds/voices/ for playback.";
    }
}
