package com.example.bitcamp26.navigation;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.Lobby;
import com.example.bitcamp26.feature.auth.AuthActivity;
import com.example.bitcamp26.feature.lobby.LobbyFragment;
import com.example.bitcamp26.feature.match.MatchFragment;
import com.example.bitcamp26.feature.ready.ReadyCheckFragment;
import com.example.bitcamp26.feature.results.ResultsFragment;

/**
 * Simple navigation helper for fragment-based screen changes inside MainActivity.
 *
 * This navigator intentionally avoids the Android Navigation Component for now so the
 * MVP stays easy to reason about. It provides a small, explicit API for replacing the
 * current fragment inside a container and passing minimal state into the destination.
 *
 * Supported destinations:
 * - LobbyFragment
 * - ReadyCheckFragment
 * - MatchFragment
 * - ResultsFragment
 *
 * Typical usage from an Activity:
 *
 * <pre>
 * AppNavigator navigator = new AppNavigator(getSupportFragmentManager(), R.id.fragment_container);
 * navigator.showLobby();
 * </pre>
 */
public class AppNavigator {

    private final FragmentManager fragmentManager;
    private final int containerId;

    public AppNavigator(@NonNull FragmentManager fragmentManager,
                        @IdRes int containerId) {
        this.fragmentManager = fragmentManager;
        this.containerId = containerId;
    }

    /**
     * Navigates to the lobby screen without adding the transaction to the back stack.
     * Use this as a default/home destination.
     */
    public void showLobby() {
        showLobby(null, null, false);
    }

    /**
     * Navigates to the lobby screen and optionally injects lobby/current-player context.
     */
    public void showLobby(@Nullable Lobby lobby,
                          @Nullable String currentPlayerId,
                          boolean addToBackStack) {
        LobbyFragment fragment = new LobbyFragment();
        replaceFragment(fragment, Routes.LOBBY, addToBackStack);
    }

    /**
     * Navigates to the ready-check screen.
     */
    public void showReadyCheck(@Nullable Lobby lobby,
                               @Nullable String currentPlayerId,
                               boolean addToBackStack) {
        ReadyCheckFragment fragment = new ReadyCheckFragment();
        if (lobby != null) {
            fragment.setLobby(lobby);
        }
        if (currentPlayerId != null) {
            fragment.setCurrentPlayerId(currentPlayerId);
        }
        replaceFragment(fragment, Routes.READY_CHECK, addToBackStack);
    }

    /**
     * Navigates to the active match screen.
     */
    public void showMatch(@Nullable GameState gameState,
                          @Nullable String currentPlayerId,
                          boolean addToBackStack) {
        MatchFragment fragment = new MatchFragment();
        if (gameState != null || currentPlayerId != null) {
            fragment.setInitialData(gameState, currentPlayerId);
        }
        replaceFragment(fragment, Routes.MATCH, addToBackStack);
    }

    /**
     * Navigates to the results screen.
     */
    public void showResults(@Nullable GameState gameState,
                            @Nullable String currentPlayerId,
                            boolean addToBackStack) {
        ResultsFragment fragment = new ResultsFragment();
        if (gameState != null) {
            fragment.setGameState(gameState);
        }
        if (currentPlayerId != null) {
            fragment.setCurrentPlayerId(currentPlayerId);
        }
        replaceFragment(fragment, Routes.RESULTS, addToBackStack);
    }

    /**
     * Replaces the current fragment with the provided fragment.
     */
    public void replaceFragment(@NonNull Fragment fragment,
                                @NonNull String backStackName,
                                boolean addToBackStack) {
        FragmentTransaction transaction = fragmentManager
                .beginTransaction()
                .replace(containerId, fragment, backStackName);

        if (addToBackStack) {
            transaction.addToBackStack(backStackName);
        }

        transaction.commit();
    }

    /**
     * Returns the fragment currently attached to the navigation container.
     */
    @Nullable
    public Fragment getCurrentFragment() {
        return fragmentManager.findFragmentById(containerId);
    }

    /**
     * Returns true if there is at least one entry on the back stack.
     */
    public boolean canGoBack() {
        return fragmentManager.getBackStackEntryCount() > 0;
    }

    /**
     * Pops the back stack if possible.
     *
     * @return true if a back-stack entry existed and was popped, otherwise false.
     */
    public boolean goBack() {
        if (!canGoBack()) {
            return false;
        }

        fragmentManager.popBackStack();
        return true;
    }

    /**
     * Clears the entire fragment back stack.
     */
    public void clearBackStack() {
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    /**
     * Convenience helper for launching the auth flow from places that only need a route name.
     *
     * Since AuthActivity is an Activity, this class does not launch it directly here.
     * MainActivity or another caller should handle the Intent launch when needed.
     */
    @NonNull
    public String getAuthRoute() {
        return Routes.AUTH;
    }

    /**
     * Returns the current number of entries on the fragment back stack.
     */
    public int getBackStackCount() {
        return fragmentManager.getBackStackEntryCount();
    }
}
