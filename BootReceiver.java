package com.jina.voiceassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // Start JINA when phone boots
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action) ||
            "com.jina.voiceassistant.RESTART".equals(action)) {

            SharedPreferences prefs = context.getSharedPreferences("jina_prefs", Context.MODE_PRIVATE);
            String wakeName = prefs.getString("wake_name", "");

            if (!wakeName.isEmpty()) {
                Log.d("JINA_BOOT", "Starting JINA service on boot for wake name: " + wakeName);
                Intent serviceIntent = new Intent(context, JinaListenerService.class);
                serviceIntent.putExtra("wake_name", wakeName);
                context.startForegroundService(serviceIntent);
            }
        }
    }
}
