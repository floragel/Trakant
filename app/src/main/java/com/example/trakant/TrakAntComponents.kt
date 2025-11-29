package com.example.trakant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

// --- COMPOSANTS UI PRESERVÉS ---

@Composable
fun LevelHeader(level: Int, xp: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TrakLevelBar),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Level $level", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(text = "$xp XP", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun MainStatsCard(userData: UserData) {
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
                // Stats factices (visuelles)
                HabitWithProgress("Fitness", 0.7f, TrakLevelBar)
                HabitWithProgress("Study", 0.4f, TrakRed)
                HabitWithProgress("Sleep", 0.9f, TrakPurple)
                HabitWithProgress("Book", 0.2f, TrakYellow)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AntPlanet(Modifier.fillMaxWidth().aspectRatio(1f))
                Spacer(Modifier.height(8.dp))
                Text("Colony: ${userData.colonySize} ants", color = TrakTextDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun HabitWithProgress(title: String, progress: Float, color: Color) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = TrakTextDark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFFE0E5C9))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(999.dp)).background(color))
        }
    }
}

@Composable
fun AntPlanet(modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape).background(TrakPlanetGrass)) {
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(40.dp).background(TrakPlanetSoil))

        // Fourmis (design pixel art abstrait)
        val antColor = Color(0xFF1C1C1C)
        val size = 8.dp

        @Composable fun Ant(x: Dp, y: Dp, align: Alignment) {
            Box(Modifier.align(align).offset(x, y).size(size).clip(CircleShape).background(antColor))
        }

        Ant(12.dp, 12.dp, Alignment.TopStart)
        Ant(0.dp, 10.dp, Alignment.TopCenter)
        Ant((-12).dp, 16.dp, Alignment.TopEnd)
        Ant(0.dp, 0.dp, Alignment.Center)
        Ant(8.dp, 20.dp, Alignment.CenterStart)
        Ant((-6).dp, (-4).dp, Alignment.CenterEnd)
        Ant(0.dp, (-12).dp, Alignment.BottomCenter)
    }
}

@Composable
fun BadgesGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BadgeChip("Streak 7 days", TrakRed, Modifier.weight(1f))
            BadgeChip("Bookworm", TrakBlue, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BadgeChip("Gym Rat", TrakYellow, Modifier.weight(1f))
            BadgeChip("Sleep Master", TrakPurple, Modifier.weight(1f))
        }
    }
}

@Composable
fun BadgeChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(14.dp), modifier = modifier.height(48.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            Text(label, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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