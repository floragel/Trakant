package com.example.trakant

import android.content.Context
import android.os.Build
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// --- XP REQUIRED FOR LEVEL (doublé à chaque level) ---
fun xpRequiredForLevel(level: Int): Int {
    if (level <= 1) return 10
    return 10 * (1 shl (level - 1)) // 10, 20, 40, 80…
}

// --- CALCUL LEVEL FROM XP ---
fun levelFromXp(totalXp: Int): Int {
    var level = 1
    var xpRemaining = totalXp

    while (xpRemaining >= xpRequiredForLevel(level)) {
        xpRemaining -= xpRequiredForLevel(level)
        level++
    }

    return level
}

// --- COLONY SIZE = LEVEL ---
fun colonySizeFromLevel(level: Int): Int {
    return level // simple équation : Level = nombre de fourmis
}

// --- CALCUL XP GAGNÉ POUR UNE QUÊTE ---
fun calculateXpForQuest(
    quest: Quest,
    mission: Mission,
    streak: Int
): Int {
    // base XP
    var xp = quest.baseXp

    // coefficient mission (fitness/study = 2, sleep/book = 1)
    xp *= mission.coef

    // streak bonus (+10% par jour)
    xp += (xp * (streak * 0.10)).toInt()

    return xp
}

// --- FINISH QUEST : MISE À JOUR DE L'UTILISATEUR ---
fun completeQuest(
    context: Context,
    userData: UserData,
    quest: Quest,
    mission: Mission
): UserData {

    // enregistrer la quête terminée
    val todayString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        LocalDate.now().toString()
    } else {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.format(Calendar.getInstance().time)
    }

    userData.completedQuests.add(
        CompletedQuest(
            questId = quest.id,
            date = todayString
        )
    )

    // streak logique basique
    val streak = userData.completedQuests.count { it.questId == quest.id }

    // gagner XP
    val gainedXp = calculateXpForQuest(quest, mission, streak)
    userData.xp += gainedXp

    // recalculer level
    val newLevel = levelFromXp(userData.xp)
    userData.level = newLevel

    // recalculer colony size
    userData.colonySize = colonySizeFromLevel(newLevel)

    // sauvegarder
    saveUserData(context, userData)

    return userData
}
