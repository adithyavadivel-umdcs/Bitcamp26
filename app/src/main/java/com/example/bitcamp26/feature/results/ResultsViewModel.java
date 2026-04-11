package com.example.bitcamp26.feature.results;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.util.TimeUitls;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for the end-of-match results screen.
 *
 * Responsibilities:
 * - hold the final GameState
 * - track the current player ID
 * - expose derived UI state such as title, subtitle, winner text, score, duration,
 *   and player summary
 * - expose simple replay / return-to-lobby events as status messages for MVP wiring
 *
 * This class is intentionally self-contained so it works even before a full
 * repository-backed results/history system exists.
 */
public class ResultsViewModel extends ViewModel {

    private final MutableLiveData<GameState> gameState = new MutableLiveData<>();
    private final MutableLiveData<String> currentPlayerId = new MutableLiveData<>();

    private final MutableLiveData<String> titleText = new MutableLiveData<>("Match Results");
    private final MutableLiveData<String> subtitleText = new MutableLiveData<>("Review the final outcome of the match.");
    private final MutableLiveData<String> scoreText = new MutableLiveData<>("Final Score: 0");
    private final MutableLiveData<String> durationText = new MutableLiveData<>("Duration: --");
    private final MutableLiveData<String> winnerText = new MutableLiveData<>("Winner: TBD");
    private final MutableLiveData<String> playersSummaryText = new MutableLiveData<>("No player summary available.");
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("Results ready.");
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ResultsViewModel() {
        seedPlaceholderResultsIfNeeded();
        recalculateDerivedState();
    }

    @NonNull
    public LiveData<GameState> getGameState() {
        return gameState;
    }

    @NonNull
    public LiveData<String> getCurrentPlayerId() {
        return currentPlayerId;
    }

    @NonNull
    public LiveData<String> getTitleText() {
        return titleText;
    }

    @NonNull
    public LiveData<String> getSubtitleText() {
        return subtitleText;
    }

    @NonNull
    public LiveData<String> getScoreText() {
        return scoreText;
    }

    @NonNull
    public LiveData<String> getDurationText() {
        return durationText;
    }

    @NonNull
    public LiveData<String> getWinnerText() {
        return winnerText;
    }

    @NonNull
    public LiveData<String> getPlayersSummaryText() {
        return playersSummaryText;
    }

    @NonNull
    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    @NonNull
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the completed game state and refreshes all derived UI values.
     */
    public void setGameState(@Nullable GameState state) {
        gameState.setValue(state);
        errorMessage.setValue(null);
        recalculateDerivedState();
    }

    /**
     * Sets the current player ID and refreshes all derived UI values.
     */
    public void setCurrentPlayerId(@Nullable String playerId) {
        currentPlayerId.setValue(playerId);
        recalculateDerivedState();
    }

    /**
     * Recomputes every user-facing string derived from the current state.
     */
    public void refresh() {
        recalculateDerivedState();
    }

    /**
     * Placeholder action for replay flow.
     */
    public void requestPlayAgain() {
        statusMessage.setValue("Replay flow not wired yet.");
    }

    /**
     * Placeholder action for return-to-lobby flow.
     */
    public void requestReturnToLobby() {
        statusMessage.setValue("Return-to-lobby flow not wired yet.");
    }

    @Nullable
    public Player getCurrentPlayerValue() {
        GameState state = gameState.getValue();
        String playerId = currentPlayerId.getValue();
        return findPlayerById(state != null ? state.getPlayers() : null, playerId);
    }

