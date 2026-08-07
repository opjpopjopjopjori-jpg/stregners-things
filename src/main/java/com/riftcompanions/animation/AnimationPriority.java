package com.riftcompanions.animation;

/** Higher values pre-empt lower-priority visual presentation only. */
public enum AnimationPriority {
    AMBIENT(10),
    LOCOMOTION(20),
    INTERACTION(30),
    COMBAT(40),
    POWER(50),
    RESCUE(60),
    DOWNED(100);

    private final int value;

    AnimationPriority(final int value) {
        this.value = value;
    }

    public int value() { return value; }
}
