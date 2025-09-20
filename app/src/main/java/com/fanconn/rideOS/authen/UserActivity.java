package com.fanconn.rideOS.authen;


import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.fanconn.rideOS.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UserActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.users_list);

        listView = findViewById(R.id.listViewUsers);

        try {
            android.content.SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    "user_data",
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    getApplicationContext(),
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

            userList.addAll(uniqueUsernames);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userList);
            listView.setAdapter(adapter);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
