package com.example.bitcamp26.core.model;

/**
 * Represents a single player in a lobby or active match.
 * Must have a no-arg constructor for Firebase deserialization.
 */
public class Player {

    private String id;
    private String displayName;
    private boolean caught;
    private String caughtBy;
    private long caughtAt;
    private String catchCode;
    private PlayerRole role;
    private double latitude;
    private double longitude;
    private long lastLocationUpdatedAt;
    private PowerupType heldPowerup;
    private PowerupType activePowerup;
    private long powerupActivatedAt;
    private long powerupExpiresAt;

    public Player() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isCaught() {
        return caught;
    }

    public void setCaught(boolean caught) {
        this.caught = caught;
    }

    public String getCaughtBy() {
        return caughtBy;
    }

    public void setCaughtBy(String caughtBy) {
        this.caughtBy = caughtBy;
    }

    public long getCaughtAt() {
        return caughtAt;
    }

    public void setCaughtAt(long caughtAt) {
        this.caughtAt = caughtAt;
    }

    public String getCatchCode() {
        return catchCode;
    }

    public void setCatchCode(String catchCode) {
        this.catchCode = catchCode;
    }

    public PlayerRole getRole() {
        return role;
    }

    public void setRole(PlayerRole role) {
        this.role = role;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getLastLocationUpdatedAt() {
        return lastLocationUpdatedAt;
    }

    public void setLastLocationUpdatedAt(long lastLocationUpdatedAt) {
        this.lastLocationUpdatedAt = lastLocationUpdatedAt;
    }

    public PowerupType getHeldPowerup() {
        return heldPowerup;
    }

    public void setHeldPowerup(PowerupType heldPowerup) {
        this.heldPowerup = heldPowerup;
    }

    public PowerupType getActivePowerup() {
        return activePowerup;
    }

    public void setActivePowerup(PowerupType activePowerup) {
        this.activePowerup = activePowerup;
    }

    public long getPowerupActivatedAt() {
        return powerupActivatedAt;
    }

    public void setPowerupActivatedAt(long powerupActivatedAt) {
        this.powerupActivatedAt = powerupActivatedAt;
    }

    public long getPowerupExpiresAt() {
        return powerupExpiresAt;
    }

    public void setPowerupExpiresAt(long powerupExpiresAt) {
        this.powerupExpiresAt = powerupExpiresAt;
    }
}
