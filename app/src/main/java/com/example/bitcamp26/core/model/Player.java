package com.example.bitcamp26.core.model;

/**
 * Compatibility player model used by the current UI while RTDB stores the
 * backend-authoritative shape.
 */
public class Player {

    private String id;
    private String userId;
    private String displayName;
    private boolean ready;
    private boolean caught;
    private boolean alive = true;
    private boolean connected = true;
    private String caughtBy;
    private long caughtAt;
    private String catchCode;
    private boolean catchEligible;
    private PlayerRole role = PlayerRole.UNASSIGNED;
    private double latitude;
    private double longitude;
    private double accuracyMeters;
    private long lastLocationUpdatedAt;
    private PowerupType heldPowerup = PowerupType.NONE;
    private PowerupType activePowerup = PowerupType.NONE;
    private long powerupActivatedAt;
    private long powerupExpiresAt;
    private long nextPowerupEligibleAt;

    public Player() {
    }

    public String getId() {
        return userId != null && !userId.trim().isEmpty() ? userId : id;
    }

    public void setId(String id) {
        this.id = id;
        this.userId = id;
    }

    public String getUserId() {
        return getId();
    }

    public void setUserId(String userId) {
        this.userId = userId;
        this.id = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isCaught() {
        return caught || !alive;
    }

    public void setCaught(boolean caught) {
        this.caught = caught;
        this.alive = !caught;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
        this.caught = !alive;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
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

    public boolean isCatchEligible() {
        return catchEligible;
    }

    public void setCatchEligible(boolean catchEligible) {
        this.catchEligible = catchEligible;
    }

    public PlayerRole getRole() {
        return role;
    }

    public void setRole(PlayerRole role) {
        this.role = role != null ? role : PlayerRole.UNASSIGNED;
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

    public double getAccuracyMeters() {
        return accuracyMeters;
    }

    public void setAccuracyMeters(double accuracyMeters) {
        this.accuracyMeters = accuracyMeters;
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
        this.heldPowerup = heldPowerup != null ? heldPowerup : PowerupType.NONE;
    }

    public PowerupType getActivePowerup() {
        return activePowerup;
    }

    public void setActivePowerup(PowerupType activePowerup) {
        this.activePowerup = activePowerup != null ? activePowerup : PowerupType.NONE;
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

    public long getNextPowerupEligibleAt() {
        return nextPowerupEligibleAt;
    }

    public void setNextPowerupEligibleAt(long nextPowerupEligibleAt) {
        this.nextPowerupEligibleAt = nextPowerupEligibleAt;
    }
}
