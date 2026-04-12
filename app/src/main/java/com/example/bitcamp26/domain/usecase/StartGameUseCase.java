package com.example.bitcamp26.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.HotspotState;
import com.example.bitcamp26.core.model.Lobby;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.util.TimeUitls;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Handles the business rules for starting a match from a lobby.
 *
 * Seeker assignment is now RANDOMIZED — any player in the lobby
 * has an equal chance of being selected as seeker. Everyone else
 * is assigned HIDER.
 */
public class StartGameUseCase {

    private static final int DEFAULT_MATCH_DURATION_SECONDS = 300;

    private final Random random;

    public StartGameUseCase() {
        this.random = new Random();
    }

    /** Testable constructor — inject a seeded Random for deterministic tests. */
    public StartGameUseCase(@NonNull Random random) {
        this.random = random;
    }

    /**
     * Attempts to start a game using the provided lobby and hotspots.
     */
    public StartGameResult execute(@Nullable Lobby lobby,
                                   @Nullable List<HotspotState> hotspots) {
        if (lobby == null) {
            return StartGameResult.failure("Lobby is missing.");
        }

        List<Player> players = lobby.getPlayers();
        if (players == null || players.isEmpty()) {
            return StartGameResult.failure("At least one player is required to start the game.");
        }

        if (lobby.isStarted()) {
            return StartGameResult.failure("The game has already started.");
        }

        // Always reassign roles from scratch using random seeker selection
        assignRolesWithRandomSeeker(players);

        GameState gameState = new GameState();
        gameState.setStarted(true);
        gameState.setFinished(false);
        gameState.setScore(0);
        gameState.setStartedAt(TimeUitls.nowMillis());
        gameState.setEndsAt(TimeUitls.secondsFromNow(resolveMatchDurationSeconds(lobby)));
        gameState.setPlayers(players);
        gameState.setHotspots(copyHotspots(hotspots));

        lobby.setStarted(true);
        lobby.setPlayerCount(players.size());

        // Record who was selected as seeker for logging/debugging
        String seekerId = findSeekerId(players);

        return StartGameResult.success(lobby, gameState, seekerId);
    }

    /**
     * Assigns roles randomly: one player becomes SEEKER, all others become HIDER.
     * If only one player exists they become the seeker (edge case).
     */
    public void assignRolesWithRandomSeeker(@NonNull List<Player> players) {
        // Filter out nulls for safety
        List<Player> valid = new ArrayList<>();
        for (Player p : players) {
            if (p != null) valid.add(p);
        }
        if (valid.isEmpty()) return;

        // Pick a random index as seeker
        int seekerIndex = random.nextInt(valid.size());

        for (int i = 0; i < valid.size(); i++) {
            valid.get(i).setRole(i == seekerIndex ? PlayerRole.SEEKER : PlayerRole.HIDER);
        }
    }

    /**
     * Returns true if at least one seeker and one hider exist.
     */
    public boolean hasRequiredRoles(@NonNull List<Player> players) {
        boolean hasSeeker = false;
        boolean hasHider  = false;
        for (Player player : players) {
            if (player == null || player.getRole() == null) continue;
            if (player.getRole() == PlayerRole.SEEKER) hasSeeker = true;
            else if (player.getRole() == PlayerRole.HIDER) hasHider = true;
        }
        return hasSeeker && hasHider;
    }

    /**
     * Legacy method kept for compatibility — use assignRolesWithRandomSeeker instead.
     * Now delegates to random assignment.
     */
    public void assignMissingRoles(@NonNull List<Player> players) {
        assignRolesWithRandomSeeker(players);
    }

    public long resolveMatchDurationSeconds(@NonNull Lobby lobby) {
        long durationSeconds = lobby.getMatchDurationSeconds();
        return durationSeconds > 0 ? durationSeconds : DEFAULT_MATCH_DURATION_SECONDS;
    }

    @NonNull
    public List<HotspotState> copyHotspots(@Nullable List<HotspotState> hotspots) {
        if (hotspots == null || hotspots.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(hotspots);
    }

    @Nullable
    private String findSeekerId(@NonNull List<Player> players) {
        for (Player p : players) {
            if (p != null && p.getRole() == PlayerRole.SEEKER) return p.getId();
        }
        return null;
    }

    // ── Result ────────────────────────────────────────────────────────────────

    public static final class StartGameResult {
        private final boolean success;
        private final String message;
        private final Lobby updatedLobby;
        private final GameState gameState;
        private final String seekerId;

        private StartGameResult(boolean success,
                                @NonNull String message,
                                @Nullable Lobby updatedLobby,
                                @Nullable GameState gameState,
                                @Nullable String seekerId) {
            this.success      = success;
            this.message      = message;
            this.updatedLobby = updatedLobby;
            this.gameState    = gameState;
            this.seekerId     = seekerId;
        }

        public static StartGameResult success(@NonNull Lobby lobby,
                                              @NonNull GameState gameState,
                                              @Nullable String seekerId) {
            return new StartGameResult(true, "Game started successfully.", lobby, gameState, seekerId);
        }

        /** Legacy overload for callers that don't need seekerId. */
        public static StartGameResult success(@NonNull Lobby lobby,
                                              @NonNull GameState gameState) {
            return success(lobby, gameState, null);
        }

        public static StartGameResult failure(@NonNull String message) {
            return new StartGameResult(false, message, null, null, null);
        }

        public boolean isSuccess()              { return success; }
        @NonNull public String getMessage()     { return message; }
        @Nullable public Lobby getUpdatedLobby(){ return updatedLobby; }
        @Nullable public GameState getGameState(){ return gameState; }
        @Nullable public String getSeekerId()   { return seekerId; }
    }
}
