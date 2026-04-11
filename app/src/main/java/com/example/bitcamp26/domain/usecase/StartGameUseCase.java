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

/**
 * Handles the business rules for starting a match from a lobby.
 */
public class StartGameUseCase {

    private static final int DEFAULT_MATCH_DURATION_SECONDS = 300;

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

        if (!hasRequiredRoles(players)) {
            assignMissingRoles(players);
        }

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

        return StartGameResult.success(lobby, gameState);
    }

    /**
     * Returns true if at least one seeker and one hider exist.
     */
    public boolean hasRequiredRoles(@NonNull List<Player> players) {
        boolean hasSeeker = false;
        boolean hasHider = false;

        for (Player player : players) {
            if (player == null || player.getRole() == null) {
                continue;
            }

            if (player.getRole() == PlayerRole.SEEKER) {
                hasSeeker = true;
            } else if (player.getRole() == PlayerRole.HIDER) {
                hasHider = true;
            }
        }

        return hasSeeker && hasHider;
    }

    /**
     * Assigns missing roles using a simple rule: first player is seeker, everyone else is hider.
     */
    public void assignMissingRoles(@NonNull List<Player> players) {
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player == null) {
                continue;
            }

            player.setRole(i == 0 ? PlayerRole.SEEKER : PlayerRole.HIDER);
        }
    }

    /**
     * Resolves match duration from the lobby or falls back to a default.
     */
    public long resolveMatchDurationSeconds(@NonNull Lobby lobby) {
        long durationSeconds = lobby.getMatchDurationSeconds();
        return durationSeconds > 0 ? durationSeconds : DEFAULT_MATCH_DURATION_SECONDS;
    }

    /**
     * Returns a safe copy of hotspot data for the game state.
     */
    @NonNull
    public List<HotspotState> copyHotspots(@Nullable List<HotspotState> hotspots) {
        if (hotspots == null || hotspots.isEmpty()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(hotspots);
    }

    public static final class StartGameResult {
        private final boolean success;
        private final String message;
        private final Lobby updatedLobby;
        private final GameState gameState;

        private StartGameResult(boolean success,
                                @NonNull String message,
                                @Nullable Lobby updatedLobby,
                                @Nullable GameState gameState) {
            this.success = success;
            this.message = message;
            this.updatedLobby = updatedLobby;
            this.gameState = gameState;
        }

        public static StartGameResult success(@NonNull Lobby lobby,
                                              @NonNull GameState gameState) {
            return new StartGameResult(true, "Game started successfully.", lobby, gameState);
        }

        public static StartGameResult failure(@NonNull String message) {
            return new StartGameResult(false, message, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        @NonNull
        public String getMessage() {
            return message;
        }

        @Nullable
        public Lobby getUpdatedLobby() {
            return updatedLobby;
        }

        @Nullable
        public GameState getGameState() {
            return gameState;
        }
    }
}
