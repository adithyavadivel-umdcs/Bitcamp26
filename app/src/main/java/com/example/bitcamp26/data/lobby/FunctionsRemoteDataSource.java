package com.example.bitcamp26.data.lobby;

import androidx.annotation.NonNull;

import com.example.bitcamp26.core.util.FirebaseBackendConfig;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Typed Android wrappers for the server-authoritative Firebase Cloud Functions.
 *
 * Usage: inject this class wherever a ViewModel or use-case needs to call
 * backend game logic.  All methods are fire-and-forget with a typed callback.
 *
 * NOTE — createLobby / joinLobby are intentionally commented out until
 * Lobby.java is updated to match the Cloud Function data model
 * (field names: inviteCode vs code, state vs started, players map vs list).
 * Wiring those without that alignment would silently break listenToLobby.
 */
public class FunctionsRemoteDataSource {

    // ── Callback types ───────────────────────────────────────────────────────

    public interface FunctionCallback {
        void onSuccess(@NonNull Map<String, Object> result);
        void onError(@NonNull Exception e);
    }

    // ── Firebase Functions instance ──────────────────────────────────────────

    private final FirebaseFunctions functions;

    public FunctionsRemoteDataSource() {
        this(FirebaseFunctions.getInstance(FirebaseBackendConfig.FUNCTIONS_REGION));
    }

    public FunctionsRemoteDataSource(@NonNull FirebaseFunctions functions) {
        this.functions = functions;
    }

    public void callCreateLobby(@NonNull String displayName,
                                @NonNull FunctionCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("displayName", displayName);
        callFunction("createLobby", data, callback);
    }

    public void callJoinLobby(@NonNull String inviteCode,
                              @NonNull String displayName,
                              @NonNull FunctionCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("inviteCode", inviteCode);
        data.put("displayName", displayName);
        callFunction("joinLobby", data, callback);
    }

    // ── startGame ────────────────────────────────────────────────────────────
    /**
     * Host-only. Assigns catch codes, places hotspots, transitions state to RUNNING.
     * Wire to the host "Start Game" button in ReadyCheckFragment (Person 1).
     *
     * @param lobbyId the lobby's invite code (= lobby path key)
     */
    public void callStartGame(@NonNull String lobbyId,
                              @NonNull FunctionCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("lobbyId", lobbyId);
        callFunction("startGame", data, callback);
    }

    // ── submitCatchCode ──────────────────────────────────────────────────────
    /**
     * Seeker submits a 6-char code. Server validates eligibility and eliminates
     * the matched hider.
     *
     * Result keys: "success" (Boolean), "eliminatedUserId" (String, on success),
     *              "reason" (String "NO_ELIGIBLE_MATCH", on failure).
     *
     * Wire to the code-entry submit button (Person 1).
     *
     * @param lobbyId the lobby's invite code
     * @param code    the 6-char code the seeker typed
     */
    public void callSubmitCatchCode(@NonNull String lobbyId,
                                    @NonNull String code,
                                    @NonNull FunctionCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("lobbyId", lobbyId);
        data.put("code", code);
        callFunction("submitCatchCode", data, callback);
    }

    // ── shrinkCircle ─────────────────────────────────────────────────────────
    /**
     * Host-only. Shrinks the play area and eliminates out-of-bounds players.
     * Call this when the local countdown timer reaches timing.nextShrinkAt.
     *
     * Result keys: "success" (Boolean), "newRadiusMeters" (Double),
     *              "eliminated" (List<String>), "gameEnded" (Boolean).
     *
     * Wire to the shrink timer in MatchViewModel (Person 1 / Person 2).
     *
     * @param lobbyId the lobby's invite code
     */
    public void callShrinkCircle(@NonNull String lobbyId,
                                 @NonNull FunctionCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("lobbyId", lobbyId);
        callFunction("shrinkCircle", data, callback);
    }

    // ── claimHotspot ─────────────────────────────────────────────────────────
    /**
     * Claims a hotspot. Server uses a transaction — first valid claim wins.
     *
     * Result keys: "success" (Boolean), "powerupType" (String).
     *
     * Wire to the hotspot claim button on the map (Person 2).
     *
     * @param lobbyId   the lobby's invite code
     * @param hotspotId the hotspot ID (e.g. "hs_0")
     */
    public void callClaimHotspot(@NonNull String lobbyId,
                                 @NonNull String hotspotId,
                                 @NonNull FunctionCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("lobbyId", lobbyId);
        data.put("hotspotId", hotspotId);
        callFunction("claimHotspot", data, callback);
    }

    // ── activatePowerup ──────────────────────────────────────────────────────
    /**
     * Player activates their held powerup, starting the duration timer.
     * Can only be called once per hotspot claim (server enforces this).
     *
     * Result keys: "success" (Boolean), "powerup" (String),
     *              "powerupEndsAt" (Long ms), "durationSeconds" (Double).
     *
     * Wire to the powerup activate button (Person 1 / Person 2).
     *
     * @param lobbyId the lobby's invite code
     */
    public void callActivatePowerup(@NonNull String lobbyId,
                                    @NonNull FunctionCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("lobbyId", lobbyId);
        callFunction("activatePowerup", data, callback);
    }

    // ── Private helper ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void callFunction(@NonNull String name,
                              @NonNull Map<String, Object> data,
                              @NonNull FunctionCallback callback) {
        functions.getHttpsCallable(name)
                .call(data)
                .addOnSuccessListener((HttpsCallableResult result) -> {
                    Object raw = result.getData();
                    if (raw instanceof Map) {
                        callback.onSuccess((Map<String, Object>) raw);
                    } else {
                        // Function returned unexpected type — treat as empty success map.
                        callback.onSuccess(new HashMap<>());
                    }
                })
                .addOnFailureListener(error -> callback.onError(normalizeError(error)));
    }

    @NonNull
    private Exception normalizeError(@NonNull Exception error) {
        if (!(error instanceof FirebaseFunctionsException)) {
            return error;
        }

        FirebaseFunctionsException functionsError = (FirebaseFunctionsException) error;
        FirebaseFunctionsException.Code code = functionsError.getCode();
        String message;

        switch (code) {
            case NOT_FOUND:
                message = "Cloud Function endpoint was not found. Deploy the latest Firebase "
                        + "Functions for project " + FirebaseBackendConfig.PROJECT_ID + ".";
                break;
            case UNAUTHENTICATED:
                message = "You must be signed in to call the lobby backend.";
                break;
            case INVALID_ARGUMENT:
                message = functionsError.getMessage() != null
                        ? functionsError.getMessage()
                        : "The lobby request was missing required data.";
                break;
            default:
                message = functionsError.getMessage() != null
                        ? functionsError.getMessage()
                        : "The lobby backend request failed.";
                break;
        }

        return new Exception(message, error);
    }
}
