
package com.example.bitcamp26.feature.match;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.bitcamp26.R;
import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.model.PowerupType;
import com.example.bitcamp26.core.util.TimeUitls;
import com.example.bitcamp26.feature.match.components.MapViewContainer;
import com.example.bitcamp26.feature.match.components.PlayerMarkersRenderer;
import com.example.bitcamp26.feature.match.components.ShrinkBannerView;

import java.util.List;

/**
 * Fragment that represents the active match screen.
 *
 * UI is defined in fragment_match.xml. This fragment connects to MatchViewModel
 * and exposes a functional match UI:
 *
 * - top shrink/status banner
 * - score and game status text
 * - placeholder map area with hotspot overlay support
 * - player details summary
 * - location submission controls
 * - claim hotspot action
 * - catch-code submission controls
 * - powerup usage controls
 * - realtime status/error output
 */
public class MatchFragment extends Fragment {

    private MatchViewModel viewModel;

    private ShrinkBannerView shrinkBannerView;
    private TextView scoreTextView;
    private TextView gameFinishedTextView;
    private TextView playerSummaryTextView;
    private TextView statusTextView;
    private ProgressBar progressBar;

    private MapViewContainer mapViewContainer;
    private final PlayerMarkersRenderer playerMarkersRenderer = new PlayerMarkersRenderer();

    private EditText currentPlayerIdInput;
    private EditText latitudeInput;
    private EditText longitudeInput;
    private EditText catchTargetPlayerIdInput;
    private EditText catchCodeInput;

    private Button submitLocationButton;
    private Button claimHotspotButton;
    private Button submitCatchCodeButton;
    private Button useHiderPowerupButton;
    private Button useSeekerPowerupButton;

    public MatchFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(MatchViewModel.class);
        return inflater.inflate(R.layout.fragment_match, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shrinkBannerView = view.findViewById(R.id.shrinkBannerView);
        scoreTextView = view.findViewById(R.id.textMatchScore);
        gameFinishedTextView = view.findViewById(R.id.textMatchFinished);
        mapViewContainer = view.findViewById(R.id.mapViewContainer);
        playerSummaryTextView = view.findViewById(R.id.textCurrentPlayerSummary);
        currentPlayerIdInput = view.findViewById(R.id.editCurrentPlayerId);
        latitudeInput = view.findViewById(R.id.editLatitude);
        longitudeInput = view.findViewById(R.id.editLongitude);
        catchTargetPlayerIdInput = view.findViewById(R.id.editCatchTargetPlayerId);
        catchCodeInput = view.findViewById(R.id.editCatchCode);
        submitLocationButton = view.findViewById(R.id.buttonSubmitLocation);
        claimHotspotButton = view.findViewById(R.id.buttonClaimHotspot);
        submitCatchCodeButton = view.findViewById(R.id.buttonSubmitCatchCode);
        useHiderPowerupButton = view.findViewById(R.id.buttonUseHiderPowerup);
        useSeekerPowerupButton = view.findViewById(R.id.buttonUseSeekerPowerup);
        progressBar = view.findViewById(R.id.progressMatch);
        statusTextView = view.findViewById(R.id.textMatchStatus);

        // Initialize banner and map placeholder to match the state set in createContentView.
        shrinkBannerView.bind(
                "Match Active",
                "Track players, claim hotspots, and use powerups.",
                "05:00",
                0,
                ShrinkBannerView.BannerState.NORMAL
        );
        mapViewContainer.setPlaceholderText("Map placeholder with player/hotspot overlay");

        bindListeners();
        bindObservers();

        if (viewModel.getGameStateValue() == null) {
            GameState placeholderState = new GameState();
            placeholderState.setStarted(true);
            placeholderState.setFinished(false);
            placeholderState.setScore(0);
            placeholderState.setStartedAt(TimeUitls.nowMillis());
            placeholderState.setEndsAt(TimeUitls.minutesFromNow(5));
            viewModel.setInitialState(placeholderState, null);
        }

        renderCurrentState();
    }

    private void bindListeners() {
        submitLocationButton.setOnClickListener(v -> submitLocation());
        claimHotspotButton.setOnClickListener(v -> claimHotspot());
        submitCatchCodeButton.setOnClickListener(v -> submitCatchCode());
        useHiderPowerupButton.setOnClickListener(v -> usePowerup(PowerupType.HIDER_INVISIBILITY));
        useSeekerPowerupButton.setOnClickListener(v -> usePowerup(PowerupType.SEEKER_REVEAL_ALL));
    }

