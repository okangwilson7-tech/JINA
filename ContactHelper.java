package com.jina.voiceassistant;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class ContactHelper {

    private static final String TAG = "JINA_CONTACTS";

    // Simple contact data model
    public static class Contact {
        public String name;
        public String phone;

        public Contact(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }
    }

    /**
     * Get all contacts from the phone
     */
    public static List<Contact> getAllContacts(Context context) {
        List<Contact> contacts = new ArrayList<>();

        try {
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    String phone = cursor.getString(1);
                    if (name != null && phone != null && !name.isEmpty() && !phone.isEmpty()) {
                        contacts.add(new Contact(name.trim(), phone.trim()));
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading contacts: " + e.getMessage());
        }

        Log.d(TAG, "Loaded " + contacts.size() + " contacts");
        return contacts;
    }

    /**
     * Find a contact by name using fuzzy matching.
     * This handles cases like:
     * - User says "Rafiki" but contact saved as "Rafiki Okello"
     * - User says "boss" but contact saved as "Boss Peter"
     * - User says "mama" but contact saved as "Mama Janet"
     */
    public static Contact findContact(Context context, String spokenName) {
        if (spokenName == null || spokenName.isEmpty()) return null;

        List<Contact> allContacts = getAllContacts(context);
        String spoken = spokenName.toLowerCase().trim();

        Contact bestMatch = null;
        int bestScore = 0;

        for (Contact contact : allContacts) {
            String contactLower = contact.name.toLowerCase().trim();
            int score = calculateMatchScore(spoken, contactLower);

            if (score > bestScore) {
                bestScore = score;
                bestMatch = contact;
            }
        }

        // Only return if score is good enough (above threshold)
        if (bestScore >= 60) {
            Log.d(TAG, "Best match: " + bestMatch.name + " (score: " + bestScore + ")");
            return bestMatch;
        }

        Log.d(TAG, "No good match for: " + spokenName);
        return null;
    }

    /**
     * Calculate how well the spoken name matches a contact name.
     * Returns 0-100 score. Higher = better match.
     */
    private static int calculateMatchScore(String spoken, String contactName) {
        // Exact match
        if (spoken.equals(contactName)) return 100;

        // Contact name contains spoken name
        if (contactName.contains(spoken)) return 90;

        // Spoken name contains contact name
        if (spoken.contains(contactName)) return 85;

        // First word of contact name matches spoken
        String[] contactParts = contactName.split("\\s+");
        String[] spokenParts = spoken.split("\\s+");

        // First word match
        if (contactParts.length > 0 && spokenParts.length > 0) {
            if (contactParts[0].equals(spokenParts[0])) return 80;
        }

        // Any word in contact matches any word in spoken
        for (String cp : contactParts) {
            for (String sp : spokenParts) {
                if (cp.equals(sp) && cp.length() > 2) return 70;
            }
        }

        // Starts-with match
        for (String cp : contactParts) {
            for (String sp : spokenParts) {
                if (cp.startsWith(sp) || sp.startsWith(cp)) {
                    if (sp.length() >= 3) return 65;
                }
            }
        }

        // Levenshtein distance for typos/accents
        for (String cp : contactParts) {
            for (String sp : spokenParts) {
                if (sp.length() >= 3 && cp.length() >= 3) {
                    int dist = levenshtein(cp, sp);
                    int maxLen = Math.max(cp.length(), sp.length());
                    int similarity = (int) ((1.0 - (double) dist / maxLen) * 100);
                    if (similarity >= 75) return similarity;
                }
            }
        }

        return 0;
    }

    /**
     * Levenshtein distance — measures how different two strings are.
     * Lower = more similar. Used for fuzzy matching.
     */
    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[a.length()][b.length()];
    }
}
