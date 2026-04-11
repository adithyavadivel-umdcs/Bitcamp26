
package com.example.bitcamp26.feature.ready;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.HotspotState;
import com.example.bitcamp26.core.model.Lobby;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.util.TimeUitls;
import com.example.bitcamp26.navigation.AppNavigator;
import com.example.bitcamp26.ui.MainActivity;
import com.example.bitcamp26.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that represents a simple ready-check screen before a match starts.
 * sds
 */
public class ReadyCheckFragment extends Fragment {

    private TextView titleTextView;
    private TextView lobbySummaryTextView;
    private TextView countdownTextView;
    private TextView playersHeaderTextView;
    private TextView playersListTextView;
    private TextView statusTextView;

    private Button toggleReadyButton;
    private Button markAllReadyButton;
    private Button startMatchButton;
    private ProgressBar progressBar;

    private Lobby currentLobby;
    private String currentPlayerId;
    private boolean currentPlayerReady;
    private long readyCheckOpenedAt;

    public ReadyCheckFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        readyCheckOpenedAt = TimeUitls.nowMillis();
        seedPlaceholderLobbyIfNeeded();
        return inflater.inflate(R.layout.fragment_ready_check, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        titleTextView = view.findViewById(R.id.textReadyTitle);
        lobbySummaryTextView = view.findViewById(R.id.textReadyLobbySummary);
        countdownTextView = view.findViewById(R.id.textReadyCountdown);
        progressBar = view.findViewById(R.id.progressReadyCheck);
        playersHeaderTextView = view.findViewById(R.id.textPlayersHeader);
        playersListTextView = view.findViewById(R.id.textPlayersList);
        toggleReadyButton = view.findViewById(R.id.buttonToggleReady);
        markAllReadyButton = view.findViewById(R.id.buttonMarkAllReady);
        startMatchButton = view.findViewById(R.id.buttonStartMatch);
        statusTextView = view.findViewById(R.id.textReadyStatus);

        bindListeners();
        refreshUi();
    }

    /**
     * Allows another part of the app to inject the current lobby state.
     */
    public void setLobby(@Nullable Lobby lobby) {
        this.currentLobby = lobby;
        refreshUi();
    }

    /**
     * Allows another part of the app to identify the current player.
     */
    public void setCurrentPlayerId(@Nullable String currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
        syncCurrentPlayerReadyFromLobby();
        refreshUi();
    }

    @Nullable
    public Lobby getLobby() {
        return currentLobby;
    }

    @Nullable
    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    private void bindListeners() {
        toggleReadyButton.setOnClickListener(v -> toggleCurrentPlayerReady());
        markAllReadyButton.setOnClickListener(v -> markEveryoneReadyForDemo());
        startMatchButton.setOnClickListener(v -> startMatchIfPossible());
    }

    private void toggleCurrentPlayerReady() {
        if (currentLobby == null) {
            showStatus("Cannot update readiness because the lobby is missing.");
            return;
        }

        Player currentPlayer = findCurrentPlayer();
        if (currentPlayer == null) {
            showStatus("Current player was not found in the lobby.");
            return;
        }

        currentPlayerReady = !currentPlayerReady;
        applyReadyStateToPlayer(currentPlayer, currentPlayerReady);

        showStatus(currentPlayerReady
                ? "You are marked ready."
                : "You are marked not ready.");
        refreshUi();
    }

    private void markEveryoneReadyForDemo() {
        if (currentLobby == null || currentLobby.getPlayers() == null) {
            showStatus("No lobby players available to update.");
            return;
        }

        for (Player player : currentLobby.getPlayers()) {
            if (player == null) {
                continue;
            }
            applyReadyStateToPlayer(player, true);
        }

        currentPlayerReady = true;
        showStatus("All players marked ready for demo purposes.");
        refreshUi();
    }

    private void startMatchIfPossible() {
        if (currentLobby == null) {
            showStatus("Lobby missing. Cannot start the match.");
            return;
        }

        if (!areAllPlayersReady()) {
            showStatus("Not everyone is ready yet.");
            return;
        }

        currentLobby.setStarted(true);
        showStatus("All players ready. Match can start now.");
        refreshUi();
        showToast("Ready check complete. Starting match...");

        // Navigation fix: Transition to the Match screen
        if (getActivity() instanceof MainActivity) {
            AppNavigator navigator = ((MainActivity) getActivity()).getAppNavigator();
            if (navigator != null) {
                GameState gameState = createGameStateFromLobby(currentLobby);
                navigator.showMatch(gameState, currentPlayerId, true);
            }
        }
    }

