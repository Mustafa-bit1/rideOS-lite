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

import com.fanconn.rideOS.GestureHelper;
import com.fanconn.rideOS.R;

public class menu extends AppCompatActivity {
    public static menu setinstance;

    // ✅ Only menu section items
    String[] titles = {"Display", "Wireless", "Features", "rider Profiles", "OS Updates"};
    int[] icons = {
            R.drawable.displayicon,       // Display
            R.drawable.wirelessicon,      // Wireless
            R.drawable.featureicon,       // Features
            R.drawable.riderprofileicon,  // Rider Profiles
            R.drawable.osupdates_icon     // OS Updates
    };

    private ListView menuList;
    private ImageButton menubackbtn;
    private MenuAdapter adapter;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        setinstance = this;
        setFullScreen();

        menuList = findViewById(R.id.menuList);
        menubackbtn = findViewById(R.id.menubackbtn);

        // Hide navigation bars
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        // ✅ Setup adapter and default fragment
        adapter = new MenuAdapter(this, titles, icons);
        menuList.setAdapter(adapter);
        loadFragment(new DisplayFragment());
        adapter.setSelectedPosition(0);

        // ✅ Handle clicks
        menuList.setOnItemClickListener((parent, view, position, id) -> {
            Fragment fragment;
            switch (position) {
                case 0:
                    fragment = new DisplayFragment();
                    break;
                case 1:
                    fragment = new WirelessFragment();
                    break;
                case 2:
                    fragment = new FeaturesFragment();
                    break;
                case 3:
                    fragment = new riderProfilesFragment();
                    break;
                case 4:
                    fragment = new OS_UpdatesFragment();
                    break;
                default:
                    return;
            }

            loadFragment(fragment);
            adapter.setSelectedPosition(position);
        });

        menubackbtn.setOnClickListener(v -> finish());
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
                .replace(R.id.contentFrame, fragment)
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
