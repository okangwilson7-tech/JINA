package com.jina.voiceassistant;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView wakeNameBadge;
    private TextView statusText;
    private TextView historyText;
    private LinearLayout historyLayout;
    private SharedPreferences prefs;
    private String wakeName;
    private StringBuilder historyLog = new StringBuilder();

    // Receives updates from the background service
    public static MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;

        prefs = getSharedPreferences("jina_prefs", MODE_PRIVATE);
        wakeName = prefs.getString("wake_name", "JINA");

        wakeNameBadge = findViewById(R.id.wake_name_badge);
        statusText = findViewById(R.id.status_text);
        historyText = findViewById(R.id.history_text);
        historyLayout = findViewById(R.id.history_layout);

        wakeNameBadge.setText("🎙  Listening for \"" + wakeName + "\"");

        // Start background listener service
        startJinaService();

        // Show contacts button
        Button contactsBtn = findViewById(R.id.contacts_button);
        contactsBtn.setOnClickListener(v -> showContacts());

        // Change name button
        Button changeNameBtn = findViewById(R.id.change_name_button);
        changeNameBtn.setOnClickListener(v -> changeName());

        // Stop/Start service button
        Button toggleBtn = findViewById(R.id.toggle_button);
        toggleBtn.setOnClickListener(v -> {
            if (JinaListenerService.isRunning) {
                stopJinaService();
                toggleBtn.setText("▶  START JINA");
                updateStatus("JINA is stopped. Tap START to activate.");
            } else {
                startJinaService();
                toggleBtn.setText("⏹  STOP JINA");
                updateStatus("JINA is listening for \"" + wakeName + "\"...");
            }
        });
    }

    private void startJinaService() {
        Intent serviceIntent = new Intent(this, JinaListenerService.class);
        serviceIntent.putExtra("wake_name", wakeName);
        startForegroundService(serviceIntent);
        updateStatus("JINA is listening for \"" + wakeName + "\"...");
    }

    private void stopJinaService() {
        Intent serviceIntent = new Intent(this, JinaListenerService.class);
        stopService(serviceIntent);
    }

    private void showContacts() {
        List<ContactHelper.Contact> contacts = ContactHelper.getAllContacts(this);
        StringBuilder sb = new StringBuilder();
        sb.append("=== YOUR CONTACTS (").append(contacts.size()).append(") ===\n\n");
        for (ContactHelper.Contact c : contacts) {
            sb.append("• ").append(c.name).append("\n");
            sb.append("  ").append(c.phone).append("\n\n");
        }
        historyText.setText(sb.toString());
        historyLayout.setVisibility(View.VISIBLE);
    }

    private void changeName() {
        prefs.edit().remove("wake_name").apply();
        stopJinaService();
        startActivity(new Intent(this, SetupActivity.class));
        finish();
    }

    // Called by service to update status on screen
    public void updateStatus(final String status) {
        runOnUiThread(() -> {
            if (statusText != null) {
                statusText.setText(status);
            }
        });
    }

    // Called by service to log a command in history
    public void logCommand(final String command, final String result) {
        runOnUiThread(() -> {
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            historyLog.insert(0, "[" + time + "] " + command + "\n→ " + result + "\n\n");
            if (historyText != null) {
                historyText.setText(historyLog.toString());
            }
            historyLayout.setVisibility(View.VISIBLE);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }
}
