package com.example.bitcamp26.core.model;

import java.util.List;

/**
 * Represents the live state of an active match.
 * Must have a no-arg constructor for Firebase deserialization.
 */
public class GameState {

    private boolean started;
    private boolean finished;
    private int score;
    private long startedAt;
    private long endsAt;
    private List<Player> players;
    private List<HotspotState> hotspots;

    public GameState() {
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(long endsAt) {
        this.endsAt = endsAt;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public List<HotspotState> getHotspots() {
        return hotspots;
    }

    public void setHotspots(List<HotspotState> hotspots) {
        this.hotspots = hotspots;
    }
}
