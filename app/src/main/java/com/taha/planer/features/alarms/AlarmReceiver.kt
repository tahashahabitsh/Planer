package com.taha.planer.features.alarms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.taha.planer.MainActivity
import com.taha.planer.R

private const val CHANNEL_ID = "planner_alarms_channel"
private const val CHANNEL_NAME = "یادآوری‌های برنامه‌ریز"

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "یادآوری"
        val message = intent.getStringExtra("message") ?: ""
        val notificationId = intent.getLongExtra("alarm_id", System.currentTimeMillis()).toInt()
        showAlarmNotification(context, title, message, notificationId)
    }
}

fun schedulePlannerAlarm(context: Context, alarm: PlannerAlarm) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("title", if (alarm.title.isNotBlank()) alarm.title else "یادآوری")
        putExtra("message", alarm.message)
        putExtra("alarm_id", alarm.id)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        alarm.id.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, alarm.hour)
        set(Calendar.MINUTE, alarm.minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    when (alarm.repeatType) {
        AlarmRepeatType.DAILY -> {
            am.setRepeating(
                android.app.AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                android.app.AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
        AlarmRepeatType.ONCE -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                am.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
}

fun cancelPlannerAlarm(context: Context, alarm: PlannerAlarm) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        alarm.id.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    am.cancel(pendingIntent)
}

private fun showAlarmNotification(
    context: Context,
    title: String,
    message: String,
    notificationId: Int
) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    val tapIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    val tapPendingIntent = PendingIntent.getActivity(
        context,
        notificationId,
        tapIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_small)
        .setContentTitle(title)
        .setContentText(if (message.isNotBlank()) message else "یادت نره :)")
        .setStyle(NotificationCompat.BigTextStyle().bigText(message.ifBlank { "یادت نره :)" }))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(tapPendingIntent)
        .setAutoCancel(true)

    manager.notify(notificationId, builder.build())
}
