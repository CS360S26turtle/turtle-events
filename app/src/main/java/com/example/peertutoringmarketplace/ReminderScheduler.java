package com.example.peertutoringmarketplace;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Date;
/**
 * Schedules a one-time AlarmManager alarm t1 hour before a given
 * session's start time, triggering {@link SessionReminderReceiver} to post
 * a phone notification. Alarms are skipped if the reminder time has already passed.
 */
public class ReminderScheduler {


    public static void scheduleSessionReminder(Context context, String sessionId, Date startTime) {
        if (context == null || startTime == null) return;

        long oneHourInMillis = 60 * 60 * 1000;
        long reminderTimeMs = startTime.getTime() - oneHourInMillis;

        if (reminderTimeMs < System.currentTimeMillis()) {
            android.util.Log.d("SessionReminder", "Reminder time already passed for session: " + sessionId);
            return;
        }

        android.util.Log.d("SessionReminder", "Scheduling reminder for session: " + sessionId + " at " + new java.util.Date(reminderTimeMs).toString());

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, SessionReminderReceiver.class);
        intent.putExtra("title", "Upcoming Session Reminder");
        intent.putExtra("message", "Your session starts in 1 hour.");
        
        int requestCode = sessionId.hashCode();
        intent.putExtra("notificationId", requestCode);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                requestCode, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMs, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMs, pendingIntent);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMs, pendingIntent);
        }
    }
}
