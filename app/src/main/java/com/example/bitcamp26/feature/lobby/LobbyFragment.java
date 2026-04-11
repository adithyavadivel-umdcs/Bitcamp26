
package com.example.bitcamp26.feature.lobby;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bitcamp26.core.model.Lobby;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.util.CodeUtils;
import com.example.bitcamp26.data.auth.AuthRepository;
import com.example.bitcamp26.data.lobby.LobbyRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple lobby screen for creating and joining lobbies.
 *
 * This fragment builds its UI programmatically so it can run even before
 * a dedicated XML layout and navigation flow are connected.
 */
public class LobbyFragment extends Fragment {

    private LobbyRepository lobbyRepository;
    private AuthRepository authRepository;

    private EditText displayNameInput;
    private EditText lobbyCodeInput;
    private Button createLobbyButton;
    private Button joinLobbyButton;
    private ProgressBar progressBar;
    private TextView statusTextView;

    public LobbyFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        lobbyRepository = new LobbyRepository();
        authRepository = new AuthRepository();
        return createContentView();
    }

    private View createContentView() {
        if (requireContext() == null) {
            return new View(getContext());
        }

        int padding = dpToPx(20);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        TextView title = new TextView(requireContext());
        title.setText("Lobby");
        title.setTextSize(26f);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, createMatchWrapParams(0, 24));

        displayNameInput = new EditText(requireContext());
        displayNameInput.setHint("Enter display name");
        root.addView(displayNameInput, createMatchWrapParams(0, 16));

        lobbyCodeInput = new EditText(requireContext());
        lobbyCodeInput.setHint("Enter lobby code to join");
        lobbyCodeInput.setInputType(InputType.TYPE_CLASS_TEXT);
        root.addView(lobbyCodeInput, createMatchWrapParams(0, 16));

        createLobbyButton = new Button(requireContext());
        createLobbyButton.setText("Create Lobby");
        root.addView(createLobbyButton, createMatchWrapParams(0, 12));

        joinLobbyButton = new Button(requireContext());
        joinLobbyButton.setText("Join Lobby");
        root.addView(joinLobbyButton, createMatchWrapParams(0, 16));

        progressBar = new ProgressBar(requireContext());
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar, createWrapWrapParams(0, 16));

        statusTextView = new TextView(requireContext());
        statusTextView.setText("Create a new lobby or join with a code.");
        statusTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(statusTextView, createMatchWrapParams(0, 0));

        createLobbyButton.setOnClickListener(v -> createLobby());
        joinLobbyButton.setOnClickListener(v -> joinLobby());

        return root;
    }

    private void createLobby() {
        String displayName = getTrimmedText(displayNameInput);
        if (displayName.isEmpty()) {
            showMessage("Please enter a display name.");
            return;
        }

        String userId = authRepository.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            showMessage("You must be signed in before creating a lobby.");
            return;
        }

        setLoading(true);

        String lobbyCode = CodeUtils.generateCode();
        Lobby lobby = buildNewLobby(lobbyCode, userId, displayName);

        lobbyRepository.createLobby(lobbyCode, lobby, new LobbyRepository.LobbyCallback() {
            @Override
            public void onSuccess(@NonNull Lobby createdLobby) {
                if (!isAdded()) {
                    return;
                }

                setLoading(false);
                String message = "Lobby created: " + CodeUtils.formatCodeForDisplay(lobbyCode);
                statusTextView.setText(message);
                lobbyCodeInput.setText(lobbyCode);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                if (!isAdded()) {
                    return;
                }

                setLoading(false);
                showMessage(errorMessage);
            }
        });
    }

    private void joinLobby() {
        String displayName = getTrimmedText(displayNameInput);
        if (displayName.isEmpty()) {
            showMessage("Please enter a display name.");
            return;
        }

        String userId = authRepository.getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            showMessage("You must be signed in before joining a lobby.");
            return;
        }

        String rawLobbyCode = getTrimmedText(lobbyCodeInput);
        String lobbyCode = CodeUtils.normalizeCode(rawLobbyCode);
        if (!CodeUtils.isValidCode(lobbyCode)) {
            showMessage("Please enter a valid 6-character lobby code.");
            return;
        }

        setLoading(true);

        lobbyRepository.getLobby(lobbyCode, new LobbyRepository.LobbyCallback() {
            @Override
            public void onSuccess(@NonNull Lobby lobby) {
                if (!isAdded()) {
                    return;
                }

                List<Player> players = lobby.getPlayers();
                if (players == null) {
                    players = new ArrayList<>();
                    lobby.setPlayers(players);
                }

                for (Player existingPlayer : players) {
                    if (existingPlayer != null && userId.equals(existingPlayer.getId())) {
                        setLoading(false);
                        String message = "You are already in this lobby.";
                        statusTextView.setText(message);
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                Player player = new Player();
                player.setId(userId);
                player.setDisplayName(displayName);
                player.setCaught(false);
                player.setCatchCode(CodeUtils.generateCode());

                players.add(player);
                lobby.setPlayerCount(players.size());

                lobbyRepository.updateLobby(lobbyCode, lobby, new LobbyRepository.LobbyCallback() {
                    @Override
                    public void onSuccess(@NonNull Lobby updatedLobby) {
                        if (!isAdded()) {
                            return;
                        }

                        setLoading(false);
                        String message = "Joined lobby: " + CodeUtils.formatCodeForDisplay(lobbyCode);
                        statusTextView.setText(message);
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        if (!isAdded()) {
                            return;
                        }

                        setLoading(false);
                        showMessage(errorMessage);
                    }
                });
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                if (!isAdded()) {
                    return;
                }

                setLoading(false);
                showMessage(errorMessage);
            }
        });
    }

    @NonNull
    private Lobby buildNewLobby(@NonNull String lobbyCode,
                                @NonNull String userId,
                                @NonNull String displayName) {
        Lobby lobby = new Lobby();
        lobby.setCode(lobbyCode);
        lobby.setStarted(false);
        lobby.setMaxPlayers(8);
        lobby.setMatchDurationSeconds(300);

        Player hostPlayer = new Player();
        hostPlayer.setId(userId);
        hostPlayer.setDisplayName(displayName);
        hostPlayer.setCaught(false);
        hostPlayer.setCatchCode(CodeUtils.generateCode());

        List<Player> players = new ArrayList<>();
        players.add(hostPlayer);

        lobby.setPlayers(players);
        lobby.setPlayerCount(players.size());
        return lobby;
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        createLobbyButton.setEnabled(!isLoading);
        joinLobbyButton.setEnabled(!isLoading);
    }

    private void showMessage(@NonNull String message) {
        if (!isAdded()) {
            return;
        }

        statusTextView.setText(message);
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    @NonNull
    private String getTrimmedText(@Nullable EditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    @NonNull
    private LinearLayout.LayoutParams createMatchWrapParams(int topMarginDp, int bottomMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(topMarginDp);
        params.bottomMargin = dpToPx(bottomMarginDp);
        return params;
    }

    @NonNull
    private LinearLayout.LayoutParams createWrapWrapParams(int topMarginDp, int bottomMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(topMarginDp);
        params.bottomMargin = dpToPx(bottomMarginDp);
        return params;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
