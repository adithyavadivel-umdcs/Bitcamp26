package com.example.bitcamp26.core.model;

/**
 * Mirrors lobbies/{lobbyId}/hotspots/{hotspotId} in Firebase Realtime Database.
 */
public class Hotspot {
    public String id;
    public double lat;
    public double lng;
    public double radiusMeters;
    public boolean active;
    public String claimedBy;   // userId or null
    public Long claimedAt;     // timestamp or null
    public String powerupType; // "HIDER_INVISIBILITY" | "SEEKER_REVEAL_ALL"

    // Required by Firebase
    public Hotspot() {}

    public Hotspot(String id, double lat, double lng, double radiusMeters, String powerupType) {
        this.id = id;
        this.lat = lat;
        this.lng = lng;
        this.radiusMeters = radiusMeters;
        this.active = true;
        this.claimedBy = null;
        this.claimedAt = null;
        this.powerupType = powerupType;
    }

    public PowerupType getPowerupType() {
        if (powerupType == null) return PowerupType.NONE;
        switch (powerupType) {
            case "HIDER_INVISIBILITY": return PowerupType.HIDER_INVISIBILITY;
            case "SEEKER_REVEAL_ALL": return PowerupType.SEEKER_REVEAL_ALL;
            default: return PowerupType.NONE;
        }
    }
}
