package com.example.bitcamp26.core.model;

/**
 * Mirrors lobbies/{lobbyId}/players/{userId} in Firebase Realtime Database.
 * Firebase requires a no-arg constructor and public fields (or getters/setters).
 */
public class Player {
    public String userId;
    public String displayName;
    public String role;           // "SEEKER" | "HIDER" | "UNASSIGNED"
    public boolean alive;
    public boolean connected;
    public boolean ready;
    public double lat;
    public double lng;
    public double accuracyMeters;
    public long lastLocationAt;
    public long stepsAtGameStart;
    public long currentMatchSteps;
    public String activePowerup;  // "NONE" | "HIDER_INVISIBILITY" | "SEEKER_REVEAL_ALL"
    public Long powerupEndsAt;    // null when no active powerup
    public String catchCode;      // 6-digit code, null for seeker

    // Required by Firebase
    public Player() {}

    public Player(String userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
        this.role = "UNASSIGNED";
        this.alive = true;
        this.connected = true;
        this.ready = false;
        this.lat = 0;
        this.lng = 0;
        this.accuracyMeters = 0;
        this.lastLocationAt = 0;
        this.stepsAtGameStart = 0;
        this.currentMatchSteps = 0;
        this.activePowerup = "NONE";
        this.powerupEndsAt = null;
        this.catchCode = null;
    }

    public PlayerRole getRole() {
        if (role == null) return PlayerRole.UNASSIGNED;
        switch (role) {
            case "SEEKER": return PlayerRole.SEEKER;
            case "HIDER": return PlayerRole.HIDER;
            default: return PlayerRole.UNASSIGNED;
        }
    }

    public PowerupType getActivePowerup() {
        if (activePowerup == null) return PowerupType.NONE;
        switch (activePowerup) {
            case "HIDER_INVISIBILITY": return PowerupType.HIDER_INVISIBILITY;
            case "SEEKER_REVEAL_ALL": return PowerupType.SEEKER_REVEAL_ALL;
            default: return PowerupType.NONE;
        }
    }
}
