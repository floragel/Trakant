package com.example.trakant

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

// Constantes
const val CHANNEL_ID = "trakant_daily_reminders"
const val CHANNEL_NAME = "Rappels Quotidiens TrakAnt"
const val NOTIFICATION_ID = 1001 // ID du rappel quotidien

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 1. Charger l'utilisateur pour vérifier si les notifications sont activées
        // C'est l'ancienne logique demandée.
        val userData = UserManager.loadUser(context)
        if (!userData.notificationsEnabled) {
            return // Ne rien faire si désactivé dans les paramètres
        }

        showNotification(context)
    }

    private fun showNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // Assurez-vous que l'icône est valide. Utiliser un drawable système simple pour garantir que ça marche.
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle("TrakAnt Colony Needs You!")
            .setContentText("Your ant colony needs to grow. Log some of your accomplishments.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            // Vérification permission Android 13+
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || 
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: Exception) {
            // Gérer les exceptions potentielles
        }
    }
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = CHANNEL_NAME
        val descriptionText = "Reminders to log your habits"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

fun scheduleDailyNotifications(context: Context) {
    scheduleAlarm(context, 8, 0, 100)  // Matin 8h00
    scheduleAlarm(context, 12, 0, 101) // Midi 12h00
    scheduleAlarm(context, 20, 0, 102) // Soir 20h00
}

fun cancelAllNotifications(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, NotificationReceiver::class.java)
    
    for (id in listOf(100, 101, 102)) {
        val pendingIntent = PendingIntent.getBroadcast(
            context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}

private fun scheduleAlarm(context: Context, hour: Int, minute: Int, requestCode: Int) {
    // Vérifier permission Exact Alarm pour Android 12+ (API 31+)
    // Ici on utilise setRepeating qui ne nécessite pas SCHEDULE_EXACT_ALARM, 
    // mais RTC_WAKEUP peut être inexact sur les versions récentes (ce qui est OK pour des notifs générales).
    // Si on voulait EXACT, il faudrait check canScheduleExactAlarms().
    
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, NotificationReceiver::class.java)
    
    // Utiliser FLAG_UPDATE_CURRENT pour s'assurer que l'intent est bien mis à jour
    val pendingIntent = PendingIntent.getBroadcast(
        context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }

    // Si l'heure est déjà passée aujourd'hui, on programme pour demain
    if (calendar.timeInMillis <= System.currentTimeMillis()) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    // Utilisation de setInexactRepeating pour économiser la batterie, suffisant pour des rappels "Matin/Midi/Soir"
    alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pendingIntent
    )
}

// ===========================================
// FONCTION DE TEST IMMÉDIAT
// ===========================================

fun sendTestNotification(context: Context) {
    // S'assurer que le canal existe
    createNotificationChannel(context)

    // Intent pour ouvrir MainActivity en cliquant sur la notification
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent: PendingIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    // Construction de la notification de test
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Notification de Test TrakAnt 🐜")
        .setContentText("Ceci est une notification de test immédiate. ✅")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    // Affichage : on utilise NOTIFICATION_ID + 1 pour ne pas écraser le rappel quotidien
    with(NotificationManagerCompat.from(context)) {
        notify(NOTIFICATION_ID + 1, builder.build())
    }
}
