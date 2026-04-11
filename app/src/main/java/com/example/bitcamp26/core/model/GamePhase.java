package com.example.bitcamp26.core.model;

/**
 * Represents the lifecycle phase of a game session.
 * Previously named GameState — renamed to free that name for the match data POJO.
 */
public enum GamePhase {
    /** Lobby exists, players are joining */
    LOBBY,

    /** All players ready, waiting for host to start */
    READY_CHECK,

    /** Head start: seeker is frozen, hiders are running */
    HEAD_START,

    /** Active match in progress */
    ACTIVE,

    /** Match is over, winner determined */
    FINISHED
}
