
package com.example.bitcamp26.feature.match;

import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

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
 * This implementation is intentionally self-contained and programmatic so the
 * screen can run before a dedicated XML layout and full navigation graph are added.
 * It connects to MatchViewModel and exposes a simple but functional match UI:
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
 *
 * Notes:
 * - This screen does not yet connect to device GPS automatically. It provides
 *   manual latitude/longitude inputs for MVP testing.
 * - It assumes another part of the app will provide/set the initial GameState.
 * - It uses MatchViewModel as the source of truth for interaction results.
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
        return createContentView();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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

        shrinkBannerView = new ShrinkBannerView(requireContext());
        shrinkBannerView.bind(
                "Match Active",
                "Track players, claim hotspots, and use powerups.",
                "05:00",
                0,
                ShrinkBannerView.BannerState.NORMAL
        );
        root.addView(shrinkBannerView, matchWrapParams(0, 12));

        scoreTextView = buildSectionTextView("Score: 0");
        root.addView(scoreTextView, matchWrapParams(0, 6));

        gameFinishedTextView = buildSectionTextView("Game Finished: No");
        root.addView(gameFinishedTextView, matchWrapParams(0, 12));

        mapViewContainer = new MapViewContainer(requireContext());
        mapViewContainer.setPlaceholderText("Map placeholder with player/hotspot overlay");
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(260)
        );
        mapParams.bottomMargin = dpToPx(12);
        root.addView(mapViewContainer, mapParams);

        playerSummaryTextView = buildSectionTextView("Current Player: None selected");
        root.addView(playerSummaryTextView, matchWrapParams(0, 12));

        TextView playerIdLabel = buildLabelTextView("Current Player ID");
        root.addView(playerIdLabel, matchWrapParams(0, 4));

        currentPlayerIdInput = buildEditText("Enter current player ID");
        root.addView(currentPlayerIdInput, matchWrapParams(0, 12));

        TextView locationSectionLabel = buildHeaderTextView("Manual Location Submission");
        root.addView(locationSectionLabel, matchWrapParams(0, 6));

        latitudeInput = buildNumberEditText("Latitude (e.g. 38.9869)");
        root.addView(latitudeInput, matchWrapParams(0, 8));

        longitudeInput = buildNumberEditText("Longitude (e.g. -76.9426)");
        root.addView(longitudeInput, matchWrapParams(0, 8));

        submitLocationButton = buildButton("Submit Location");
        root.addView(submitLocationButton, matchWrapParams(0, 8));

        claimHotspotButton = buildButton("Claim Nearby Hotspot");
        root.addView(claimHotspotButton, matchWrapParams(0, 16));

        TextView catchSectionLabel = buildHeaderTextView("Catch Code Submission");
        root.addView(catchSectionLabel, matchWrapParams(0, 6));

        catchTargetPlayerIdInput = buildEditText("Target player ID");
        root.addView(catchTargetPlayerIdInput, matchWrapParams(0, 8));

        catchCodeInput = buildEditText("Target catch code");
        catchCodeInput.setInputType(InputType.TYPE_CLASS_TEXT);
        root.addView(catchCodeInput, matchWrapParams(0, 8));

        submitCatchCodeButton = buildButton("Submit Catch Code");
        root.addView(submitCatchCodeButton, matchWrapParams(0, 16));

        TextView powerupSectionLabel = buildHeaderTextView("Powerups");
        root.addView(powerupSectionLabel, matchWrapParams(0, 6));

        useHiderPowerupButton = buildButton("Use Hider Invisibility");
        root.addView(useHiderPowerupButton, matchWrapParams(0, 8));

        useSeekerPowerupButton = buildButton("Use Seeker Reveal All");
        root.addView(useSeekerPowerupButton, matchWrapParams(0, 16));

        progressBar = new ProgressBar(requireContext());
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar, wrapWrapParams(0, 12));

        statusTextView = buildSectionTextView("Status: Ready");
        root.addView(statusTextView, matchWrapParams(0, 0));

        bindListeners();
        return scrollView;
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
    private TextView buildHeaderTextView(@NonNull String text) {
        TextView textView = new TextView(requireContext());
        textView.setText(text);
        textView.setTextSize(17f);
        textView.setGravity(Gravity.START);
        return textView;
    }

    @NonNull
    private TextView buildLabelTextView(@NonNull String text) {
        TextView textView = new TextView(requireContext());
        textView.setText(text);
        textView.setTextSize(14f);
        textView.setGravity(Gravity.START);
        return textView;
    }

    @NonNull
    private TextView buildSectionTextView(@NonNull String text) {
        TextView textView = new TextView(requireContext());
        textView.setText(text);
        textView.setTextSize(15f);
        textView.setGravity(Gravity.START);
        return textView;
    }

    @NonNull
    private EditText buildEditText(@NonNull String hint) {
        EditText editText = new EditText(requireContext());
        editText.setHint(hint);
        editText.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return editText;
    }

    @NonNull
    private EditText buildNumberEditText(@NonNull String hint) {
        EditText editText = buildEditText(hint);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);
        return editText;
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

    @NonNull
    private LinearLayout.LayoutParams wrapWrapParams(int topDp, int bottomDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(topDp);
        params.bottomMargin = dpToPx(bottomDp);
        return params;
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

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private void showToast(@NonNull String message) {
        if (!isAdded()) {
            return;
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