    private GameState createGameStateFromLobby(Lobby lobby) {
        GameState state = new GameState();
        state.setStarted(true);
        state.setFinished(false);
        state.setScore(0);
        state.setPlayers(lobby.getPlayers());

        long now = TimeUitls.nowMillis();
        state.setStartedAt(now);

        long durationMillis = lobby.getMatchDurationSeconds() * 1000L;
        if (durationMillis <= 0) {
            durationMillis = 300000L; // Default 5 minutes
        }
        state.setEndsAt(now + durationMillis);

        // Add some sample hotspots near a default location if none exist
        List<HotspotState> hotspots = new ArrayList<>();
        hotspots.add(new HotspotState("h1", 38.9869, -76.9426, 50, "HIDER_INVISIBILITY"));
        hotspots.add(new HotspotState("h2", 38.9875, -76.9400, 30, "SEEKER_REVEAL_ALL"));
        state.setHotspots(hotspots);

        return state;
    }

    private void refreshUi() {
        if (!isAdded()) {
            return;
        }

        updateLobbySummary();
        updateCountdownInfo();
        updatePlayersList();
        updateButtons();
        updateProgress();
    }

    private void updateLobbySummary() {
        if (currentLobby == null) {
            lobbySummaryTextView.setText("Lobby summary unavailable.");
            return;
        }

        int totalPlayers = safePlayers().size();
        int readyPlayers = countReadyPlayers();
        int maxPlayers = currentLobby.getMaxPlayers();
        long durationSeconds = currentLobby.getMatchDurationSeconds();

        StringBuilder summary = new StringBuilder();
        summary.append("Code: ")
                .append(currentLobby.getCode() != null ? currentLobby.getCode() : "N/A")
                .append("\nPlayers: ")
                .append(totalPlayers);

        if (maxPlayers > 0) {
            summary.append(" / ").append(maxPlayers);
        }

        summary.append("\nReady: ")
                .append(readyPlayers)
                .append(" / ")
                .append(totalPlayers)
                .append("\nMatch Duration: ")
                .append(durationSeconds > 0 ? durationSeconds + "s" : "default")
                .append("\nStarted: ")
                .append(currentLobby.isStarted() ? "Yes" : "No");

        lobbySummaryTextView.setText(summary.toString());
    }

    private void updateCountdownInfo() {
        long elapsedMillis = Math.max(0L, TimeUitls.nowMillis() - readyCheckOpenedAt);
        countdownTextView.setText("Ready check open for: " + TimeUitls.formatDurationHuman(elapsedMillis));
    }

    private void updatePlayersList() {
        List<Player> players = safePlayers();
        if (players.isEmpty()) {
            playersListTextView.setText("No players found.");
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player == null) {
                continue;
            }

            String displayName = player.getDisplayName() != null && !player.getDisplayName().trim().isEmpty()
                    ? player.getDisplayName().trim()
                    : "Player";

            builder.append(i + 1)
                    .append(". ")
                    .append(displayName);

            if (player.getId() != null && player.getId().equals(currentPlayerId)) {
                builder.append(" (You)");
            }

            builder.append("\n   Role: ")
                    .append(player.getRole() != null ? player.getRole().name() : "UNKNOWN")
                    .append("\n   Ready: ")
                    .append(isPlayerReady(player) ? "Yes" : "No");

            if (player.isCaught()) {
                builder.append("\n   Match State: Caught");
            }

            if (i < players.size() - 1) {
                builder.append("\n\n");
            }
        }

