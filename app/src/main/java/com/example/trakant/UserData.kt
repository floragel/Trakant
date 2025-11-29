package com.example.trakant

import android.content.Context
import com.google.gson.Gson

data class CompletedQuest(
    val questId: String,
    val date: String // format "YYYY-MM-DD"
)

data class UserSettings(
    val biome: String = "forest",
    val notificationsEnabled: Boolean = true
)

data class UserData(
    val userId: String,
    var name: String,
    var age: Int = 18, // Ajout du champ Age
    var xp: Int,
    var level: Int,
    var colonySize: Int,
    val completedQuests: MutableList<CompletedQuest>,
    val badgesUnlocked: MutableList<String>,
    var settings: UserSettings
)

private const val PREFS_NAME = "trakant_prefs"

private fun userDataKey(userId: String) = "user_data_$userId"

fun loadUserData(context: Context, userId: String): UserData {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val key = userDataKey(userId)
    val json = prefs.getString(key, null)

    return if (json.isNullOrBlank()) {
        // si pas trouvé, on crée un user par défaut
        UserData(
            userId = userId,
            name = "Ant",
            age = 18,
            xp = 0,
            level = 1,
            colonySize = 1,
            completedQuests = mutableListOf(),
            badgesUnlocked = mutableListOf(),
            settings = UserSettings()
        )
    } else {
        Gson().fromJson(json, UserData::class.java)
    }
}

fun saveUserData(context: Context, userData: UserData) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val key = userDataKey(userData.userId)
    val json = Gson().toJson(userData)
    prefs.edit().putString(key, json).apply()
}
