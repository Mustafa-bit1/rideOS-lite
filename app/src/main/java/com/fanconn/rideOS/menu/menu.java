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

public class menu extends AppCompatActivity {

    public static menu setinstance;

    // Cached fragments (prevents recreation)
    private final DisplayFragment displayFragment = new DisplayFragment();
    private final WirelessFragment wirelessFragment = new WirelessFragment();
    private final FeaturesFragment featuresFragment = new FeaturesFragment();
    private final riderProfilesFragment riderProfilesFragment = new riderProfilesFragment();
    private final OS_UpdatesFragment osUpdatesFragment = new OS_UpdatesFragment();

    // Menu items
    private final String[] titles = {
            "Display",
            "Wireless",
            "Features",
            "rider Profiles",
            "OS Updates"
    };

    private final int[] icons = {
            R.drawable.displayicon,
            R.drawable.wirelessicon,
            R.drawable.featureicon,
            R.drawable.riderprofileicon,
            R.drawable.osupdates_icon
    };

    private ListView menuList;
    private ImageButton menubackbtn;
    private MenuAdapter adapter;

    private int currentPosition = -1;

    private final float[] startY = new float[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        setinstance = this;

        setFullScreen();

        menuList = findViewById(R.id.menuList);
        menubackbtn = findViewById(R.id.menubackbtn);

        // immersive fullscreen


        // Adapter
        adapter = new MenuAdapter(this, titles, icons);
        menuList.setAdapter(adapter);

        // Disable ListView animation for smoother embedded performance
        menuList.setLayoutAnimation(null);

        String openFragment = getIntent().getStringExtra("openFragment");

        Fragment firstFragment = displayFragment;
        int firstPosition = 0;

        if ("wireless".equals(openFragment)) {
            firstFragment = wirelessFragment;
            firstPosition = 1;
        }

        // Load first fragment instantly
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFrame, firstFragment)
                .commitNow();

        adapter.setSelectedPosition(firstPosition);
        currentPosition = firstPosition;

        // Handle menu clicks
        menuList.setOnItemClickListener((parent, view, position, id) -> {

            if (position == currentPosition) return;

            currentPosition = position;

            Fragment fragment = displayFragment;

            switch (position) {

                case 0:
                    fragment = displayFragment;
                    break;

                case 1:
                    fragment = wirelessFragment;
                    break;

                case 2:
                    fragment = featuresFragment;
                    break;

                case 3:
                    fragment = riderProfilesFragment;
                    break;

                case 4:
                    fragment = osUpdatesFragment;
                    break;
            }

            loadFragment(fragment);
            adapter.setSelectedPosition(position);
        });

        menubackbtn.setOnClickListener(v -> finish());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        if (GestureHelper.handleGesture(this, ev)) {
            return true;
        }

        return super.dispatchTouchEvent(ev);
    }

    private void loadFragment(Fragment fragment) {

        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.contentFrame, fragment)
                .disallowAddToBackStack()
                .commitAllowingStateLoss();
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
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }
}