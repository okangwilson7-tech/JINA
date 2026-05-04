package com.jina.voiceassistant;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

public class CommandProcessor {

    private static final String TAG = "JINA_CMD";

    // Action types
    private static final String ACTION_NORMAL_CALL = "NORMAL_CALL";
    private static final String ACTION_WHATSAPP_CALL = "WHATSAPP_CALL";
    private static final String ACTION_WHATSAPP_VIDEO = "WHATSAPP_VIDEO";
    private static final String ACTION_SMS = "SMS";
    private static final String ACTION_WHATSAPP_MSG = "WHATSAPP_MSG";

    /**
     * Main entry point — takes the full voice text and executes the right action.
     * Returns a result string for logging.
     */
    public static String process(Context context, String fullText, String wakeName) {
        Log.d(TAG, "Processing: " + fullText);

        String text = fullText.toLowerCase().trim();

        // Remove wake name from beginning if present
        String wakeNameLower = wakeName.toLowerCase();
        if (text.startsWith(wakeNameLower)) {
            text = text.substring(wakeNameLower.length()).trim();
        }
        // Also remove if wake name appears anywhere
        text = text.replace(wakeNameLower, "").trim();

        Log.d(TAG, "Command after stripping wake name: " + text);

        // Determine action type
        String action = detectAction(text);

        if (action == null) {
            speakToUser(context, "Sorry, I didn't understand that command.");
            return "Command not understood: " + fullText;
        }

        // Extract contact name from command
        String contactName = extractContactName(text, action);

        if (contactName == null || contactName.isEmpty()) {
            speakToUser(context, "Please say a contact name.");
            return "No contact name found in: " + text;
        }

        Log.d(TAG, "Looking for contact: " + contactName);

        // Find the contact in phone
        ContactHelper.Contact contact = ContactHelper.findContact(context, contactName);

        if (contact == null) {
            speakToUser(context, "I could not find " + contactName + " in your contacts.");
            return "Contact not found: " + contactName;
        }

        Log.d(TAG, "Found contact: " + contact.name + " - " + contact.phone);

        // Execute the action
        return executeAction(context, action, contact);
    }

    // ============================================================
    // DETECT WHAT ACTION TO DO
    // ============================================================
    private static String detectAction(String text) {

        // WhatsApp video call
        if ((text.contains("video") && text.contains("call")) ||
            (text.contains("video call")) ||
            (text.contains("whatsapp") && text.contains("video"))) {
            return ACTION_WHATSAPP_VIDEO;
        }

        // WhatsApp voice call
        if ((text.contains("whatsapp") && text.contains("call")) ||
            (text.contains("whatsapp call")) ||
            (text.contains("whatsapp voice"))) {
            return ACTION_WHATSAPP_CALL;
        }

        // WhatsApp message
        if ((text.contains("whatsapp") && (text.contains("message") || text.contains("msg") || text.contains("text"))) ||
            text.contains("whatsapp message")) {
            return ACTION_WHATSAPP_MSG;
        }

        // SMS
        if (text.contains("sms") ||
            (text.contains("send") && text.contains("message")) ||
            text.contains("text message")) {
            return ACTION_SMS;
        }

        // Normal call (most common — check last)
        if (text.contains("call") || text.contains("ring") || text.contains("phone")) {
            return ACTION_NORMAL_CALL;
        }

        return null;
    }

    // ============================================================
    // EXTRACT CONTACT NAME FROM COMMAND
    // ============================================================
    private static String extractContactName(String text, String action) {
        // Remove action keywords to isolate the contact name
        text = text
            .replace("video call", "")
            .replace("whatsapp call", "")
            .replace("whatsapp video", "")
            .replace("whatsapp message", "")
            .replace("whatsapp msg", "")
            .replace("whatsapp", "")
            .replace("video", "")
            .replace("normal call", "")
            .replace("send message to", "")
            .replace("send message", "")
            .replace("send sms to", "")
            .replace("send sms", "")
            .replace("text message to", "")
            .replace("text message", "")
            .replace("call back", "")
            .replace("call", "")
            .replace("sms", "")
            .replace("ring", "")
            .replace("phone", "")
            .replace(" to ", " ")
            .replace(" me ", " ")
            .replace(" on ", " ")
            .replace(" please", "")
            .trim();

        // Clean up multiple spaces
        text = text.replaceAll("\\s+", " ").trim();

        return text.length() > 0 ? text : null;
    }

