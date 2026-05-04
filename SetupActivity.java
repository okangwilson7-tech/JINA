package com.jina.voiceassistant;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class SetupActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 100;
    private EditText nameInput;
    private Button startButton;
    private TextView statusText;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("jina_prefs", MODE_PRIVATE);

        // Already set up — go straight to main screen
        if (prefs.contains("wake_name") && !prefs.getString("wake_name", "").isEmpty()) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_setup);

        nameInput = findViewById(R.id.name_input);
        startButton = findViewById(R.id.start_button);
        statusText = findViewById(R.id.status_text);

        // Enable button only when name is long enough
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                boolean ready = s.toString().trim().length() >= 2;
                startButton.setEnabled(ready);
                startButton.setAlpha(ready ? 1.0f : 0.5f);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        startButton.setEnabled(false);
        startButton.setAlpha(0.5f);

        startButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.length() >= 2) {
                // Save wake name
                prefs.edit().putString("wake_name", name).apply();
                statusText.setText("Setting up " + name + "...");
                requestAllPermissions();
            }
        });
    }

    private void requestAllPermissions() {
        String[] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        };

        List<String> needed = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }

        if (needed.isEmpty()) {
            goToMain();
        } else {
            ActivityCompat.requestPermissions(
                this,
                needed.toArray(new String[0]),
                PERMISSION_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        // Go to main regardless — handle missing permissions gracefully
        goToMain();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
