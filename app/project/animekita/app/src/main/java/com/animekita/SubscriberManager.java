package com.animekita;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.HashMap;
import java.util.Map;

public final class SubscriberManager {

    private static final String TAG = "SubscriberManager";
    private static final String PREFS = "anime_subscription";
    private static final String KEY_SUBSCRIBED = "subscribed";

    /** Topik FCM yang dipakai untuk push "anime terbaru". */
    public static final String TOPIC_ANIME_TERBARU = "anime_terbaru";

    private SubscriberManager() {
    }

    public static boolean isSubscribed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SUBSCRIBED, false);
    }

    /**
     * Simpan subscriber ke Firestore (collection "subscribers", dokumen = token FCM) dan subscribe ke topik push anime terbaru. Dipanggil setelah izin notifikasi diberikan.
     */
    public static void subscribe(Context context) {
        if (isSubscribed(context)) {
            return;
        }
        FirebaseApp.initializeApp(context); // aman jika sudah diinisialisasi provider
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            saveSubscriber(context, token);
            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_ANIME_TERBARU)
                    .addOnSuccessListener(
                            aVoid -> Log.i(TAG, "subscribed to " + TOPIC_ANIME_TERBARU))
                    .addOnFailureListener(e -> Log.w(TAG, "subscribeToTopic failed", e));
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putBoolean(KEY_SUBSCRIBED, true).apply();
        }).addOnFailureListener(e -> Log.w(TAG, "getToken failed", e));
    }

    /** Token FCM berubah (refresh) — perbarui Firestore dan pastikan tetap subscribe topik. */
    public static void onNewToken(Context context, String token) {
        FirebaseApp.initializeApp(context);
        saveSubscriber(context, token);
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_ANIME_TERBARU)
                .addOnSuccessListener(aVoid -> Log.i(TAG, "resubscribed to " + TOPIC_ANIME_TERBARU))
                .addOnFailureListener(e -> Log.w(TAG, "subscribeToTopic failed", e));
    }

    private static void saveSubscriber(Context context, String token) {
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("device", Build.MANUFACTURER + " " + Build.MODEL);
        data.put("androidVersion", Build.VERSION.RELEASE);
        data.put("subscribedAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("subscribers").document(token).set(data)
                .addOnSuccessListener(aVoid -> Log.i(TAG, "subscriber saved to Firestore"))
                .addOnFailureListener(e -> Log.w(TAG, "save subscriber failed", e));
    }
}
