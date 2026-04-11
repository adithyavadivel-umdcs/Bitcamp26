
package com.example.bitcamp26.feature.match;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.bitcamp26.R;
import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.model.PowerupType;
import com.example.bitcamp26.core.util.TimeUitls;
import com.example.bitcamp26.data.location.LocationRepository;
import com.example.bitcamp26.data.location.PlayerLocation;
import com.example.bitcamp26.feature.match.components.PlayerMarkersRenderer;
import com.example.bitcamp26.feature.match.components.ShrinkBannerView;
import com.example.bitcamp26.feature.match.map.MapManager;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

/**
 * Fragment for the active match screen.
 *
 * Location + map responsibilities (this file):
 *  - Requests ACCESS_FINE_LOCATION at runtime
 *  - Starts/stops GPS via LocationRepository in onResume/onPause
 *  - Auto-populates lat/lng inputs from GPS so the player never has to type them
 *  - Drives MapManager (SupportMapFragment, self marker, player markers, hotspot circles)
 *
 * Game logic (MatchViewModel, untouched):
 *  - Score, hotspot claiming, catch codes, powerups, timer
 */
public class MatchFragment extends Fragment {

    private MatchViewModel viewModel;
    private GameState initialGameState;
    private String initialCurrentPlayerId;
    private CountDownTimer matchTimer;

    // ---- Location + Map ----
    private final MapManager mapManager = new MapManager();
    private LocationRepository locationRepository;
    @Nullable
    private PlayerLocation currentLocation;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

    // ---- Views ----
    private ShrinkBannerView shrinkBannerView;
    private TextView scoreTextView;
    private TextView gameFinishedTextView;
    private TextView playerSummaryTextView;
    private TextView statusTextView;
    private ProgressBar progressBar;

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

    private final PlayerMarkersRenderer playerMarkersRenderer = new PlayerMarkersRenderer();

    public MatchFragment() {
        // Required empty public constructor
    }

    /**
     * Injects initial state before fragment creation.
     */
    public void setInitialData(@Nullable GameState gameState, @Nullable String currentPlayerId) {
        this.initialGameState = gameState;
        this.initialCurrentPlayerId = currentPlayerId;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register the permission launcher before onStart
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fine = result.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false);
                    if (Boolean.TRUE.equals(fine)) {
                        startLocationUpdates();
                    } else {
                        showToast("Location permission denied. GPS tracking disabled.");
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(MatchViewModel.class);
        locationRepository = new LocationRepository(requireContext());
        return inflater.inflate(R.layout.fragment_match, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        shrinkBannerView            = view.findViewById(R.id.shrinkBannerView);
        scoreTextView               = view.findViewById(R.id.textMatchScore);
        gameFinishedTextView        = view.findViewById(R.id.textMatchFinished);
        playerSummaryTextView       = view.findViewById(R.id.textCurrentPlayerSummary);
        currentPlayerIdInput        = view.findViewById(R.id.editCurrentPlayerId);
        latitudeInput               = view.findViewById(R.id.editLatitude);
        longitudeInput              = view.findViewById(R.id.editLongitude);
        catchTargetPlayerIdInput    = view.findViewById(R.id.editCatchTargetPlayerId);
        catchCodeInput              = view.findViewById(R.id.editCatchCode);
        submitLocationButton        = view.findViewById(R.id.buttonSubmitLocation);
        claimHotspotButton          = view.findViewById(R.id.buttonClaimHotspot);
        submitCatchCodeButton       = view.findViewById(R.id.buttonSubmitCatchCode);
        useHiderPowerupButton       = view.findViewById(R.id.buttonUseHiderPowerup);
        useSeekerPowerupButton      = view.findViewById(R.id.buttonUseSeekerPowerup);
        progressBar                 = view.findViewById(R.id.progressMatch);
        statusTextView              = view.findViewById(R.id.textMatchStatus);

        // Hide testing/manual inputs as requested by user
        currentPlayerIdInput.setVisibility(View.GONE);
        view.findViewById(R.id.textCurrentPlayerIdLabel).setVisibility(View.GONE);
        view.findViewById(R.id.textManualLocationLabel).setVisibility(View.GONE);
        
        latitudeInput.setVisibility(View.GONE);
        longitudeInput.setVisibility(View.GONE);
        submitLocationButton.setVisibility(View.GONE);
        
        // Attach Google Map into the FrameLayout container
        setupMap();

        bindListeners();
        bindObservers();

        // Use injected initial data if available
        if (initialGameState != null) {
            viewModel.setInitialState(initialGameState, initialCurrentPlayerId);
            startMatchTimer();
        } else if (viewModel.getGameStateValue() == null) {
            GameState placeholderState = new GameState();
            placeholderState.setStarted(true);
            placeholderState.setFinished(false);
            placeholderState.setScore(0);
            placeholderState.setStartedAt(TimeUitls.nowMillis());
            placeholderState.setEndsAt(TimeUitls.minutesFromNow(5));
            viewModel.setInitialState(placeholderState, null);
            startMatchTimer();
        }

        renderCurrentState();
    }

    private void startMatchTimer() {
        if (matchTimer != null) {
            matchTimer.cancel();
        }

        GameState state = viewModel.getGameStateValue();
        if (state == null || state.isFinished()) return;

        long now = TimeUitls.nowMillis();
        long remaining = state.getEndsAt() - now;

        if (remaining > 0) {
            matchTimer = new CountDownTimer(remaining, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    // Update the banner with remaining time
                    renderGameState(viewModel.getGameStateValue());
                }

                @Override
                public void onFinish() {
                    GameState s = viewModel.getGameStateValue();
                    if (s != null) {
                        s.setFinished(true);
                        viewModel.updateGameState(s);
                    }
                }
            }.start();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAndRequestLocationPermission();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (locationRepository != null) {
            locationRepository.stopLocationUpdates();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (matchTimer != null) {
            matchTimer.cancel();
        }
    }

    // -------------------------------------------------------------------------
    // Map setup
    // -------------------------------------------------------------------------

    private void setupMap() {
        // Reuse an existing child fragment if the system already recreated it
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.mapContainer);

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager()
                    .beginTransaction()
                    .add(R.id.mapContainer, mapFragment)
                    .commit();
        }

        mapFragment.getMapAsync(mapManager);
    }

