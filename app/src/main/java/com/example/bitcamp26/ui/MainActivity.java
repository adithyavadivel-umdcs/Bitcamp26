package com.example.bitcamp26.ui;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bitcamp26.navigation.AppNavigator;

/**
 * Main activity that hosts the app's fragment-based flow.
 *
 * Responsibilities:
 * - enables edge-to-edge drawing
 * - creates a fragment container programmatically
 * - applies system bar insets safely
 * - initializes AppNavigator
 * - shows the default lobby screen on first launch
 */
public class MainActivity extends AppCompatActivity {

    private AppNavigator appNavigator;

    @IdRes
    private static final int FRAGMENT_CONTAINER_ID = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        FrameLayout root = new FrameLayout(this);
        root.setId(FRAGMENT_CONTAINER_ID);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setContentView(root);

        appNavigator = new AppNavigator(getSupportFragmentManager(), FRAGMENT_CONTAINER_ID);

        if (savedInstanceState == null) {
            appNavigator.showLobby();
        }
    }

    public AppNavigator getAppNavigator() {
        return appNavigator;
    }

    @Override
    public void onBackPressed() {
        if (appNavigator != null && appNavigator.goBack()) {
            return;
        }
        super.onBackPressed();
    }
}