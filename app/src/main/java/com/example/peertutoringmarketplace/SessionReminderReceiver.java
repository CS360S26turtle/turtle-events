package com.example.peertutoringmarketplace;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
/**
 * BroadcastReceiver that fires when a session reminder alarm triggers.
 */
public class SessionReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "session_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        android.util.Log.d("SessionReminder", "onReceive triggered");
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        int notificationId = intent.getIntExtra("notificationId", (int) System.currentTimeMillis());

        android.util.Log.d("SessionReminder", "Title: " + title + ", Message: " + message);

        createNotificationChannel(context);

        Intent targetIntent = new Intent(context, LoginActivity.class);
        targetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, notificationId, targetIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title != null ? title : "Upcoming Session Reminder")
                .setContentText(message != null ? message : "Your session starts in 1 hour.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(notificationId, builder.build());
            }
        } else {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        }

        // Also broadcast an in-app event
        Intent inAppIntent = new Intent("com.example.peertutoringmarketplace.SESSION_REMINDER");
        inAppIntent.putExtra("title", title);
        inAppIntent.putExtra("message", message);
        context.sendBroadcast(inAppIntent);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Session Reminders";
            String description = "Reminders for upcoming peer tutoring sessions";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
