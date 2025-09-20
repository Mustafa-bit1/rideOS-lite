//package com.fanconn.rideOS.authen;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.TextView;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.fanconn.rideOS.R;
//
//
//public class MainActivity extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        View decorView = getWindow().getDecorView();
//        decorView.setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_IMMERSIVE
//                        | View.SYSTEM_UI_FLAG_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//        );
//
//        // Initialize SecureStorageUtil for secure data storage
//        try {
//            SecureStorageUtil.init(this);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        // Check if user is logged in
//        if (isUserLoggedIn()) {
//            // If the user is already logged in, show the welcome message
//            displayWelcomeMessage();
//        } else {
//            // If the user is not logged in, show the login options
//            navigateToLogin();
//        }
//
//        Button btnViewUsers = findViewById(R.id.btnViewUsers);
//        btnViewUsers.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this, UserActivity.class);
//            startActivity(intent);
//        });
//
//    }
//
//    // Check if the user is logged in (using secure storage or SharedPreferences)
//    private boolean isUserLoggedIn() {
//        // Replace with your actual check for logged-in status
//        return SecureStorageUtil.isUserLoggedIn(); // Modify this as needed
//    }
//
//    // Display the welcome message on the MainActivity
//    private void displayWelcomeMessage() {
//        // Get the username or details from secure storage
//        String username = SecureStorageUtil.getLoggedInUsername(); // Retrieve the username
//
//        // Find the TextView and set the welcome message
//        TextView welcomeMessageTextView = findViewById(R.id.tvWelcomeMessage);
//        String welcomeMessage = "Welcome to the app, " + username + "!";
//        welcomeMessageTextView.setText(welcomeMessage);
//    }
//
//    // Redirect to the LoginActivity if the user is not logged in
//    private void navigateToLogin() {
//        findViewById(R.id.btnRegister).setOnClickListener(v -> {
//            startActivity(new Intent(this, RegisterActivity.class));
//        });
//
//        findViewById(R.id.btnLogin).setOnClickListener(v -> {
//            startActivity(new Intent(this, LoginActivity.class));
//        });
//    }
//}