    // ============================================================
    // EXECUTE THE ACTION
    // ============================================================
    private static String executeAction(Context context, String action, ContactHelper.Contact contact) {
        String cleanPhone = contact.phone.replaceAll("[^0-9+]", "");

        switch (action) {
            case ACTION_NORMAL_CALL:
                return makeNormalCall(context, contact, cleanPhone);

            case ACTION_WHATSAPP_CALL:
                return makeWhatsAppCall(context, contact, cleanPhone);

            case ACTION_WHATSAPP_VIDEO:
                return makeWhatsAppVideoCall(context, contact, cleanPhone);

            case ACTION_SMS:
                return openSMS(context, contact, cleanPhone);

            case ACTION_WHATSAPP_MSG:
                return openWhatsAppChat(context, contact, cleanPhone);

            default:
                return "Unknown action";
        }
    }

    // ============================================================
    // NORMAL PHONE CALL
    // ============================================================
    private static String makeNormalCall(Context context, ContactHelper.Contact contact, String phone) {
        try {
            speakToUser(context, "Calling " + contact.name);
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phone));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(callIntent);
            return "Normal call → " + contact.name;
        } catch (Exception e) {
            speakToUser(context, "Could not make the call. Check phone permissions.");
            return "Call failed: " + e.getMessage();
        }
    }

    // ============================================================
    // WHATSAPP VOICE CALL
    // ============================================================
    private static String makeWhatsAppCall(Context context, ContactHelper.Contact contact, String phone) {
        try {
            speakToUser(context, "WhatsApp calling " + contact.name);

            // Format number for WhatsApp (remove leading 0, add country code if needed)
            String waPhone = formatForWhatsApp(phone);

            // Try WhatsApp direct call intent
            Intent waIntent = new Intent(Intent.ACTION_VIEW);
            waIntent.setData(Uri.parse("https://wa.me/" + waPhone));
            waIntent.setPackage("com.whatsapp");
            waIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (isAppInstalled(context, "com.whatsapp")) {
                context.startActivity(waIntent);
                return "WhatsApp call → " + contact.name;
            } else {
                // Try WhatsApp Business
                waIntent.setPackage("com.whatsapp.w4b");
                if (isAppInstalled(context, "com.whatsapp.w4b")) {
                    context.startActivity(waIntent);
                    return "WhatsApp Business call → " + contact.name;
                } else {
                    speakToUser(context, "WhatsApp is not installed on this phone.");
                    return "WhatsApp not installed";
                }
            }
        } catch (Exception e) {
            // Fallback: open WhatsApp chat
            return openWhatsAppChat(context, contact, phone);
        }
    }

    // ============================================================
    // WHATSAPP VIDEO CALL
    // ============================================================
    private static String makeWhatsAppVideoCall(Context context, ContactHelper.Contact contact, String phone) {
        try {
            speakToUser(context, "Opening WhatsApp video call with " + contact.name);

            String waPhone = formatForWhatsApp(phone);

            // WhatsApp video call intent
            Intent waIntent = new Intent(Intent.ACTION_VIEW);
            waIntent.setData(Uri.parse("https://wa.me/" + waPhone));
            waIntent.setPackage("com.whatsapp");
            waIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (isAppInstalled(context, "com.whatsapp")) {
                context.startActivity(waIntent);
                return "WhatsApp video → " + contact.name;
            } else {
                speakToUser(context, "WhatsApp is not installed.");
                return "WhatsApp not installed";
            }
        } catch (Exception e) {
            return openWhatsAppChat(context, contact, phone);
        }
    }

    // ============================================================
    // OPEN WHATSAPP CHAT
    // ============================================================
    private static String openWhatsAppChat(Context context, ContactHelper.Contact contact, String phone) {
        try {
            String waPhone = formatForWhatsApp(phone);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://wa.me/" + waPhone));
            intent.setPackage("com.whatsapp");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "WhatsApp chat → " + contact.name;
        } catch (Exception e) {
            speakToUser(context, "Could not open WhatsApp.");
            return "WhatsApp error: " + e.getMessage();
        }
    }

    // ============================================================
    // SEND SMS
    // ============================================================
    private static String openSMS(Context context, ContactHelper.Contact contact, String phone) {
        try {
            speakToUser(context, "Opening SMS to " + contact.name);
            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
            smsIntent.setData(Uri.parse("smsto:" + phone));
            smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(smsIntent);
            return "SMS opened → " + contact.name;
        } catch (Exception e) {
            speakToUser(context, "Could not open SMS.");
            return "SMS error: " + e.getMessage();
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    // Format phone number for WhatsApp (needs country code)
    private static String formatForWhatsApp(String phone) {
        // Remove all non-digits except +
        phone = phone.replaceAll("[^0-9+]", "");

        // If starts with 0, replace with Uganda code +256
        if (phone.startsWith("0")) {
            phone = "256" + phone.substring(1);
        }
        // If starts with +, remove the +
        if (phone.startsWith("+")) {
            phone = phone.substring(1);
        }
        return phone;
    }

    private static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Show toast message on main thread
    private static void speakToUser(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() ->
            Toast.makeText(context, "JINA: " + message, Toast.LENGTH_SHORT).show()
        );
    }
}
