package com.example.trakant

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- PALETTE DE COULEURS ORIGINALE ---
val TrakBackground = Color(0xFFCDECC4)   // vert pastel fond
val TrakLevelBar   = Color(0xFF4B9C6A)   // vert plus foncé
val TrakCreamCard  = Color(0xFFF7F0D9)   // carte beige
val TrakPlanetGrass = Color(0xFF99D46D)  // herbe planète
val TrakPlanetSoil  = Color(0xFFB77746)  // terre planète
val TrakSoilStrip   = Color(0xFF8B5A3C)  // bande de sol
val TrakTextDark    = Color(0xFF233221)

val TrakBlue   = Color(0xFF4AA7C9)
val TrakRed    = Color(0xFFF28B82)
val TrakYellow = Color(0xFFF6C453)
val TrakPurple = Color(0xFFB39DDB)

// --- COMPOSANTS UI MODIFIÉS POUR PROGRESSION DYNAMIQUE ---

/**
 * LevelHeader : affiche Level + XP textuel ET une barre de progression
 * progress = xp % 100 / 100f  (selon la logique actuelle de niveau : level = 1 + xp/100)
 * on utilise animateFloatAsState pour une transition fluide.
 */
@Composable
fun LevelHeader(level: Int, xp: Int) {
    val xpIntoLevel = xp % 100
    val progressRaw = xpIntoLevel / 100f
    val animatedProgress by animateFloatAsState(targetValue = progressRaw)

    Card(
        colors = CardDefaults.cardColors(containerColor = TrakLevelBar),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(72.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Level $level", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "$xp XP", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            // Barre de progression vers le prochain niveau
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x33FFFFFF)) // légère piste
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                )
            }
        }
    }
}

/**
 * MainStatsCard : calcule dynamique pour chaque "habit" le progress sur 7 jours
 * On mappe chaque label à un QuestType (ajuste si tu veux d'autres correspondances).
 *
 * Logique : progress = min(countInWindow / windowDays, 1f)
 * where countInWindow = nombre d'entrées matching type dans les derniers 7 jours.
 */
@Composable
fun MainStatsCard(userData: UserData) {
    // Helper pour compter occurrences d'un QuestType dans la fenêtre passée (par défaut 7 jours)
    fun calcProgressForQuestType(type: QuestType, windowDays: Int = 7): Float {
        if (userData.history.isEmpty()) return 0f
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.DAY_OF_YEAR, - (windowDays - 1)) // window inclusive
        val windowStart = cal.time

        val count = userData.history.count { entry ->
            try {
                val d = sdf.parse(entry.date)
                d != null && !d.before(windowStart) && entry.typeName == type.name
            } catch (e: Exception) {
                false
            }
        }
        // On considère 1 action par jour comme l'objectif → progress ∈ [0,1]
        return (count.toFloat() / windowDays.toFloat()).coerceIn(0f, 1f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = TrakCreamCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 230.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Mapping label -> QuestType
                HabitWithProgress("Fitness", calcProgressForQuestType(QuestType.SPORT), TrakLevelBar)
                HabitWithProgress("Hydratation", calcProgressForQuestType(QuestType.HYDRATION), TrakBlue)
                HabitWithProgress("Sommeil", calcProgressForQuestType(QuestType.SLEEP_WELL), TrakPurple)
                HabitWithProgress("Lecture", calcProgressForQuestType(QuestType.READING), TrakRed)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AntPlanet(Modifier.fillMaxWidth().aspectRatio(1f), userData.colonySize)
                Spacer(Modifier.height(8.dp))
                Text("Colony: ${userData.colonySize} ants", color = TrakTextDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * HabitWithProgress : affiche le titre + une barre de progression selon progress ∈ [0,1]
 * L'apparence est simple et claire.
 */
@Composable
fun HabitWithProgress(title: String, progress: Float, color: Color) {
    // animation légère pour fluidifier les mises à jour
    val animated by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f))

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = TrakTextDark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            val percent = (animated * 100).toInt()
            Text("$percent%", color = TrakTextDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFFE0E5C9))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(animated).clip(RoundedCornerShape(999.dp)).background(color))
        }
    }
}

@Composable
fun SoilStrip() {
    Box(
        modifier = Modifier.fillMaxWidth().height(40.dp)
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .background(TrakSoilStrip),
        contentAlignment = Alignment.Center
    ) {
        Text("Ant colony biome", color = Color(0xFFF5E3C7), fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}
