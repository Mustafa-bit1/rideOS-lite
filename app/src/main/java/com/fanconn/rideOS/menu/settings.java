package com.fanconn.rideOS.menu;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.fanconn.rideOS.GestureHelper;
import com.fanconn.rideOS.R;

public class settings extends AppCompatActivity {

    public static settings instance;

    // Cached fragments (avoid recreation)
    private final BatteryFragment batteryFragment = new BatteryFragment();
    private final RideCamFragment rideCamFragment = new RideCamFragment();
    private final DocsFragment docsFragment = new DocsFragment();
    private final supportFragment supportFragment = new supportFragment();
    private final informationFragment informationFragment = new informationFragment();
    private final DiagnosticsFragment diagnosticsFragment = new DiagnosticsFragment();

    // Settings items
    private final String[] titles = {
            "Battery", "rideCam", "Documents", "Support", "Information", "Diagnostic"
    };

    private final int[] icons = {
            R.drawable.batteryicononmenu,
            R.drawable.ridecamicon,
            R.drawable.docicon,
            R.drawable.supporticon,
            R.drawable.informationicon,
            R.drawable.diagnosticicon
    };

    private ListView settingsList;
    private ImageButton settingsBackBtn;
    private MenuAdapter adapter;

    private final float[] startY = new float[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        instance = this;
        setFullScreen();

        settingsList = findViewById(R.id.settingsMenuList);
        settingsBackBtn = findViewById(R.id.settingsBackBtn);

        // immersive fullscreen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        // Adapter
        adapter = new MenuAdapter(this, titles, icons);
        settingsList.setAdapter(adapter);

        // Load first fragment instantly
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settingsContentFrame, batteryFragment)
                .commitNow();

        adapter.setSelectedPosition(0);

        // Handle menu click
        settingsList.setOnItemClickListener((parent, view, position, id) -> {

            Fragment fragment = batteryFragment;

            switch (position) {
                case 0:
                    fragment = batteryFragment;
                    break;

                case 1:
                    fragment = rideCamFragment;
                    break;

                case 2:
                    fragment = docsFragment;
                    break;

                case 3:
                    fragment = supportFragment;
                    break;

                case 4:
                    fragment = informationFragment;
                    break;

                case 5:
                    fragment = diagnosticsFragment;
                    break;
            }

            loadFragment(fragment);
            adapter.setSelectedPosition(position);
        });

        settingsBackBtn.setOnClickListener(v -> finish());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (GestureHelper.handleGesture(this, ev, startY)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    // Fragment loader
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settingsContentFrame, fragment)
                .commit();
    }

    private void setFullScreen() {

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        View decorView = getWindow().getDecorView();

        int uiOptions =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}