        playersListTextView.setText(builder.toString());
    }

    private void updateButtons() {
        boolean hasCurrentPlayer = findCurrentPlayer() != null;
        boolean allReady = areAllPlayersReady();
        boolean started = currentLobby != null && currentLobby.isStarted();

        toggleReadyButton.setEnabled(hasCurrentPlayer && !started);
        toggleReadyButton.setText(currentPlayerReady ? "Mark Not Ready" : "Mark Ready");

        markAllReadyButton.setEnabled(!started);
        startMatchButton.setEnabled(allReady && !started);
        startMatchButton.setText(started ? "Match Started" : "Start Match");
    }

    private void updateProgress() {
        List<Player> players = safePlayers();
        if (players.isEmpty()) {
            progressBar.setProgress(0);
            return;
        }

        int readyCount = countReadyPlayers();
        int percent = (int) ((readyCount * 100f) / players.size());
        progressBar.setProgress(Math.max(0, Math.min(100, percent)));
    }

    @Nullable
    private Player findCurrentPlayer() {
        if (currentPlayerId == null || currentPlayerId.trim().isEmpty()) {
            return null;
        }

        for (Player player : safePlayers()) {
            if (player == null || player.getId() == null) {
                continue;
            }
            if (currentPlayerId.equals(player.getId())) {
                return player;
            }
        }
        return null;
    }

    private int countReadyPlayers() {
        int count = 0;
        for (Player player : safePlayers()) {
            if (player != null && isPlayerReady(player)) {
                count++;
            }
        }
        return count;
    }

    private boolean areAllPlayersReady() {
        List<Player> players = safePlayers();
        if (players.isEmpty()) {
            return false;
        }

        for (Player player : players) {
            if (player == null || !isPlayerReady(player)) {
                return false;
            }
        }
        return true;
    }

    /**
     * MVP/local ready-state rule.
     *
     * Since the Player model may not yet have a dedicated ready boolean, this method
     * uses the player's active powerup timestamps/fields only if you later want to swap
     * to a model-backed solution. For now, we derive readiness from catch-code presence
     * plus current-player local toggle only for the active user, and treat everyone else
     * as not ready unless marked through the demo helper.
     *
     * To keep this fragment stable without forcing model edits, we store readiness in the
     * player's displayName suffix: "[READY]". That is not ideal for production, but it is
     * safe for MVP/local testing until the Player model gets a dedicated field.
     */
    private boolean isPlayerReady(@NonNull Player player) {
        String displayName = player.getDisplayName();
        return displayName != null && displayName.contains("[READY]");
    }

    private void applyReadyStateToPlayer(@NonNull Player player, boolean ready) {
        String name = player.getDisplayName();
        if (name == null || name.trim().isEmpty()) {
            name = "Player";
        }

        String cleaned = name.replace("[READY]", "").trim();
        player.setDisplayName(ready ? cleaned + " [READY]" : cleaned);
    }

    @NonNull
    private List<Player> safePlayers() {
        if (currentLobby == null || currentLobby.getPlayers() == null) {
            return new ArrayList<>();
        }
        return currentLobby.getPlayers();
    }

    private void syncCurrentPlayerReadyFromLobby() {
        Player player = findCurrentPlayer();
        currentPlayerReady = player != null && isPlayerReady(player);
    }

    /**
     * Creates placeholder lobby/player data so the screen is immediately testable.
     */
    private void seedPlaceholderLobbyIfNeeded() {
        if (currentLobby != null) {
            syncCurrentPlayerReadyFromLobby();
            return;
        }

        Lobby lobby = new Lobby();
        lobby.setCode("READY1");
        lobby.setStarted(false);
        lobby.setMaxPlayers(4);
        lobby.setMatchDurationSeconds(300);

        List<Player> players = new ArrayList<>();

        Player p1 = new Player();
        p1.setId("player_host");
        p1.setDisplayName("Host Player [READY]");
        p1.setRole(PlayerRole.SEEKER);
        p1.setCaught(false);

        Player p2 = new Player();
        p2.setId("player_me");
        p2.setDisplayName("My Player");
        p2.setRole(PlayerRole.HIDER);
        p2.setCaught(false);

        Player p3 = new Player();
        p3.setId("player_3");
        p3.setDisplayName("Teammate");
        p3.setRole(PlayerRole.HIDER);
        p3.setCaught(false);

        players.add(p1);
        players.add(p2);
        players.add(p3);

        lobby.setPlayers(players);
        lobby.setPlayerCount(players.size());

        currentLobby = lobby;
        currentPlayerId = "player_me";
        syncCurrentPlayerReadyFromLobby();
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
