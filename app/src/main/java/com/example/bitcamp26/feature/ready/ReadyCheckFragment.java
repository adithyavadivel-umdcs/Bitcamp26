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
import androidx.lifecycle.ViewModelProvider;

import com.example.bitcamp26.R;
import com.example.bitcamp26.core.model.Lobby;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.util.TimeUitls;
import com.example.bitcamp26.navigation.AppNavigator;
import com.example.bitcamp26.ui.MainActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Ready-check screen backed by realtime lobby observation.
 */
public class ReadyCheckFragment extends Fragment {

    private ReadyCheckViewModel viewModel;

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
    private boolean navigatedToMatch;

    public ReadyCheckFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(ReadyCheckViewModel.class);
        readyCheckOpenedAt = TimeUitls.nowMillis();
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
        bindObservers();

        if (currentLobby != null) {
            viewModel.setLobby(currentLobby);
            if (currentLobby.getCode() != null) {
                viewModel.observeLobby(currentLobby.getCode());
            }
        }
        if (currentPlayerId != null) {
            viewModel.setCurrentPlayerId(currentPlayerId);
        }

        refreshUi();
    }

    public void setLobby(@Nullable Lobby lobby) {
        this.currentLobby = lobby;
        if (viewModel != null) {
            viewModel.setLobby(lobby);
            if (lobby != null && lobby.getCode() != null) {
                viewModel.observeLobby(lobby.getCode());
            }
        }
        refreshUi();
    }

    public void setCurrentPlayerId(@Nullable String currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
        if (viewModel != null) {
            viewModel.setCurrentPlayerId(currentPlayerId);
        }
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
        toggleReadyButton.setOnClickListener(v -> viewModel.toggleCurrentPlayerReady());
        markAllReadyButton.setOnClickListener(v -> viewModel.markEveryoneReady());
        startMatchButton.setOnClickListener(v -> viewModel.startMatch());
    }

    private void bindObservers() {
        viewModel.getCurrentLobby().observe(getViewLifecycleOwner(), lobby -> {
            currentLobby = lobby;
            syncCurrentPlayerReadyFromLobby();
            refreshUi();
            if (!navigatedToMatch && lobby != null && lobby.isStarted()) {
                navigatedToMatch = true;
                navigateToMatch();
            }
        });

        viewModel.getCurrentPlayerId().observe(getViewLifecycleOwner(), playerId -> {
            currentPlayerId = playerId;
            syncCurrentPlayerReadyFromLobby();
            refreshUi();
        });

        viewModel.getCurrentPlayerReady().observe(getViewLifecycleOwner(), ready -> {
            currentPlayerReady = Boolean.TRUE.equals(ready);
            refreshUi();
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            refreshUi();
        });

        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message == null || message.trim().isEmpty()) {
                return;
            }
            showStatus(message);
            showToast(message);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message == null || message.trim().isEmpty()) {
                return;
            }
            showStatus(message);
            showToast(message);
        });
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
                    .append(player.isReady() ? "Yes" : "No");

            if (player.isCaught()) {
                builder.append("\n   Match State: Eliminated");
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
        boolean loading = progressBar.getVisibility() == View.VISIBLE;

        toggleReadyButton.setEnabled(hasCurrentPlayer && !started && !loading);
        toggleReadyButton.setText(currentPlayerReady ? "Mark Not Ready" : "Mark Ready");

        markAllReadyButton.setEnabled(false);
        startMatchButton.setEnabled(allReady && !started && !loading && isCurrentPlayerHost());
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
            if (player != null && currentPlayerId.equals(player.getId())) {
                return player;
            }
        }
        return null;
    }

    private boolean isCurrentPlayerHost() {
        return currentLobby != null
                && currentLobby.getHostId() != null
                && currentLobby.getHostId().equals(currentPlayerId);
    }

    private int countReadyPlayers() {
        int count = 0;
        for (Player player : safePlayers()) {
            if (player != null && player.isReady()) {
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
            if (player == null || !player.isReady()) {
                return false;
            }
        }
        return true;
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
        currentPlayerReady = player != null && player.isReady();
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

    private void navigateToMatch() {
        if (!isAdded()) {
            return;
        }
        if (!(requireActivity() instanceof MainActivity)) {
            return;
        }
        AppNavigator navigator = ((MainActivity) requireActivity()).getAppNavigator();
        if (navigator == null) {
            return;
        }
        navigator.showMatch(null, currentPlayerId, true);
    }
}
