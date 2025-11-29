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
        // Intent pour ouvrir MainActivity en cliquant sur la notification
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Assurez-vous d'avoir une ressource ic_notification dans res/drawable
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Remplacer par R.drawable.ic_notification si vous l'avez
            .setContentTitle("N'oublie pas ta colonie !")
            .setContentText("Ta colonie de fourmie a besoin de grandire. Rajoute des accomplissements pour l'aider")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }
}

// Fonction utilitaire pour créer le canal de notification
fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = "Rappels quotidiens pour compléter les quêtes."
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

// Fonction pour planifier la notification (ex: tous les jours à 19h30)
fun scheduleDailyNotifications(context: Context) {
    // Annule les anciennes planifications avant d'en créer une nouvelle
    cancelDailyNotifications(context)

    // Planifié pour 19h30
    scheduleAlarm(context, hour = 19, minute = 30, requestCode = 100)
}

// Fonction pour annuler la notification
fun cancelDailyNotifications(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, NotificationReceiver::class.java)

    val pendingIntent = PendingIntent.getBroadcast(
        context, 100, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
    )
    if (pendingIntent != null) {
        alarmManager.cancel(pendingIntent)
    }
}

// Logique interne pour la planification
private fun scheduleAlarm(context: Context, hour: Int, minute: Int, requestCode: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, NotificationReceiver::class.java)
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

    // Utilisation de setInexactRepeating pour les rappels quotidiens
    alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pendingIntent
    )
}

// ===========================================
// NOUVEAU: FONCTION DE TEST IMMÉDIAT
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
        .setContentTitle("N'oublie pas ta colonie !")
        .setContentText("Ta colonie de fourmie a besoin de grandire. Rajoute des accomplissements pour l'aider")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    // Affichage : on utilise NOTIFICATION_ID + 1 pour ne pas écraser le rappel quotidien
    with(NotificationManagerCompat.from(context)) {
        notify(NOTIFICATION_ID + 1, builder.build())
    }
}