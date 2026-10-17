package com.animekita;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Menerima push notification FCM. Saat token FCM berubah, token baru otomatis
 * disimpan ke Firestore dan topik anime terbaru disubscribe ulang.
 */
public class AnimeFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        String title = null;
        String body = null;

        if (message.getNotification() != null) {
            title = message.getNotification().getTitle();
            body = message.getNotification().getBody();
        }

        Map<String, String> data = message.getData();
        if (title == null && data.containsKey("title")) {
            title = data.get("title");
        }
        if (body == null) {
            if (data.containsKey("body")) {
                body = data.get("body");
            } else if (data.containsKey("message")) {
                body = data.get("message");
            }
        }
        if (title == null) {
            title = getString(R.string.app_name);
        }
        if (body == null) {
            body = getString(R.string.push_default_body);
        }

        NotificationHelper.showMessageNotification(this, title, body);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        SubscriberManager.onNewToken(this, token);
    }
}
