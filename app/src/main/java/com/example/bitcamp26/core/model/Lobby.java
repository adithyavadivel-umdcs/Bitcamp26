package com.example.bitcamp26.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Compatibility lobby model used by the current fragments while the backend
 * stores a richer RTDB shape.
 */
public class Lobby {

    private String code;
    private String inviteCode;
    private String state;
    private String hostId;
    private String seekerId;
    private boolean started;
    private int maxPlayers;
    private long matchDurationSeconds;
    private long createdAt;
    private long gameStartAt;
    private long nextShrinkAt;
    private double mapCenterLat;
    private double mapCenterLng;
    private double initialRadiusMeters;
    private double currentRadiusMeters;
    private boolean allReady;
    private List<Player> players;
    private int playerCount;

    public Lobby() {
        this.players = new ArrayList<>();
    }

    public String getCode() {
        return inviteCode != null && !inviteCode.trim().isEmpty() ? inviteCode : code;
    }

    public void setCode(String code) {
        this.code = code;
        this.inviteCode = code;
    }

    public String getInviteCode() {
        return getCode();
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
        this.code = inviteCode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
        this.started = "RUNNING".equalsIgnoreCase(state) || "ENDED".equalsIgnoreCase(state);
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getSeekerId() {
        return seekerId;
    }

    public void setSeekerId(String seekerId) {
        this.seekerId = seekerId;
    }

    public boolean isStarted() {
        return started || "RUNNING".equalsIgnoreCase(state) || "ENDED".equalsIgnoreCase(state);
    }

    public void setStarted(boolean started) {
        this.started = started;
        if (started && (state == null || state.trim().isEmpty() || "WAITING".equalsIgnoreCase(state))) {
            this.state = "RUNNING";
        }
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public long getMatchDurationSeconds() {
        return matchDurationSeconds;
    }

    public void setMatchDurationSeconds(long matchDurationSeconds) {
        this.matchDurationSeconds = matchDurationSeconds;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getGameStartAt() {
        return gameStartAt;
    }

    public void setGameStartAt(long gameStartAt) {
        this.gameStartAt = gameStartAt;
    }

    public long getNextShrinkAt() {
        return nextShrinkAt;
    }

    public void setNextShrinkAt(long nextShrinkAt) {
        this.nextShrinkAt = nextShrinkAt;
    }

    public double getMapCenterLat() {
        return mapCenterLat;
    }

    public void setMapCenterLat(double mapCenterLat) {
        this.mapCenterLat = mapCenterLat;
    }

    public double getMapCenterLng() {
        return mapCenterLng;
    }

    public void setMapCenterLng(double mapCenterLng) {
        this.mapCenterLng = mapCenterLng;
    }

    public double getInitialRadiusMeters() {
        return initialRadiusMeters;
    }

    public void setInitialRadiusMeters(double initialRadiusMeters) {
        this.initialRadiusMeters = initialRadiusMeters;
    }

    public double getCurrentRadiusMeters() {
        return currentRadiusMeters;
    }

    public void setCurrentRadiusMeters(double currentRadiusMeters) {
        this.currentRadiusMeters = currentRadiusMeters;
    }

    public boolean isAllReady() {
        return allReady;
    }

    public void setAllReady(boolean allReady) {
        this.allReady = allReady;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players != null ? players : new ArrayList<>();
        this.playerCount = this.players.size();
    }

    public int getPlayerCount() {
        return playerCount > 0 ? playerCount : (players != null ? players.size() : 0);
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }
}
