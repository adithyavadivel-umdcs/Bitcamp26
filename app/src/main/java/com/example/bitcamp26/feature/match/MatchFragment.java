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
import com.example.bitcamp26.core.model.HotspotState;
import com.example.bitcamp26.core.model.Lobby;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.model.PowerupType;
import com.example.bitcamp26.core.util.TimeUitls;
import com.example.bitcamp26.data.lobby.LobbyRepository;
import com.example.bitcamp26.data.location.LocationRepository;
import com.example.bitcamp26.data.location.PlayerLocation;
import com.example.bitcamp26.feature.match.components.PlayerMarkersRenderer;
import com.example.bitcamp26.feature.match.components.ShrinkBannerView;
import com.example.bitcamp26.feature.match.map.MapManager;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that represents the active match screen with GPS + map integration.
 */
public class MatchFragment extends Fragment {

    private MatchViewModel viewModel;
    private LobbyRepository lobbyRepository;
    private LocationRepository locationRepository;

    private Lobby initialLobby;
    private Lobby currentLobby;
    private GameState initialGameState;
    private String initialCurrentPlayerId;

    private ValueEventListener lobbyListener;
    private String observedLobbyCode;
    private CountDownTimer matchTimer;

    private final MapManager mapManager = new MapManager();
    private final PlayerMarkersRenderer playerMarkersRenderer = new PlayerMarkersRenderer();
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

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

    public MatchFragment() {
        // Required empty public constructor
    }

