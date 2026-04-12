package com.example.bitcamp26.core.model;

public enum PowerupType {
    NONE,
    /** Hider: seeker-specific visibility for this hider is reduced to 0.6x the base amount. */
    HIDER_VISION_REDUCTION,
    /** Seeker: minimap width and height are both doubled while active. */
    SEEKER_MINIMAP_BOOST,
    /** Legacy alias kept for compatibility with older snapshots and unfinished UI wiring. */
    @Deprecated
    HIDER_INVISIBILITY,
    /** Legacy alias kept for compatibility with older snapshots and unfinished UI wiring. */
    @Deprecated
    SEEKER_REVEAL_ALL;

    public boolean isHiderVisionReduction() {
        return this == HIDER_VISION_REDUCTION || this == HIDER_INVISIBILITY;
    }

    public boolean isSeekerMinimapBoost() {
        return this == SEEKER_MINIMAP_BOOST || this == SEEKER_REVEAL_ALL;
    }
}
