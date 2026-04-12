package com.example.bitcamp26.core.model;

import java.util.List;

/**
 * Represents a game lobby stored in Firebase Realtime Database.
 * Must have a no-arg constructor for Firebase deserialization.
 */
public class Lobby {

    private String code;
    private boolean started;
    private int maxPlayers;
    private long matchDurationSeconds;
    private List<Player> players;
    private int playerCount;

    // Boundary info
    private double boundaryCenterLat;
    private double boundaryCenterLng;
    private double boundaryRadiusMeters;

    public Lobby() {
    }

    public double getBoundaryCenterLat() { return boundaryCenterLat; }
    public void setBoundaryCenterLat(double lat) { this.boundaryCenterLat = lat; }

    public double getBoundaryCenterLng() { return boundaryCenterLng; }
    public void setBoundaryCenterLng(double lng) { this.boundaryCenterLng = lng; }

    public double getBoundaryRadiusMeters() { return boundaryRadiusMeters; }
    public void setBoundaryRadiusMeters(double radius) { this.boundaryRadiusMeters = radius; }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
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

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }
}
