package com.example.bitcamp26.ui;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bitcamp26.R;
import com.example.bitcamp26.navigation.AppNavigator;

public class MainActivity extends AppCompatActivity {

    private AppNavigator appNavigator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragment_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        appNavigator = new AppNavigator(getSupportFragmentManager(), R.id.fragment_container);

        if (savedInstanceState == null) {
            appNavigator.showLobby();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (appNavigator != null && appNavigator.goBack()) {
                    return;
                }
                finish();
            }
        });
    }

    public AppNavigator getAppNavigator() {
        return appNavigator;
    }
}