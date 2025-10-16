package com.fanconn.rideOS.menu;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.fanconn.rideOS.menu.BatteryFragment;
import com.fanconn.rideOS.GestureHelper;
import com.fanconn.rideOS.R;

public class settings extends AppCompatActivity {
    public static settings instance;

    // ✅ settings section items
    String[] titles = {"Battery", "rideCam", "Documents", "Support", "Information", "Diagnostic"};
    int[] icons = {
            R.drawable.batteryicononmenu,//ridecamicon
            R.drawable.ridecamicon,
            R.drawable.docicon,
            R.drawable.supporticon,
            R.drawable.informationicon,
            R.drawable.diagnosticicon
    };

    private ListView settingsList;
    private ImageButton settingsBackBtn;
    private MenuAdapter adapter;
    private GestureDetector gestureDetector;

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

        // ✅ Setup adapter
        adapter = new MenuAdapter(this, titles, icons);
        settingsList.setAdapter(adapter);

        // ✅ Default fragment
        loadFragment(new BatteryFragment());
        adapter.setSelectedPosition(0);

        // ✅ Handle item clicks
        settingsList.setOnItemClickListener((parent, view, position, id) -> {
            Fragment fragment;
            switch (position) {
                case 0: fragment = new BatteryFragment(); break;//RideCamFragment
                case 1: fragment = new RideCamFragment(); break;
                case 2: fragment = new DocsFragment(); break;
                case 3: fragment = new supportFragment(); break;
                case 4: fragment = new informationFragment(); break;
                case 5: fragment = new DiagnosticsFragment(); break;
                default: return;
            }

            loadFragment(fragment);
            adapter.setSelectedPosition(position);
        });

        settingsBackBtn.setOnClickListener(v -> finish());
    }

    float[] startY = new float[1];

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (GestureHelper.handleGesture(this, ev, startY)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settingsContentFrame, fragment)
                .commit();
    }

    private void setFullScreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