    private void bindObservers() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            setActionButtonsEnabled(!loading);
        });

        viewModel.getScore().observe(getViewLifecycleOwner(), score -> {
            int safeScore = score != null ? score : 0;
            scoreTextView.setText("Score: " + safeScore);
        });

        viewModel.getGameFinished().observe(getViewLifecycleOwner(), finished -> {
            boolean isFinished = Boolean.TRUE.equals(finished);
            gameFinishedTextView.setText("Game Finished: " + (isFinished ? "Yes" : "No"));
            updateBannerForGameState(isFinished);
        });

        viewModel.getCurrentPlayer().observe(getViewLifecycleOwner(), player -> {
            renderPlayerSummary(player);
            renderPlayerMarkers();
        });

        viewModel.getGameState().observe(getViewLifecycleOwner(), state -> {
            renderGameState(state);
            renderPlayerMarkers();
        });

        viewModel.getInsideHotspot().observe(getViewLifecycleOwner(), inside -> {
            boolean isInside = Boolean.TRUE.equals(inside);
            if (isInside) {
                shrinkBannerView.showSuccessState("You are inside an active hotspot.");
            }
        });

        viewModel.getClaimableHotspot().observe(getViewLifecycleOwner(), hotspot -> {
            claimHotspotButton.setEnabled(hotspot != null && progressBar.getVisibility() != View.VISIBLE);
        });

        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                statusTextView.setText("Status: " + message);
                showToast(message);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.trim().isEmpty()) {
                statusTextView.setText("Error: " + error);
                showToast(error);
            }
        });
    }

    private void submitLocation() {
        String playerId = getTrimmedText(currentPlayerIdInput);
        Double latitude = parseDouble(latitudeInput);
        Double longitude = parseDouble(longitudeInput);

        if (playerId.isEmpty()) {
            showToast("Enter a current player ID first.");
            return;
        }

        if (latitude == null || longitude == null) {
            showToast("Enter valid latitude and longitude values.");
            return;
        }

        viewModel.submitLocation(playerId, latitude, longitude);
    }

    private void claimHotspot() {
        String playerId = getTrimmedText(currentPlayerIdInput);
        Double latitude = parseDouble(latitudeInput);
        Double longitude = parseDouble(longitudeInput);

        if (playerId.isEmpty()) {
            showToast("Enter a current player ID first.");
            return;
        }

        if (latitude == null || longitude == null) {
            showToast("Enter valid latitude and longitude values first.");
            return;
        }

        viewModel.claimHotspot(playerId, latitude, longitude);
    }

    private void submitCatchCode() {
        String submittingPlayerId = getTrimmedText(currentPlayerIdInput);
        String targetPlayerId = getTrimmedText(catchTargetPlayerIdInput);
        String submittedCode = getTrimmedText(catchCodeInput);

        if (submittingPlayerId.isEmpty() || targetPlayerId.isEmpty() || submittedCode.isEmpty()) {
            showToast("Enter submitting player ID, target player ID, and catch code.");
            return;
        }

        viewModel.submitCatchCode(submittingPlayerId, targetPlayerId, submittedCode);
    }

    private void usePowerup(@NonNull PowerupType powerupType) {
        String playerId = getTrimmedText(currentPlayerIdInput);
        if (playerId.isEmpty()) {
            showToast("Enter a current player ID first.");
            return;
        }

        viewModel.usePowerup(playerId, powerupType);
    }

    private void renderCurrentState() {
        renderGameState(viewModel.getGameStateValue());
        renderPlayerSummary(viewModel.getCurrentPlayerValue());
        renderPlayerMarkers();
    }

    private void renderGameState(@Nullable GameState state) {
        if (state == null) {
            scoreTextView.setText("Score: 0");
            gameFinishedTextView.setText("Game Finished: No");
            shrinkBannerView.showInactiveState();
            mapViewContainer.clearHotspots();
            return;
        }

        scoreTextView.setText("Score: " + state.getScore());
        gameFinishedTextView.setText("Game Finished: " + (state.isFinished() ? "Yes" : "No"));

        if (state.isFinished()) {
            shrinkBannerView.showDangerState("00:00");
            shrinkBannerView.setTitle("Match Over");
            shrinkBannerView.setMessage("The game has ended.");
            shrinkBannerView.setProgressPercent(100);
        } else {
            long remainingMillis = Math.max(0L, state.getEndsAt() - TimeUitls.nowMillis());
            String remainingText = TimeUitls.formatMinutesSeconds(remainingMillis);
            shrinkBannerView.bind(
                    "Match Active",
                    "Complete objectives before time runs out.",
                    remainingText,
                    calculateProgressPercent(state),
                    ShrinkBannerView.BannerState.NORMAL
            );
        }

        mapViewContainer.setHotspots(state.getHotspots());
    }

    private void renderPlayerSummary(@Nullable Player player) {
        if (player == null) {
            playerSummaryTextView.setText("Current Player: None selected");
            return;
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Current Player: ")
                .append(player.getDisplayName() != null ? player.getDisplayName() : "Player")
                .append("\nID: ").append(player.getId() != null ? player.getId() : "N/A")
                .append("\nRole: ").append(player.getRole() != null ? player.getRole().name() : "UNKNOWN")
                .append("\nCaught: ").append(player.isCaught() ? "Yes" : "No")
                .append("\nLocation: ")
                .append(player.getLatitude())
                .append(", ")
                .append(player.getLongitude());

        if (player.getHeldPowerup() != null) {
            summary.append("\nHeld Powerup: ").append(player.getHeldPowerup().name());
        }

        if (player.getActivePowerup() != null) {
            summary.append("\nActive Powerup: ").append(player.getActivePowerup().name());
        }

        playerSummaryTextView.setText(summary.toString());
    }

    private void renderPlayerMarkers() {
        GameState state = viewModel.getGameStateValue();
        Player currentPlayer = viewModel.getCurrentPlayerValue();

        if (state == null) {
            return;
        }

        List<PlayerMarkersRenderer.RenderablePlayerMarker> markers = playerMarkersRenderer.buildMarkers(
                state.getPlayers(),
                currentPlayer != null ? currentPlayer.getId() : null,
                currentPlayer != null ? currentPlayer.getRole() : null,
                shouldRevealAllHiders(currentPlayer),
                true
        );

        StringBuilder placeholder = new StringBuilder("Map placeholder with players: ");
        if (markers.isEmpty()) {
            placeholder.append("none visible");
        } else {
            for (int i = 0; i < markers.size(); i++) {
                placeholder.append(markers.get(i).getLabel());
                if (i < markers.size() - 1) {
                    placeholder.append(", ");
                }
            }
        }
        mapViewContainer.setPlaceholderText(placeholder.toString());
    }

    private boolean shouldRevealAllHiders(@Nullable Player player) {
        return player != null
                && player.getRole() == PlayerRole.SEEKER
                && player.getActivePowerup() == PowerupType.SEEKER_REVEAL_ALL;
    }

    private void updateBannerForGameState(boolean isFinished) {
        if (isFinished) {
            shrinkBannerView.setBannerState(ShrinkBannerView.BannerState.DANGER);
            shrinkBannerView.setTitle("Match Over");
            shrinkBannerView.setMessage("No more actions can be performed.");
            shrinkBannerView.setTimerText("00:00");
        }
    }

    private int calculateProgressPercent(@NonNull GameState state) {
        long startedAt = state.getStartedAt();
        long endsAt = state.getEndsAt();

        if (startedAt <= 0 || endsAt <= startedAt) {
            return 0;
        }

        long total = endsAt - startedAt;
        long elapsed = Math.max(0L, TimeUitls.nowMillis() - startedAt);
        long clampedElapsed = Math.min(elapsed, total);
        return (int) ((clampedElapsed * 100L) / total);
    }

    private void setActionButtonsEnabled(boolean enabled) {
        submitLocationButton.setEnabled(enabled);
        submitCatchCodeButton.setEnabled(enabled);
        useHiderPowerupButton.setEnabled(enabled);
        useSeekerPowerupButton.setEnabled(enabled);

        Boolean hotspotAvailable = viewModel.getClaimableHotspot().getValue() != null;
        claimHotspotButton.setEnabled(enabled && hotspotAvailable);
    }

    @NonNull
    private String getTrimmedText(@Nullable EditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    @Nullable
    private Double parseDouble(@Nullable EditText editText) {
        try {
            String text = getTrimmedText(editText);
            if (text.isEmpty()) {
                return null;
            }
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showToast(@NonNull String message) {
        if (!isAdded()) {
            return;
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