    public void setInitialData(@Nullable Lobby lobby,
                               @Nullable GameState gameState,
                               @Nullable String currentPlayerId) {
        this.initialLobby = lobby;
        this.currentLobby = lobby;
        this.initialGameState = gameState;
        this.initialCurrentPlayerId = currentPlayerId;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false))) {
                        startLocationUpdates();
                    } else {
                        setStatusMessage("Location permission denied. GPS tracking disabled.");
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
        lobbyRepository = new LobbyRepository();
        locationRepository = new LocationRepository(requireContext());
        return inflater.inflate(R.layout.fragment_match, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shrinkBannerView = view.findViewById(R.id.shrinkBannerView);
        scoreTextView = view.findViewById(R.id.textMatchScore);
        gameFinishedTextView = view.findViewById(R.id.textMatchFinished);
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

        currentPlayerIdInput.setText(resolveCurrentPlayerId());
        currentPlayerIdInput.setVisibility(View.GONE);
        latitudeInput.setVisibility(View.GONE);
        longitudeInput.setVisibility(View.GONE);
        submitLocationButton.setVisibility(View.GONE);

        setupMap();
        bindListeners();
        bindObservers();
        seedInitialState();
        startObservingLobby();
        renderCurrentState();
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
        stopObservingLobby();
        if (matchTimer != null) {
            matchTimer.cancel();
            matchTimer = null;
        }
        mapManager.clearAll();
    }

    private void seedInitialState() {
        GameState state = initialGameState;
        if (state == null && currentLobby != null) {
            state = createGameStateFromLobby(currentLobby);
        }
        if (state == null) {
            state = createPlaceholderGameState();
        }

        viewModel.setInitialState(state, resolveCurrentPlayerId());
        startMatchTimerIfNeeded(state);
    }

    private void setupMap() {
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
            scoreTextView.setText("Score: " + (score != null ? score : 0));
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
            claimHotspotButton.setEnabled(hotspot != null && progressBar.getVisibility() != View.VISIBLE);
        });

        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                setStatusMessage("Status: " + message);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.trim().isEmpty()) {
                setStatusMessage("Error: " + error);
                showToast(error);
            }
        });
    }

    private void startObservingLobby() {
        if (currentLobby == null || currentLobby.getCode() == null || lobbyListener != null) {
            return;
        }

        observedLobbyCode = currentLobby.getCode();
        lobbyListener = lobbyRepository.observeLobby(observedLobbyCode, new LobbyRepository.LobbyCallback() {
            @Override
            public void onSuccess(@NonNull Lobby lobby) {
                currentLobby = lobby;
                GameState updatedState = mergeLobbyIntoGameState(lobby, viewModel.getGameStateValue());
                viewModel.updateGameState(updatedState);
                startMatchTimerIfNeeded(updatedState);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                setStatusMessage("Lobby sync error: " + errorMessage);
            }
        });
    }

    private void stopObservingLobby() {
        if (observedLobbyCode != null && lobbyListener != null) {
            lobbyRepository.removeLobbyObserver(observedLobbyCode, lobbyListener);
        }
        observedLobbyCode = null;
        lobbyListener = null;
    }

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
        String playerId = resolveCurrentPlayerId();
        if (playerId.isEmpty()) {
            setStatusMessage("Missing current player ID for GPS tracking.");
            return;
        }

        locationRepository.startLocationUpdates(playerId, new LocationRepository.LocationUpdateCallback() {
            @Override
            public void onLocationUpdate(@NonNull PlayerLocation location) {
                if (getView() == null || latitudeInput == null || longitudeInput == null) {
                    return;
                }

                latitudeInput.setText(String.valueOf(location.getLatitude()));
                longitudeInput.setText(String.valueOf(location.getLongitude()));

                Player player = viewModel.getCurrentPlayerValue();
                mapManager.updateSelfLocation(location, player != null ? player.getDisplayName() : null);
                viewModel.submitLocation(location.getUserId(), location.getLatitude(), location.getLongitude());
                syncLocationToLobby(location);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                setStatusMessage("GPS error: " + errorMessage);
            }
        });
    }

    private void syncLocationToLobby(@NonNull PlayerLocation location) {
        if (currentLobby == null || currentLobby.getCode() == null) {
            return;
        }

        lobbyRepository.updatePlayerLocation(
                currentLobby.getCode(),
                location.getUserId(),
                location.getLatitude(),
                location.getLongitude(),
                location.getTimestamp(),
                new LobbyRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        // Live lobby observer will deliver the updated positions.
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        setStatusMessage("Failed to sync location: " + errorMessage);
                    }
                }
        );
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
        syncLocationToLobby(new PlayerLocation(playerId, latitude, longitude, TimeUitls.nowMillis()));
    }

    private void claimHotspot() {
        String playerId = getTrimmedText(currentPlayerIdInput);
        Double latitude = parseDouble(latitudeInput);
        Double longitude = parseDouble(longitudeInput);

        if (playerId.isEmpty()) {
            showToast("Current player is unavailable.");
            return;
        }
        if (latitude == null || longitude == null) {
            showToast("Waiting for GPS location.");
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
            showToast("Current player is unavailable.");
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
                currentPlayer != null ? currentPlayer.getId() : resolveCurrentPlayerId(),
                currentPlayer != null ? currentPlayer.getRole() : null,
                shouldRevealAllHiders(currentPlayer),
                true
        );
        mapManager.updatePlayerMarkers(markers);
    }

    private boolean shouldRevealAllHiders(@Nullable Player player) {
        return player != null
                && player.getRole() == PlayerRole.SEEKER
                && player.getActivePowerup() == PowerupType.SEEKER_REVEAL_ALL;
    }

    private void updateBannerForGameState(boolean finished) {
        if (finished) {
            shrinkBannerView.showDangerState("00:00");
            shrinkBannerView.setTitle("Match Over");
            shrinkBannerView.setMessage("The game has ended.");
            shrinkBannerView.setProgressPercent(100);
        }
    }

    private void setActionButtonsEnabled(boolean enabled) {
        submitLocationButton.setEnabled(enabled);
        submitCatchCodeButton.setEnabled(enabled);
        useHiderPowerupButton.setEnabled(enabled);
        useSeekerPowerupButton.setEnabled(enabled);

        boolean hotspotAvailable = viewModel.getClaimableHotspot().getValue() != null;
        claimHotspotButton.setEnabled(enabled && hotspotAvailable);
    }

    private void startMatchTimerIfNeeded(@Nullable GameState state) {
        if (state == null || state.isFinished() || state.getEndsAt() <= 0) {
            return;
        }
        if (matchTimer != null) {
            return;
        }

        long remainingMillis = Math.max(0L, state.getEndsAt() - TimeUitls.nowMillis());
        if (remainingMillis <= 0L) {
            return;
        }

        matchTimer = new CountDownTimer(remainingMillis, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                renderGameState(viewModel.getGameStateValue());
            }

            @Override
            public void onFinish() {
                GameState existingState = viewModel.getGameStateValue();
                if (existingState != null) {
                    existingState.setFinished(true);
                    viewModel.updateGameState(existingState);
                }
                matchTimer = null;
            }
        }.start();
    }

    @NonNull
    private GameState createGameStateFromLobby(@NonNull Lobby lobby) {
        GameState state = initialGameState != null ? initialGameState : new GameState();
        state.setStarted(true);
        state.setFinished(false);
        state.setPlayers(lobby.getPlayers());

        if (initialGameState == null) {
            state.setScore(0);
            long now = TimeUitls.nowMillis();
            state.setStartedAt(now);
            long durationMillis = lobby.getMatchDurationSeconds() > 0
                    ? lobby.getMatchDurationSeconds() * 1000L
                    : 300_000L;
            state.setEndsAt(now + durationMillis);
            state.setHotspots(buildDefaultHotspots());
        } else if (state.getHotspots() == null || state.getHotspots().isEmpty()) {
            state.setHotspots(buildDefaultHotspots());
        }

        return state;
    }

    @NonNull
    private GameState mergeLobbyIntoGameState(@NonNull Lobby lobby, @Nullable GameState existingState) {
        GameState state = existingState != null ? existingState : createGameStateFromLobby(lobby);
        state.setStarted(lobby.isStarted());
        state.setPlayers(lobby.getPlayers());
        if (state.getHotspots() == null || state.getHotspots().isEmpty()) {
            state.setHotspots(buildDefaultHotspots());
        }
        if (state.getStartedAt() <= 0L) {
            long now = TimeUitls.nowMillis();
            state.setStartedAt(now);
            long durationMillis = lobby.getMatchDurationSeconds() > 0
                    ? lobby.getMatchDurationSeconds() * 1000L
                    : 300_000L;
            state.setEndsAt(now + durationMillis);
        }
        return state;
    }

    @NonNull
    private GameState createPlaceholderGameState() {
        GameState placeholderState = new GameState();
        placeholderState.setStarted(true);
        placeholderState.setFinished(false);
        placeholderState.setScore(0);
        placeholderState.setStartedAt(TimeUitls.nowMillis());
        placeholderState.setEndsAt(TimeUitls.minutesFromNow(5));
        placeholderState.setHotspots(buildDefaultHotspots());
        placeholderState.setPlayers(new ArrayList<>());
        return placeholderState;
    }

    @NonNull
    private List<HotspotState> buildDefaultHotspots() {
        List<HotspotState> hotspots = new ArrayList<>();
        hotspots.add(new HotspotState("h1", 38.9869, -76.9426, 50, "HIDER_INVISIBILITY"));
        hotspots.add(new HotspotState("h2", 38.9875, -76.9400, 30, "SEEKER_REVEAL_ALL"));
        return hotspots;
    }

    private int calculateProgressPercent(@NonNull GameState state) {
        long startedAt = state.getStartedAt();
        long endsAt = state.getEndsAt();

        if (startedAt <= 0L || endsAt <= startedAt) {
            return 0;
        }

        long total = endsAt - startedAt;
        long elapsed = Math.max(0L, TimeUitls.nowMillis() - startedAt);
        long clampedElapsed = Math.min(elapsed, total);
        return (int) ((clampedElapsed * 100L) / total);
    }

    @NonNull
    private String resolveCurrentPlayerId() {
        if (initialCurrentPlayerId != null && !initialCurrentPlayerId.trim().isEmpty()) {
            return initialCurrentPlayerId.trim();
        }
        return getTrimmedText(currentPlayerIdInput);
    }

    private void setStatusMessage(@NonNull String message) {
        if (getView() == null || statusTextView == null) {
            return;
        }
        statusTextView.setText(message);
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
