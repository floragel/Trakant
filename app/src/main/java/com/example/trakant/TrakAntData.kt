package com.example.trakant

import android.content.Context
import androidx.room.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.util.UUID

// ==========================================
// 1. DÉFINITIONS ET ENUMS
// ==========================================

enum class QuestType(val title: String, val baseXp: Int, val colorHex: Long) {
    SPORT("Faire du sport", 20, 0xFF4B9C6A),
    HYDRATION("S'hydrater", 5, 0xFF4AA7C9),
    EAT_HEALTHY("Manger sainement", 15, 0xFFF6C453),
    SLEEP_WELL("Bien dormir", 15, 0xFFB39DDB),
    READING("Lire un livre", 10, 0xFFF28B82),
    NO_SOCIAL_MEDIA("Pas de réseaux sociaux", 25, 0xFF8B5A3C)
}

// ==========================================
// 2. ROOM DATABASE
// ==========================================

@Entity(tableName = "quests")
data class Quest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: QuestType,
    var isCompleted: Boolean = false,
    val note: String? = null
)

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests ORDER BY id DESC")
    fun getAllQuests(): Flow<List<Quest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: Quest)

    @Update
    suspend fun updateQuest(quest: Quest)

    @Delete
    suspend fun deleteQuest(quest: Quest)
}

class QuestTypeConverter {
    @TypeConverter fun fromQuestType(t: QuestType) = t.name
    @TypeConverter fun toQuestType(n: String) = QuestType.valueOf(n)
}

@Database(entities = [Quest::class], version = 1, exportSchema = false)
@TypeConverters(QuestTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questDao(): QuestDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "trakant_db").build().also { INSTANCE = it }
            }
        }
    }
}

// ==========================================
// 3. USER DATA (Profil + Logbook)
// ==========================================

data class HistoryEntry(val typeName: String, val date: String, val xpGained: Int)

data class UserData(
    val userId: String,
    var name: String,
    var age: Int = 18,              // Géré par Settings
    var notificationsEnabled: Boolean = true, // Géré par Settings et Notifications
    var xp: Int,
    var level: Int,
    var colonySize: Int,
    var history: MutableList<HistoryEntry> = mutableListOf() // Le Logbook
)

object UserManager {
    private const val PREFS_NAME = "trakant_user_prefs"
    private const val KEY_USER_DATA = "user_data_v3" // Clé pour la version avec Logbook

    fun loadUser(context: Context): UserData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_USER_DATA, null)
        return if (json != null) {
            try {
                Gson().fromJson(json, UserData::class.java)
            } catch (e: Exception) {
                // Fallback si la structure JSON change
                UserData(UUID.randomUUID().toString(), "AntKeeper", 18, true, 0, 1, 1)
            }
        } else {
            // Nouvel utilisateur par défaut
            UserData(UUID.randomUUID().toString(), "AntKeeper", 18, true, 0, 1, 1)
        }
    }

    fun saveUser(context: Context, user: UserData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_DATA, Gson().toJson(user)).apply()
    }

    // Mise à jour du profil (Settings)
    fun updateProfile(context: Context, user: UserData, name: String, age: Int, notif: Boolean): UserData {
        user.name = name
        user.age = age
        user.notificationsEnabled = notif
        saveUser(context, user)
        return user
    }

    // Gestion XP et Logbook
    fun addXp(context: Context, user: UserData, amount: Int, source: String, date: String): UserData {
        user.xp += amount
        user.history.add(HistoryEntry(source, date, amount))

        val newLevel = 1 + (user.xp / 100)
        if (newLevel > user.level) {
            user.level = newLevel
            user.colonySize = user.level
        }

        saveUser(context, user)
        return user
    }

    fun removeXp(context: Context, user: UserData, amount: Int): UserData {
        user.xp = (user.xp - amount).coerceAtLeast(0)
        user.level = 1 + (user.xp / 100)
        user.colonySize = user.level
        // Optionnel : on pourrait retirer la dernière entrée de l'historique ici si on annule une action
        if (user.history.isNotEmpty()) {
            // user.history.removeAt(user.history.lastIndex)
        }
        saveUser(context, user)
        return user
    }
}