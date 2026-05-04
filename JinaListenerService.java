package com.jina.voiceassistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;

public class JinaListenerService extends Service {

    private static final String TAG = "JINA";
    private static final String CHANNEL_ID = "jina_channel";
    private static final int NOTIF_ID = 1;

    public static boolean isRunning = false;

    private SpeechRecognizer speechRecognizer;
    private String wakeName = "JINA";
    private Handler handler;
    private PowerManager.WakeLock wakeLock;
    private boolean listeningForCommand = false;
    private boolean shouldRestart = true;

    // Delay before restarting listening after each session (milliseconds)
    private static final int RESTART_DELAY = 300;
    // How long to wait for command after wake word (milliseconds)
    private static final int COMMAND_LISTEN_DELAY = 500;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;

        if (intent != null && intent.hasExtra("wake_name")) {
            wakeName = intent.getStringExtra("wake_name");
        } else {
            SharedPreferences prefs = getSharedPreferences("jina_prefs", MODE_PRIVATE);
            wakeName = prefs.getString("wake_name", "JINA");
        }

        // Show persistent notification
        startForeground(NOTIF_ID, buildNotification("Listening for \"" + wakeName + "\"..."));

        shouldRestart = true;
        startListeningForWakeWord();

        // If killed, restart automatically
        return START_STICKY;
    }

    // ============================================================
    // PHASE 1: LISTEN FOR WAKE WORD
    // ============================================================
    private void startListeningForWakeWord() {
        if (!shouldRestart) return;
        listeningForCommand = false;

        handler.post(() -> {
            try {
                if (speechRecognizer != null) {
                    speechRecognizer.destroy();
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(JinaListenerService.this);
                speechRecognizer.setRecognitionListener(new WakeWordListener());

                Intent listenIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                listenIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                listenIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
                listenIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
                listenIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L);
                listenIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L);
                listenIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L);

                speechRecognizer.startListening(listenIntent);
                Log.d(TAG, "Listening for wake word: " + wakeName);

            } catch (Exception e) {
                Log.e(TAG, "Error starting listener: " + e.getMessage());
                restartAfterDelay(2000);
            }
        });
    }

    // ============================================================
    // PHASE 2: LISTEN FOR COMMAND (after wake word detected)
    // ============================================================
    private void startListeningForCommand() {
        listeningForCommand = true;
        updateNotification("Listening... speak your command");

        if (MainActivity.instance != null) {
            MainActivity.instance.updateStatus("🎙  Speak your command now...");
        }

        // Small vibration to signal JINA is awake
        vibratePhone();

        handler.postDelayed(() -> {
            try {
                if (speechRecognizer != null) {
                    speechRecognizer.destroy();
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(JinaListenerService.this);
                speechRecognizer.setRecognitionListener(new CommandListener());

                Intent listenIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                listenIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                listenIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
                listenIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
                listenIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L);
                listenIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L);

                speechRecognizer.startListening(listenIntent);
                Log.d(TAG, "Listening for command...");

            } catch (Exception e) {
                Log.e(TAG, "Error starting command listener: " + e.getMessage());
                restartAfterDelay(1000);
            }
        }, COMMAND_LISTEN_DELAY);
    }

    // ============================================================
    // WAKE WORD LISTENER
    // ============================================================
    private class WakeWordListener implements RecognitionListener {

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null) {
                for (String match : matches) {
                    Log.d(TAG, "Heard: " + match);

                    String lowerMatch = match.toLowerCase().trim();
                    String lowerWake = wakeName.toLowerCase().trim();

                    // Check if wake word is in what was heard
                    if (lowerMatch.contains(lowerWake)) {
                        Log.d(TAG, "WAKE WORD DETECTED!");

                        // Check if the full command is in the same sentence
                        // e.g., user said "Remmy call Rafiki" all at once
                        String afterWake = lowerMatch.substring(
                            lowerMatch.indexOf(lowerWake) + lowerWake.length()
                        ).trim();

                        if (afterWake.length() > 2) {
                            // Full command already captured
                            processFullCommand(match);
                        } else {
                            // Just wake word, listen for command next
                            startListeningForCommand();
                        }
                        return;
                    }
                }
            }
            // Wake word not detected, listen again
            restartAfterDelay(RESTART_DELAY);
        }

        @Override
        public void onError(int error) {
            // Errors are normal (silence, network, etc.) — just restart
            int delay = (error == SpeechRecognizer.ERROR_NETWORK) ? 3000 : 500;
            restartAfterDelay(delay);
        }

        @Override public void onReadyForSpeech(Bundle params) {}
        @Override public void onBeginningOfSpeech() {}
        @Override public void onRmsChanged(float rmsdB) {}
        @Override public void onBufferReceived(byte[] buffer) {}
        @Override public void onEndOfSpeech() {}
        @Override public void onPartialResults(Bundle partialResults) {
            // Check partial results too for faster response
            ArrayList<String> partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (partial != null && !partial.isEmpty()) {
                String heard = partial.get(0).toLowerCase();
                if (heard.contains(wakeName.toLowerCase())) {
                    Log.d(TAG, "Wake word in partial: " + heard);
                }
            }
        }
        @Override public void onEvent(int eventType, Bundle params) {}
        @Override public void onSegmentResults(Bundle segmentResults) {}
        @Override public void onEndOfSegmentedSession() {}
    }

    // ============================================================
    // COMMAND LISTENER
    // ============================================================
    private class CommandListener implements RecognitionListener {

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String command = matches.get(0);
                Log.d(TAG, "Command heard: " + command);
                processFullCommand(wakeName + " " + command);
            } else {
                updateNotification("Listening for \"" + wakeName + "\"...");
                restartAfterDelay(RESTART_DELAY);
            }
        }

        @Override
        public void onError(int error) {
            updateNotification("Listening for \"" + wakeName + "\"...");
            restartAfterDelay(500);
        }

        @Override public void onReadyForSpeech(Bundle params) {}
        @Override public void onBeginningOfSpeech() {}
        @Override public void onRmsChanged(float rmsdB) {}
        @Override public void onBufferReceived(byte[] buffer) {}
        @Override public void onEndOfSpeech() {}
        @Override public void onPartialResults(Bundle partialResults) {}
        @Override public void onEvent(int eventType, Bundle params) {}
        @Override public void onSegmentResults(Bundle segmentResults) {}
        @Override public void onEndOfSegmentedSession() {}
    }

    // ============================================================
    // PROCESS THE FULL VOICE COMMAND
    // ============================================================
    private void processFullCommand(String fullText) {
        updateNotification("Processing: " + fullText);

        String result = CommandProcessor.process(
            JinaListenerService.this,
            fullText,
            wakeName
        );

        updateNotification("Listening for \"" + wakeName + "\"...");

        if (MainActivity.instance != null) {
            MainActivity.instance.updateStatus("✅ Done — say \"" + wakeName + "\" for next command");
            MainActivity.instance.logCommand(fullText, result);
        }

        restartAfterDelay(1500);
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private void restartAfterDelay(int delay) {
        handler.postDelayed(() -> startListeningForWakeWord(), delay);
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_ID, buildNotification(text));
        }
    }

    private Notification buildNotification(String text) {
        Intent notifIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
            this, 0, notifIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JINA")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "JINA Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("JINA is listening for your voice");
            channel.setSound(null, null);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "JINA::WakeLock"
            );
            wakeLock.acquire();
        }
    }

    private void vibratePhone() {
        try {
            android.os.Vibrator v = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                v.vibrate(150);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        shouldRestart = false;
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        // Restart service if killed
        sendBroadcast(new Intent("com.jina.voiceassistant.RESTART"));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
