package com.example.bitcamp26.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bitcamp26.data.auth.AuthRepository;
import com.example.bitcamp26.ui.MainActivity;
import com.google.firebase.auth.FirebaseUser;

/**
 * Simple authentication screen that signs the user in anonymously.
 *
 * This activity uses a programmatically-created layout so it can run
 * even before a dedicated XML screen is added.
 */
public class AuthActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private ProgressBar progressBar;
    private Button signInButton;
    private TextView titleTextView;
    private TextView subtitleTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authRepository = new AuthRepository();
        setContentView(createContentView());

        signInButton.setText("Sign In / Continue as Guest");

        if (authRepository.isSignedIn()) {
            openMainScreen();
            return;
        }

        signInButton.setOnClickListener(view -> signInAnonymously());
    }

    private LinearLayout createContentView() {
        int padding = dpToPx(24);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(padding, padding, padding, padding);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        titleTextView = new TextView(this);
        titleTextView.setText("HideNSeek");
        titleTextView.setTextSize(28f);
        titleTextView.setGravity(Gravity.CENTER);

        subtitleTextView = new TextView(this);
        subtitleTextView.setText("Sign in to join or host a match.");
        subtitleTextView.setTextSize(16f);
        subtitleTextView.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.bottomMargin = dpToPx(12);

        root.addView(titleTextView, textParams);
        root.addView(subtitleTextView, textParams);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(ProgressBar.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        progressParams.bottomMargin = dpToPx(16);
        root.addView(progressBar, progressParams);

        signInButton = new Button(this);
        signInButton.setText("Sign In / Continue as Guest");
        root.addView(signInButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        return root;
    }

    private void signInAnonymously() {
        setLoading(true);

        authRepository.signInAnonymously(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                setLoading(false);
                if (user != null) {
                    Toast.makeText(
                            AuthActivity.this,
                            "Signed in successfully.",
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Toast.makeText(
                            AuthActivity.this,
                            "Continuing in guest mode.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
                openMainScreen();
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                setLoading(false);
                Toast.makeText(
                        AuthActivity.this,
                        "Sign-in failed. Please try again.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? ProgressBar.VISIBLE : ProgressBar.GONE);
        signInButton.setEnabled(!isLoading);
        signInButton.setText(isLoading ? "Signing In..." : "Sign In / Continue as Guest");
    }

    private void openMainScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