    @NonNull
    public List<Player> getPlayersSnapshot() {
        GameState state = gameState.getValue();
        if (state == null || state.getPlayers() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(state.getPlayers());
    }

    private void recalculateDerivedState() {
        GameState state = gameState.getValue();

        if (state == null) {
            titleText.setValue("Match Results");
            subtitleText.setValue("No completed game data available.");
            scoreText.setValue("Final Score: 0");
            durationText.setValue("Duration: --");
            winnerText.setValue("Winner: TBD");
            playersSummaryText.setValue("No player summary available.");
            return;
        }

        titleText.setValue(resolveTitle(state));
        subtitleText.setValue(resolveSubtitle(state));
        scoreText.setValue("Final Score: " + state.getScore());
        durationText.setValue("Duration: " + resolveDurationText(state));
        winnerText.setValue("Winner: " + resolveWinnerText(state));
        playersSummaryText.setValue(buildPlayersSummary(state.getPlayers(), currentPlayerId.getValue()));
    }

    @NonNull
    private String resolveTitle(@NonNull GameState state) {
        if (!state.isFinished()) {
            return "Match In Progress";
        }

        Player currentPlayer = findPlayerById(state.getPlayers(), currentPlayerId.getValue());
        if (currentPlayer == null || currentPlayer.getRole() == null) {
            return "Match Results";
        }

        boolean seekersWon = areAllHidersCaught(state.getPlayers());
        if ((seekersWon && currentPlayer.getRole() == PlayerRole.SEEKER)
                || (!seekersWon && currentPlayer.getRole() == PlayerRole.HIDER)) {
            return "You Win";
        }

        return "You Lose";
    }

    @NonNull
    private String resolveSubtitle(@NonNull GameState state) {
        if (!state.isFinished()) {
            return "The match has not been finalized yet.";
        }

        boolean seekersWon = areAllHidersCaught(state.getPlayers());
        if (seekersWon) {
            return "All hiders were caught before time ran out.";
        }

        return "At least one hider survived until the end of the match.";
    }

    @NonNull
    private String resolveDurationText(@NonNull GameState state) {
        long startedAt = state.getStartedAt();
        long endedAt = state.getEndsAt();

        if (startedAt <= 0L || endedAt <= startedAt) {
            return "Unavailable";
        }

        long durationMillis = endedAt - startedAt;
        return TimeUitls.formatDurationHuman(durationMillis);
    }

    @NonNull
    private String resolveWinnerText(@NonNull GameState state) {
        return areAllHidersCaught(state.getPlayers()) ? "Seekers" : "Hiders";
    }

    @NonNull
    private String buildPlayersSummary(@Nullable List<Player> players,
                                       @Nullable String playerId) {
        if (players == null || players.isEmpty()) {
            return "No player summary available.";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player == null) {
                continue;
            }

            builder.append(i + 1)
                    .append(". ")
                    .append(resolvePlayerName(player));

            if (player.getId() != null && player.getId().equals(playerId)) {
                builder.append(" (You)");
            }

            builder.append("\n   Role: ")
                    .append(player.getRole() != null ? player.getRole().name() : "UNKNOWN")
                    .append("\n   Caught: ")
                    .append(player.isCaught() ? "Yes" : "No");

            if (player.getCaughtBy() != null && !player.getCaughtBy().trim().isEmpty()) {
                builder.append("\n   Caught By: ").append(player.getCaughtBy());
            }

            if (player.getHeldPowerup() != null) {
                builder.append("\n   Held Powerup: ").append(player.getHeldPowerup().name());
            }

            if (player.getActivePowerup() != null) {
                builder.append("\n   Active Powerup: ").append(player.getActivePowerup().name());
            }

            if (i < players.size() - 1) {
                builder.append("\n\n");
            }
        }

        return builder.toString();
    }

    @Nullable
    private Player findPlayerById(@Nullable List<Player> players,
                                  @Nullable String playerId) {
        if (players == null || players.isEmpty() || playerId == null || playerId.trim().isEmpty()) {
            return null;
        }

        for (Player player : players) {
            if (player == null || player.getId() == null) {
                continue;
            }
            if (playerId.equals(player.getId())) {
                return player;
            }
        }
        return null;
    }

    private boolean areAllHidersCaught(@Nullable List<Player> players) {
        if (players == null || players.isEmpty()) {
            return false;
        }

        boolean foundHider = false;
        for (Player player : players) {
            if (player == null || player.getRole() == null) {
                continue;
            }

            if (player.getRole() == PlayerRole.HIDER) {
                foundHider = true;
                if (!player.isCaught()) {
                    return false;
                }
            }
        }

        return foundHider;
    }

    @NonNull
    private String resolvePlayerName(@NonNull Player player) {
        String displayName = player.getDisplayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            return "Player";
        }
        return displayName.trim();
    }

    private void seedPlaceholderResultsIfNeeded() {
        if (gameState.getValue() != null) {
            return;
        }

        GameState state = new GameState();
        state.setStarted(true);
        state.setFinished(true);
        state.setScore(45);

        long startedAt = TimeUitls.nowMillis() - (5 * 60 * 1000L);
        long endedAt = TimeUitls.nowMillis();
        state.setStartedAt(startedAt);
        state.setEndsAt(endedAt);

        List<Player> players = new ArrayList<>();

        Player p1 = new Player();
        p1.setId("player_me");
        p1.setDisplayName("My Player");
        p1.setRole(PlayerRole.SEEKER);
        p1.setCaught(false);

        Player p2 = new Player();
        p2.setId("player_2");
        p2.setDisplayName("Runner One");
        p2.setRole(PlayerRole.HIDER);
        p2.setCaught(true);
        p2.setCaughtBy("player_me");

        Player p3 = new Player();
        p3.setId("player_3");
        p3.setDisplayName("Runner Two");
        p3.setRole(PlayerRole.HIDER);
        p3.setCaught(true);
        p3.setCaughtBy("player_me");

        players.add(p1);
        players.add(p2);
        players.add(p3);

        state.setPlayers(players);

        gameState.setValue(state);
        currentPlayerId.setValue("player_me");
    }
}
