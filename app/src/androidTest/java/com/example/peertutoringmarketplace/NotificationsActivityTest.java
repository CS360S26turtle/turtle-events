package com.example.peertutoringmarketplace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class NotificationsActivityTest {

    private FirebaseFirestore db;
    private Context context;

    private String testTutorProfileId;
    private String testStudentProfileId;
    private String testSlotId;
    private String testSessionId;
    private String testSlotId2;
    private String testSessionId2;

    @Before
    public void setUp() {
        db      = FirebaseFirestore.getInstance();
        context = ApplicationProvider.getApplicationContext();

        long now             = System.currentTimeMillis();
        testTutorProfileId   = "TEST_TUTOR_"    + now;
        testStudentProfileId = "TEST_STUDENT_"  + now;
        testSlotId           = "TEST_SLOT_"     + now;
        testSessionId        = "TEST_SESSION_"  + now;
        testSlotId2          = "TEST_SLOT2_"    + now;
        testSessionId2       = "TEST_SESSION2_" + now;
    }

    @After
    public void tearDown() throws Exception {
        db.collection("sessions").document(testSessionId).delete();
        db.collection("sessions").document(testSessionId2).delete();
        db.collection("slots").document(testSlotId).delete();
        db.collection("slots").document(testSlotId2).delete();
        cancelTestAlarm(testSessionId);
        cancelTestAlarm(testSessionId2);
        Thread.sleep(400);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Date hoursFromNow(int hours) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR_OF_DAY, hours);
        return c.getTime();
    }

    private Date hoursAgo(int hours) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR_OF_DAY, -hours);
        return c.getTime();
    }

    private boolean writeSlot(String slotId, Date start, Date end) throws Exception {
        Map<String, Object> slot = new HashMap<>();
        slot.put("tutorId",   testTutorProfileId);
        slot.put("startTime", new Timestamp(start));
        slot.put("endTime",   new Timestamp(end));
        slot.put("isBooked",  true);
        CountDownLatch latch = new CountDownLatch(1);
        db.collection("slots").document(slotId).set(slot)
                .addOnCompleteListener(t -> latch.countDown());
        return latch.await(10, TimeUnit.SECONDS);
    }

    private boolean writeSession(String sessionId, String slotId,
                                 String tutorId, List<String> studentsId) throws Exception {
        Map<String, Object> session = new HashMap<>();
        session.put("timeSlotId", slotId);
        session.put("tutorId",    tutorId);
        session.put("studentsId", studentsId);
        session.put("status",     "confirmed");
        CountDownLatch latch = new CountDownLatch(1);
        db.collection("sessions").document(sessionId).set(session)
                .addOnCompleteListener(t -> latch.countDown());
        return latch.await(10, TimeUnit.SECONDS);
    }

    private void cancelTestAlarm(String sessionId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = PendingIntent.getBroadcast(
                context, sessionId.hashCode(),
                new Intent(context, SessionReminderReceiver.class),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) { am.cancel(pi); pi.cancel(); }
    }

    // ════════════════════════════════════════════════════════════════════════
    // IN-APP NOTIFICATIONS (Firestore)
    // ════════════════════════════════════════════════════════════════════════

    /** Student query returns their booked session */
    @Test
    public void testStudentQueryReturnsTheirSession() throws Exception {
        assertTrue(writeSlot(testSlotId, hoursFromNow(2), hoursFromNow(3)));
        List<String> students = new ArrayList<>();
        students.add(testStudentProfileId);
        assertTrue(writeSession(testSessionId, testSlotId, testTutorProfileId, students));
        Thread.sleep(500);

        CountDownLatch latch = new CountDownLatch(1);
        final int[] count = {0};

        db.collection("sessions")
                .whereArrayContains("studentsId", testStudentProfileId)
                .get()
                .addOnSuccessListener(snap -> {
                    for (var doc : snap.getDocuments())
                        if (doc.getId().equals(testSessionId)) count[0]++;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        latch.await(10, TimeUnit.SECONDS);
        assertEquals("Query must return the student's session", 1, count[0]);
    }


    /** Student must not see sessions belonging to a different student */
    @Test
    public void testStudentDoesNotSeeOtherStudentsSessions() throws Exception {
        String otherStudent = "OTHER_" + System.currentTimeMillis();
        assertTrue(writeSlot(testSlotId, hoursFromNow(2), hoursFromNow(3)));
        List<String> others = new ArrayList<>();
        others.add(otherStudent);
        assertTrue(writeSession(testSessionId, testSlotId, testTutorProfileId, others));
        Thread.sleep(500);

        CountDownLatch latch = new CountDownLatch(1);
        final int[] count = {0};

        db.collection("sessions")
                .whereArrayContains("studentsId", testStudentProfileId)
                .get()
                .addOnSuccessListener(snap -> {
                    for (var doc : snap.getDocuments())
                        if (doc.getId().equals(testSessionId)) count[0]++;
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        latch.await(10, TimeUnit.SECONDS);
        assertEquals("Student must not see another student's session", 0, count[0]);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PHONE NOTIFICATIONS (ReminderScheduler + SessionReminderReceiver)
    // ════════════════════════════════════════════════════════════════════════

    /** Notification channel is created with high importance when receiver fires */
    @Test
    public void testNotificationChannelCreatedWithHighImportance() {
        new SessionReminderReceiver().onReceive(context,
                new Intent(context, SessionReminderReceiver.class));

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        assertNotNull(nm);
        android.app.NotificationChannel ch =
                nm.getNotificationChannel(SessionReminderReceiver.CHANNEL_ID);
        assertNotNull("Channel must exist", ch);
        assertEquals(NotificationManager.IMPORTANCE_HIGH, ch.getImportance());
    }

    /** Alarm is registered for a session starting in 3 hours */
    @Test
    public void testAlarmRegisteredForFutureSession() {
        ReminderScheduler.scheduleSessionReminder(context, testSessionId, hoursFromNow(3));

        PendingIntent pi = PendingIntent.getBroadcast(
                context, testSessionId.hashCode(),
                new Intent(context, SessionReminderReceiver.class),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);

        assertNotNull("Alarm must be registered for a future session", pi);
    }

    /** No alarm for a session that already started */
    @Test
    public void testNoAlarmForPastSession() {
        ReminderScheduler.scheduleSessionReminder(context, testSessionId, hoursAgo(1));

        PendingIntent pi = PendingIntent.getBroadcast(
                context, testSessionId.hashCode(),
                new Intent(context, SessionReminderReceiver.class),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);

        assertNull("No alarm should be registered for a past session", pi);
    }

    @Test
    public void testReceiverHandlesMissingExtrasWithoutCrashing() {
        try {
            new SessionReminderReceiver().onReceive(context,
                    new Intent(context, SessionReminderReceiver.class));
        } catch (Exception e) {
            assertTrue("Receiver must not throw with missing extras: " + e.getMessage(), false);
        }
    }
}