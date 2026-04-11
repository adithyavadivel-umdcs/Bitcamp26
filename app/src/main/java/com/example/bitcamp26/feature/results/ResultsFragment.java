
package com.example.bitcamp26.feature.results;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.util.TimeUitls;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that shows the end-of-match results.
 *
 * This implementation is intentionally programmatic and self-contained so it can
 * be used immediately without requiring an XML layout first.
 *
 * What this screen shows:
 * - match result header (win/loss/completed)
 * - final score
 * - match timing summary
 * - player summary list
 * - simple winner/role interpretation
 * - buttons for replay / return flow hooks
 *
 * This fragment currently uses local placeholder data when no GameState has been supplied.
 * Later, it can be connected to navigation arguments, Firebase-backed match history,
 * or a ResultsViewModel.
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
        return createContentView();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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

    private View createContentView() {
        int padding = dpToPx(16);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        scrollView.addView(root);

        titleTextView = new TextView(requireContext());
        titleTextView.setTextSize(26f);
        titleTextView.setGravity(Gravity.START);
        titleTextView.setText("Match Results");
        root.addView(titleTextView, matchWrapParams(0, 8));

        subtitleTextView = buildBodyTextView("Review the final outcome of the match.");
        root.addView(subtitleTextView, matchWrapParams(0, 12));

        scoreTextView = buildBodyTextView("Final Score: 0");
        root.addView(scoreTextView, matchWrapParams(0, 8));

        timingTextView = buildBodyTextView("Duration: --");
        root.addView(timingTextView, matchWrapParams(0, 8));

        winnerTextView = buildBodyTextView("Winner: TBD");
        root.addView(winnerTextView, matchWrapParams(0, 16));

        playersHeaderTextView = new TextView(requireContext());
        playersHeaderTextView.setText("Players");
        playersHeaderTextView.setTextSize(18f);
        root.addView(playersHeaderTextView, matchWrapParams(0, 8));

        playersSummaryTextView = buildBodyTextView("No player summary available.");
        root.addView(playersSummaryTextView, matchWrapParams(0, 16));

        playAgainButton = buildButton("Play Again");
        root.addView(playAgainButton, matchWrapParams(0, 8));

        returnToLobbyButton = buildButton("Return to Lobby");
        root.addView(returnToLobbyButton, matchWrapParams(0, 12));

        statusTextView = buildBodyTextView("Status: Results ready.");
        root.addView(statusTextView, matchWrapParams(0, 0));

        bindListeners();
        return scrollView;
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

    @NonNull
    private TextView buildBodyTextView(@NonNull String text) {
        TextView textView = new TextView(requireContext());
        textView.setText(text);
        textView.setTextSize(15f);
        textView.setGravity(Gravity.START);
        return textView;
    }

    @NonNull
    private Button buildButton(@NonNull String text) {
        Button button = new Button(requireContext());
        button.setText(text);
        return button;
    }

    @NonNull
    private LinearLayout.LayoutParams matchWrapParams(int topDp, int bottomDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(topDp);
        params.bottomMargin = dpToPx(bottomDp);
        return params;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
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
