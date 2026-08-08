package com.riftcompanions.social;

import com.riftcompanions.entity.CompanionAction;

/** Safe, non-authoritative social/world cue vocabulary. */
public enum SocialCue {
    CAMPFIRE("social_campfire_lead", "social_campfire_reply", CompanionAction.SOCIAL_CAMPFIRE),
    WEATHER("social_weather_lead", "social_weather_reply", CompanionAction.SOCIAL_WEATHER),
    HORIZON("social_horizon_lead", "social_horizon_reply", CompanionAction.SOCIAL_HORIZON),
    BASE("social_base_lead", "social_base_reply", CompanionAction.SOCIAL_BASE),
    WORK("social_work_lead", "social_work_reply", CompanionAction.SOCIAL_WORK),
    CAVE("social_cave_lead", "social_cave_reply", CompanionAction.SOCIAL_CAVE),
    VILLAGE("social_village_lead", "social_village_reply", CompanionAction.SOCIAL_VILLAGE),
    TRAVEL("social_travel_lead", "social_travel_reply", CompanionAction.SOCIAL_TRAVEL),
    CALM("social_calm_lead", "social_calm_reply", CompanionAction.SOCIAL_CALM);

    private final String leadTrigger;
    private final String replyTrigger;
    private final CompanionAction leadAction;

    SocialCue(final String leadTrigger, final String replyTrigger, final CompanionAction leadAction) {
        this.leadTrigger = leadTrigger;
        this.replyTrigger = replyTrigger;
        this.leadAction = leadAction;
    }

    public String leadTrigger() { return leadTrigger; }
    public String replyTrigger() { return replyTrigger; }
    public CompanionAction leadAction() { return leadAction; }
}