    // -------------------------------------------------------------------------
    // Location permission + updates
    // -------------------------------------------------------------------------

    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startLocationUpdates() {
        // Use initial player ID if provided (from Ready Check), otherwise fall back to Firebase or local placeholder
        String userId = initialCurrentPlayerId != null ? initialCurrentPlayerId : "local_player";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (userId.equals("local_player") && user != null) {
            userId = user.getUid();
        }

        locationRepository.startLocationUpdates(userId, new LocationRepository.LocationUpdateCallback() {
            @Override
            public void onLocationUpdate(@NonNull PlayerLocation location) {
                currentLocation = location;

                // Auto-fill hidden inputs so logic still works internally
                latitudeInput.setText(String.valueOf(location.getLatitude()));
                longitudeInput.setText(String.valueOf(location.getLongitude()));

                // Show player name on map if we can find it
                String displayName = null;
                Player p = viewModel.getCurrentPlayerValue();
                if (p != null) {
                    displayName = p.getDisplayName();
                }

                // Move the self marker on the real Google Map with the label
                mapManager.updateSelfLocation(location, displayName);
                
                // Also trigger an internal submission so other players see us (for the teammate's backend work)
                viewModel.submitLocation(location.getUserId(), location.getLatitude(), location.getLongitude());
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                showToast("GPS error: " + errorMessage);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Listeners and observers (game logic — unchanged)
    // -------------------------------------------------------------------------

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
            if (Boolean.TRUE.equals(inside)) {
                shrinkBannerView.showSuccessState("You are inside an active hotspot.");
            }
        });

        viewModel.getClaimableHotspot().observe(getViewLifecycleOwner(), hotspot -> {
            claimHotspotButton.setEnabled(
                    hotspot != null && progressBar.getVisibility() != View.VISIBLE);
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

    // -------------------------------------------------------------------------
    // Game actions (unchanged — just read from the auto-populated inputs)
    // -------------------------------------------------------------------------

    private void submitLocation() {
        String playerId = getTrimmedText(currentPlayerIdInput);
        Double latitude  = parseDouble(latitudeInput);
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
        Double latitude  = parseDouble(latitudeInput);
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
        String submittingId = getTrimmedText(currentPlayerIdInput);
        String targetId     = getTrimmedText(catchTargetPlayerIdInput);
        String code         = getTrimmedText(catchCodeInput);

        if (submittingId.isEmpty() || targetId.isEmpty() || code.isEmpty()) {
            showToast("Enter submitting player ID, target player ID, and catch code.");
            return;
        }
        viewModel.submitCatchCode(submittingId, targetId, code);
    }

    private void usePowerup(@NonNull PowerupType powerupType) {
        String playerId = getTrimmedText(currentPlayerIdInput);
        if (playerId.isEmpty()) {
            showToast("Enter a current player ID first.");
            return;
        }
        viewModel.usePowerup(playerId, powerupType);
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

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
            mapManager.updateHotspots(null);
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
            shrinkBannerView.bind(
                    "Match Active",
                    "Complete objectives before time runs out.",
                    TimeUitls.formatMinutesSeconds(remainingMillis),
                    calculateProgressPercent(state),
                    ShrinkBannerView.BannerState.NORMAL
            );
        }

        // Draw hotspot circles on the real map
        mapManager.updateHotspots(state.getHotspots());
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
                .append(player.getLatitude()).append(", ").append(player.getLongitude());

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
        if (state == null) return;

        List<PlayerMarkersRenderer.RenderablePlayerMarker> markers = playerMarkersRenderer.buildMarkers(
                state.getPlayers(),
                currentPlayer != null ? currentPlayer.getId() : null,
                currentPlayer != null ? currentPlayer.getRole() : null,
                shouldRevealAllHiders(currentPlayer),
                true
        );

        // Push markers to the real map (self marker is handled by GPS, others are shown here)
        mapManager.updatePlayerMarkers(markers);
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
        long endsAt    = state.getEndsAt();
        if (startedAt <= 0 || endsAt <= startedAt) return 0;

        long total          = endsAt - startedAt;
        long elapsed        = Math.max(0L, TimeUitls.nowMillis() - startedAt);
        long clampedElapsed = Math.min(elapsed, total);
        return (int) ((clampedElapsed * 100L) / total);
    }

    private void setActionButtonsEnabled(boolean enabled) {
        submitLocationButton.setEnabled(enabled);
        submitCatchCodeButton.setEnabled(enabled);
        useHiderPowerupButton.setEnabled(enabled);
        useSeekerPowerupButton.setEnabled(enabled);

        boolean hotspotAvailable = viewModel.getClaimableHotspot().getValue() != null;
        claimHotspotButton.setEnabled(enabled && hotspotAvailable);
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    @NonNull
    private String getTrimmedText(@Nullable EditText editText) {
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString().trim();
    }

    @Nullable
    private Double parseDouble(@Nullable EditText editText) {
        try {
            String text = getTrimmedText(editText);
            return text.isEmpty() ? null : Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showToast(@NonNull String message) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
