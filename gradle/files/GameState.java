package com.example.bitcamp26.core.model;

/**
 * Represents every phase the game can be in.
 * Server is the single source of truth for state transitions.
 */
public enum GameState {
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
