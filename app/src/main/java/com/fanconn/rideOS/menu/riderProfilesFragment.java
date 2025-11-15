package com.fanconn.rideOS.menu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.fanconn.rideOS.InactivityManager;
import com.fanconn.rideOS.R;
import com.fanconn.rideOS.SoundPlayer;
import com.fanconn.rideOS.authen.RegisterActivity;
import com.fanconn.rideOS.authen.SecureStorageUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class riderProfilesFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private LinearLayout ridersContainer;
    private ImageButton  btnRegister;

    private Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_rider_profiles, container, false);

        context = requireContext();  // save context

        btnRegister = view.findViewById(R.id.btnRegister);
        ridersContainer = view.findViewById(R.id.ridersContainer);

        try {
            sharedPreferences = EncryptedSharedPreferences.create(
                    "user_data",
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            SecureStorageUtil.init(context);
            loadUsers();

        } catch (Exception e) {
            e.printStackTrace();
        }

        btnRegister.setOnClickListener(v -> {
            if (getRegisteredRiderCount() >= 4) {
//                Toast.makeText(context, "Only 4 riders can be registered", Toast.LENGTH_SHORT).show();
                showCustomToasterror("Maximum 4 Riders");
            } else {
                InactivityManager.getInstance().stopTimer();
                Intent intent = new Intent(context, com.fanconn.rideOS.authen.RegisterActivity.class);
                startActivityForResult(intent, 101);  // 101 is request code
            }
        });

        return view;
    }

    private int getRegisteredRiderCount() {
        int count = 0;
        try {
            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    "user_data",
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    requireContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            Set<String> uniqueUsernames = new HashSet<>();
            for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("user_") && key.contains("_age")) {
                    String username = key.replace("user_", "").replace("_age", "");
                    uniqueUsernames.add(username);
                }
            }
            count = uniqueUsernames.size();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            // Optional: retrieve the rider name
            String newRider = data.getStringExtra("newRider");

            // Now reload the riders list
            loadUsers();  // or whatever method builds rider cards
        }
    }

    private void loadUsers() {
        ridersContainer.removeAllViews();
        Set<String> uniqueUsernames = new HashSet<>();
        for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("user_") && key.contains("_age")) {
                String username = key.replace("user_", "").replace("_age", "");
                uniqueUsernames.add(username);
            }
        }

        for (String username : uniqueUsernames) {
            addRiderCard(username);
        }
    }

    private int getRiderCount() {
        int count = 0;
        for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("user_") && key.contains("_age")) {
                count++;
            }
        }
        return count;
    }


    private void addRiderCard(String username) {
        View card = LayoutInflater.from(context).inflate(R.layout.rider_card, ridersContainer, false);
        TextView nameText = card.findViewById(R.id.riderName);
        ImageButton deleteBtn = card.findViewById(R.id.deleteBtn);

        nameText.setText(username);

        deleteBtn.setOnClickListener(v -> {
            int riderCount = getRiderCount();

            if (riderCount <= 1) {
                showCustomToasterror("At least one rider must remain");
                return;
            }

            // Inflate your custom popup layout
            View dialogView = LayoutInflater.from(context).inflate(R.layout.popupdialog, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(dialogView);

            // Create the dialog
            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            // Access views inside popupdialog.xml
            TextView title = dialogView.findViewById(R.id.dialogTitle);
            TextView message = dialogView.findViewById(R.id.dialogMessage);
            Button btnYes = dialogView.findViewById(R.id.btnYes);
            Button btnNo = dialogView.findViewById(R.id.btnNo);

            // Customize text
            title.setText("Confirm Deletion");
            message.setText("Delete rider \"" + username + "\" ?");
            btnYes.setText("Delete");
            btnNo.setText("Cancel");

            // Button actions
            btnYes.setOnClickListener(b -> {
                dialog.dismiss();
                deleteUser(username);
            });

            btnNo.setOnClickListener(b -> dialog.dismiss());

            // Show dialog
            dialog.show();
        });

        ridersContainer.addView(card);
    }


    private void deleteUser(String username) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String[] keysToDelete = {
                    "user_" + username + "_age",
                    "user_" + username + "_lucky",
                    "user_" + username + "_torque",
                    "user_" + username + "_salt",
                    "user_" + username + "_pin",
                    "user_" + username + "_pattern"
            };
            for (String key : keysToDelete) editor.remove(key);
            editor.apply();

            SoundPlayer.playOnce(context, R.raw.deletesound);
            loadUsers();
            showCustomToast("Rider Deleted " + username);
        } catch (Exception e) {
            e.printStackTrace();
            showCustomToasterror("Error deleting " + username);
        }
    }

    private void showCustomToast(String message) {
        View layout = LayoutInflater.from(context).inflate(R.layout.toast_custom, null);
        ImageView icon = layout.findViewById(R.id.toast_icon);
        TextView text = layout.findViewById(R.id.toast_text);
        icon.setImageResource(R.drawable.toastlogo);
        text.setText(message);

        Toast toast = new Toast(context.getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
    private void showCustomToasterror(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast_errror, null);
        ImageView icon = layout.findViewById(R.id.toast_icon);
        TextView text = layout.findViewById(R.id.toast_text);
        icon.setImageResource(R.drawable.toastlogo);
        text.setText(message);
        Toast toast = new Toast(context.getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}
