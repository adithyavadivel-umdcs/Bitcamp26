
package com.example.bitcamp26.feature.results;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bitcamp26.R;
import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.util.TimeUitls;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that shows the end-of-match results.
 *
 * UI is defined in fragment_results.xml.
 *
 * What this screen shows:
 * - match result header (win/loss/completed)
 * - final score
 * - match timing summary
 * - player summary list
 * - simple winner/role interpretation
 * - buttons for replay / return flow hooks
 */
public class ResultsFragment extends Fragment {

    private TextView titleTextView;
    private TextView subtitleTextView;
    private TextView scoreTextView;
    private TextView timingTextView;
    private TextView winnerTextView;
    private TextView playersHeaderTextView;
    private TextView playersSummaryTextView;
    private TextView statusTextView;

    private Button playAgainButton;
    private Button returnToLobbyButton;

    private GameState currentGameState;
    private String currentPlayerId;

    public ResultsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        seedPlaceholderResultsIfNeeded();
        return inflater.inflate(R.layout.fragment_results, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        titleTextView = view.findViewById(R.id.textResultsTitle);
        subtitleTextView = view.findViewById(R.id.textResultsSubtitle);
        scoreTextView = view.findViewById(R.id.textResultsScore);
        timingTextView = view.findViewById(R.id.textResultsDuration);
        winnerTextView = view.findViewById(R.id.textResultsWinner);
        playersHeaderTextView = view.findViewById(R.id.textResultsPlayersHeader);
        playersSummaryTextView = view.findViewById(R.id.textResultsPlayersSummary);
        playAgainButton = view.findViewById(R.id.buttonPlayAgain);
        returnToLobbyButton = view.findViewById(R.id.buttonReturnToLobby);
        statusTextView = view.findViewById(R.id.textResultsStatus);

        bindListeners();
        refreshUi();
    }

    /**
     * Allows other parts of the app to provide the completed game state.
     */
    public void setGameState(@Nullable GameState gameState) {
        this.currentGameState = gameState;
        refreshUi();
    }

    /**
     * Allows other parts of the app to tell the screen who the current player is.
     */
    public void setCurrentPlayerId(@Nullable String currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
        refreshUi();
    }

    @Nullable
    public GameState getGameState() {
        return currentGameState;
    }

    @Nullable
    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    private void bindListeners() {
        playAgainButton.setOnClickListener(v -> {
            showStatus("Replay flow not wired yet.");
            showToast("Replay flow placeholder.");
        });

        returnToLobbyButton.setOnClickListener(v -> {
            showStatus("Return-to-lobby flow not wired yet.");
            showToast("Return to lobby placeholder.");
        });
    }

    private void refreshUi() {
        if (!isAdded()) {
            return;
        }

        if (currentGameState == null) {
            titleTextView.setText("Match Results");
            subtitleTextView.setText("No completed game data available.");
            scoreTextView.setText("Final Score: 0");
            timingTextView.setText("Duration: --");
            winnerTextView.setText("Winner: TBD");
            playersSummaryTextView.setText("No player summary available.");
            return;
        }

        titleTextView.setText(resolveTitle(currentGameState));
        subtitleTextView.setText(resolveSubtitle(currentGameState));
        scoreTextView.setText("Final Score: " + currentGameState.getScore());
        timingTextView.setText("Duration: " + resolveDurationText(currentGameState));
        winnerTextView.setText("Winner: " + resolveWinnerText(currentGameState));
        playersSummaryTextView.setText(buildPlayersSummary(currentGameState.getPlayers(), currentPlayerId));
    }

    @NonNull
    private String resolveTitle(@NonNull GameState gameState) {
        if (!gameState.isFinished()) {
            return "Match In Progress";
        }

        Player currentPlayer = findPlayerById(gameState.getPlayers(), currentPlayerId);
        if (currentPlayer == null || currentPlayer.getRole() == null) {
            return "Match Results";
        }

        boolean seekersWon = areAllHidersCaught(gameState.getPlayers());
        if ((seekersWon && currentPlayer.getRole() == PlayerRole.SEEKER)
                || (!seekersWon && currentPlayer.getRole() == PlayerRole.HIDER)) {
            return "You Win";
        }

        return "You Lose";
    }

    @NonNull
    private String resolveSubtitle(@NonNull GameState gameState) {
        if (!gameState.isFinished()) {
            return "The match has not been finalized yet.";
        }

        boolean seekersWon = areAllHidersCaught(gameState.getPlayers());
        if (seekersWon) {
            return "All hiders were caught before time ran out.";
        }

        return "At least one hider survived until the end of the match.";
    }

    @NonNull
    private String resolveDurationText(@NonNull GameState gameState) {
        long startedAt = gameState.getStartedAt();
        long endedAt = gameState.getEndsAt();

        if (startedAt <= 0L || endedAt <= startedAt) {
            return "Unavailable";
        }

        long durationMillis = endedAt - startedAt;
        return TimeUitls.formatDurationHuman(durationMillis);
    }

    @NonNull
    private String resolveWinnerText(@NonNull GameState gameState) {
        return areAllHidersCaught(gameState.getPlayers()) ? "Seekers" : "Hiders";
    }

    @NonNull
    private String buildPlayersSummary(@Nullable List<Player> players,
                                       @Nullable String currentPlayerId) {
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

            if (player.getId() != null && player.getId().equals(currentPlayerId)) {
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
        if (currentGameState != null) {
            return;
        }

        GameState gameState = new GameState();
        gameState.setStarted(true);
        gameState.setFinished(true);
        gameState.setScore(45);

        long startedAt = TimeUitls.nowMillis() - (5 * 60 * 1000L);
        long endedAt = TimeUitls.nowMillis();
        gameState.setStartedAt(startedAt);
        gameState.setEndsAt(endedAt);

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

        gameState.setPlayers(players);

        currentGameState = gameState;
        currentPlayerId = "player_me";
    }

    private void showStatus(@NonNull String message) {
        if (!isAdded()) {
            return;
        }
        statusTextView.setText("Status: " + message);
    }

    private void showToast(@NonNull String message) {
        if (!isAdded()) {
            return;
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
