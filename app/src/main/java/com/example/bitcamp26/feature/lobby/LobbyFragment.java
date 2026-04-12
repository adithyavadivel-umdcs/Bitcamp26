package com.example.bitcamp26.feature.lobby;

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
import com.example.bitcamp26.core.model.Lobby;
import com.example.bitcamp26.navigation.AppNavigator;
import com.example.bitcamp26.ui.MainActivity;

/**
 * Lobby screen wired to backend-authoritative create/join flows.
 */
public class LobbyFragment extends Fragment {

    private LobbyViewModel viewModel;

    private EditText displayNameInput;
    private EditText lobbyCodeInput;
    private Button createLobbyButton;
    private Button joinLobbyButton;
    private ProgressBar progressBar;
    private TextView statusTextView;
    private boolean pendingNavigationToReadyCheck;

    public LobbyFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(LobbyViewModel.class);
        return inflater.inflate(R.layout.fragment_lobby, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        displayNameInput = view.findViewById(R.id.editDisplayName);
        lobbyCodeInput = view.findViewById(R.id.editLobbyCode);
        createLobbyButton = view.findViewById(R.id.buttonCreateLobby);
        joinLobbyButton = view.findViewById(R.id.buttonJoinLobby);
        progressBar = view.findViewById(R.id.progressLobby);
        statusTextView = view.findViewById(R.id.textLobbyStatus);

        createLobbyButton.setOnClickListener(v -> {
            pendingNavigationToReadyCheck = true;
            viewModel.createLobby(getTrimmedText(displayNameInput));
        });
        joinLobbyButton.setOnClickListener(v -> {
            pendingNavigationToReadyCheck = true;
            viewModel.joinLobby(getTrimmedText(lobbyCodeInput), getTrimmedText(displayNameInput));
        });

        bindObservers();
    }

    private void bindObservers() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading ->
                setLoading(Boolean.TRUE.equals(isLoading)));

        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message == null || message.trim().isEmpty() || !isAdded()) {
                return;
            }
            statusTextView.setText(message);
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message == null || message.trim().isEmpty()) {
                return;
            }
            pendingNavigationToReadyCheck = false;
            showMessage(message);
        });

        viewModel.getCurrentLobbyCode().observe(getViewLifecycleOwner(), lobbyCode -> {
            if (lobbyCode != null && !lobbyCode.trim().isEmpty()) {
                lobbyCodeInput.setText(lobbyCode);
            }
        });

        viewModel.getCurrentLobby().observe(getViewLifecycleOwner(), lobby -> {
            if (lobby == null) {
                return;
            }
            if (pendingNavigationToReadyCheck) {
                String userId = viewModel.getSignedInUserId();
                if (userId != null && !userId.trim().isEmpty()) {
                    pendingNavigationToReadyCheck = false;
                    navigateToReadyCheck(lobby, userId);
                }
            }
        });
    }

    private void navigateToReadyCheck(@NonNull Lobby lobby, @NonNull String currentPlayerId) {
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
        navigator.showReadyCheck(lobby, currentPlayerId, true);
    }

    private void setLoading(boolean isLoading) {
        if (progressBar == null || createLobbyButton == null || joinLobbyButton == null) {
            return;
        }
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
}
