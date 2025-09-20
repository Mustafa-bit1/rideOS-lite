package com.fanconn.rideOS.menu;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.fanconn.rideOS.GestureHelper;
import com.fanconn.rideOS.InactivityManager;
import com.fanconn.rideOS.R;

public class settings extends AppCompatActivity {
    public static settings setinstance;
    String[] titles = {"Display", "Wireless", "Features","rider Profiles", "rideCam", "Battery", "Documents", "OS Updates", "Support","Information", "Diagnostic"};
    int[] icons = {
            R.drawable.displayicon, // display
            R.drawable.wirelessicon, // wireless
            R.drawable.featureicon,  // features
            R.drawable.riderprofileicon,  // features
            R.drawable.ridecamicon, // rideCam
            R.drawable.batteryicononmenu, // battery
            R.drawable.docicon, // documents
            R.drawable.osupdates_icon, // battery
            R.drawable.supporticon, // battery
            R.drawable.informationicon, // battery
            R.drawable.diagnosticicon  // features
    };

    private ListView menuList;
    private ImageButton menubackbtn;
    private MenuAdapter adapter;  // ✅ our custom adapter
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setinstance = this;
        setFullScreen();

//        InactivityManager.getInstance().startTimer(5);
        menuList = findViewById(R.id.menuList);
        menubackbtn = findViewById(R.id.menubackbtn);
//        GestureHelper.enableSwipeGestures(this);
///for gestures of quick setting and Home page
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        // ✅ Custom adapter with icons
        adapter = new MenuAdapter(this, titles, icons);
        menuList.setAdapter(adapter);

        // ✅ Load default fragment
        loadFragment(new DisplayFragment());
        adapter.setSelectedPosition(0);  // default selection

        // ✅ Handle tab click & highlight
        menuList.setOnItemClickListener((parent, view, position, id) -> {
            Fragment fragment;
            switch (position) {
                case 0: fragment = new DisplayFragment(); break;
                case 1: fragment = new WirelessFragment(); break;
                case 2: fragment = new FeaturesFragment(); break;
                case 3: fragment = new riderProfilesFragment(); break;
                case 4: fragment = new RideCamFragment(); break;
                case 5: fragment = new BatteryFragment(); break;
                case 6: fragment = new DocsFragment(); break;
                case 7: fragment = new OS_UpdatesFragment(); break;
                case 8: fragment = new supportFragment(); break;
                case 9: fragment = new informationFragment(); break;
                case 10: fragment = new DiagnosticsFragment(); break;
                default: return;
            }

            loadFragment(fragment);
            adapter.setSelectedPosition(position);  // highlight selected tab
        });


        menubackbtn.setOnClickListener(v -> finish());
    }

    float[] startY = new float[1]; // Declare this as a field

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (GestureHelper.handleGesture(this, ev, startY)) {
            return true; // handled
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
        // 设置窗口标志，启用全屏模式
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 获取装饰视图
        View decorView = getWindow().getDecorView();

        // 设置系统 UI 选项，隐藏导航栏和状态栏，启用粘性沉浸模式
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }


    @Override
    protected void onResume() {
        super.onResume();
        //InactivityManager.getInstance().startTimer(4);
//        InactivityManager.getInstance().startTimer(5);
//        Log.d("sdfgdfh","RESUMED CHARGINGUI");
    }

}
