package com.example.trakant   // ⬅️ adapte ce package à ton projet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow

// ---------- COULEURS TRAKANT (pastel fourmilière) ----------

private val TrakBackground = Color(0xFFCDECC4)   // vert pastel fond
private val TrakLevelBar   = Color(0xFF4B9C6A)   // vert plus foncé
private val TrakCreamCard  = Color(0xFFF7F0D9)   // carte beige
private val TrakPlanetGrass = Color(0xFF99D46D)  // herbe planète
private val TrakTextDark    = Color(0xFF233221)

private val TrakBlue   = Color(0xFF4AA7C9)
private val TrakRed    = Color(0xFFF28B82)
private val TrakYellow = Color(0xFFF6C453)
private val TrakPurple = Color(0xFFB39DDB)

// ---------- ACTIVITÉ PRINCIPALE ----------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {        // tu peux remplacer par ton thème si tu en as un
                TrakAntHomeApp()
            }
        }
    }
}

// ---------- BOTTOM NAV ----------

enum class TrakTab(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    HOME("Home", Icons.Default.Home),
    QUESTS("Quests", Icons.Default.Check),
    GRAPHS("Graphs", Icons.Default.TrendingUp),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun TrakAntHomeApp() {
    var currentTab by rememberSaveable { mutableStateOf(TrakTab.HOME) }

    Scaffold(
        containerColor = TrakBackground,
        bottomBar = {
            TrakBottomBar(
                current = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(TrakBackground)
        ) {
            // Pour l'instant on n'affiche que la Home,
            // mais la bottom bar est prête pour la suite.
            TrakAntHomeScreen(userName = "Lumi")
        }
    }
}

@Composable
fun TrakBottomBar(
    current: TrakTab,
    onTabSelected: (TrakTab) -> Unit
) {
    NavigationBar(
        containerColor = TrakCreamCard,
        tonalElevation = 4.dp
    ) {
        TrakTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == current,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}

// ---------- HOME SCREEN : DESIGN PRINCIPAL ----------

@Composable
fun TrakAntHomeScreen(userName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // --- Bandeau de niveau ---
            LevelHeader(level = 12, xp = 430)

            Spacer(Modifier.height(16.dp))

            // --- Salut utilisateur ---
            Text(
                text = "Hi, $userName",
                color = TrakTextDark,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(16.dp))

            // --- Grande carte centrale ---
            MainStatsCard()
        }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(Modifier.height(16.dp))

            // --- Badges ---
            BadgesGrid()

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------- BLOCS UI ----------

@Composable
fun LevelHeader(level: Int, xp: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = TrakLevelBar
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Level $level",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$xp XP",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MainStatsCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = TrakCreamCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 230.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Colonne gauche : 4 paramètres avec barres ---
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HabitWithProgress(
                    title = "Fitness",
                    progress = 0.4f,
                    color = TrakLevelBar
                )
                HabitWithProgress(
                    title = "Study",
                    progress = 0.25f,
                    color = TrakRed
                )
                HabitWithProgress(
                    title = "Sleep",
                    progress = 0.6f,
                    color = TrakPurple
                )
                HabitWithProgress(
                    title = "Book",
                    progress = 0.3f,
                    color = TrakYellow
                )
            }

            // --- Droite : planète colonie ---
            AntPlanet(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
            )
        }
    }
}

@Composable
fun HabitWithProgress(
    title: String,
    progress: Float,
    color: Color
) {
    Column {
        Text(
            text = title,
            color = TrakTextDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        HabitProgressBar(progress = progress, color = color)
    }
}

@Composable
fun HabitProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFE0E5C9))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(999.dp))
                .background(color)
        )
    }
}

private fun randomPointInCircle(radius: Dp): Pair<Dp, Dp> {
    val r = radius.value * sqrt(Random.nextDouble())
    val theta = Random.nextDouble() * 2 * Math.PI

    val x = r * cos(theta)
    val y = r * sin(theta)

    return x.toFloat().dp to y.toFloat().dp
}

// Planète colonie en pixel-style simplifié (vert + sol + points fourmis)
@Composable
fun AntPlanet(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val planetRadius = maxWidth / 2
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(TrakPlanetGrass),
            contentAlignment = Alignment.Center
        ) {
            // petites "fourmis" pixel art
            val size = 16.dp
            for (i in 1..7) {
                PixelAnt(i, size, planetRadius)
            }
        }
    }
}

// simple helper extension to align with offset in AntPlanet
@Composable
private fun BoxScope.PixelAnt(
    id: Int,
    size: Dp,
    planetRadius: Dp
) {
    val effectiveRadius = planetRadius - (size / 2)

    var targetOffsetX by remember { mutableStateOf(0.dp) }
    var targetOffsetY by remember { mutableStateOf(0.dp) }
    var duration by remember { mutableStateOf(1000) }
    var fadeDuration by remember { mutableStateOf(1000) }
    val alpha = remember { Animatable(0f) }

    val animatedOffsetX by animateDpAsState(
        targetValue = targetOffsetX,
        animationSpec = tween(durationMillis = duration),
        label = "ant_x_$id"
    )
    val animatedOffsetY by animateDpAsState(
        targetValue = targetOffsetY,
        animationSpec = tween(durationMillis = duration),
        label = "ant_y_$id"
    )

    LaunchedEffect(id) {
        // Stagger ant movement starts
        delay(id * 1500L + Random.nextLong(500))
        
        while (true) {

            // --- Ant is at center, invisible ---

            // 1. Fade in at the center
            alpha.animateTo(1f, animationSpec = tween(fadeDuration))

            val speed = 10f // dp per second

            // 2. Move to a random point 3 to 7 times
            for (i in 1..Random.nextInt(3, 7)) {
                val (randomX, randomY) = randomPointInCircle(effectiveRadius)
                val distanceToRandom = sqrt(randomX.value.pow(2) + randomY.value.pow(2)).toFloat()

                val travelDuration = ((distanceToRandom / speed) * 1000).toInt().coerceAtLeast(1000)

                duration = travelDuration
                targetOffsetX = randomX
                targetOffsetY = randomY

                // Wait for travel and a pause
                delay(travelDuration.toLong() + Random.nextLong(500, 1500))
            }

            // 3. Move back to the center
            val (randomX, randomY) = randomPointInCircle(effectiveRadius)
            val distanceToCenter = sqrt(randomX.value.pow(2) + randomY.value.pow(2)).toFloat()
            val travelDuration = ((distanceToCenter / speed) * 1000).toInt().coerceAtLeast(1000)

            duration = travelDuration
            targetOffsetX = 0.dp
            targetOffsetY = 0.dp

            delay(travelDuration.toLong())

            // 4. Fade out at the center
            alpha.animateTo(0f, animationSpec = tween(fadeDuration))

            // 5. Disappear for 7 seconds
            delay(7000)
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data("file:///android_asset/pixel_ant.png")
            .build(),
        contentDescription = "Pixel Ant",
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha.value }
            .size(size)
            .align(Alignment.Center)
            .offset(x = animatedOffsetX, y = animatedOffsetY)
    )
}

@Composable
fun BadgesGrid() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BadgeChip(
                label = "Streak 7 days",
                color = TrakRed,
                modifier = Modifier.weight(1f)
            )
            BadgeChip(
                label = "Bookworm",
                color = TrakBlue,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BadgeChip(
                label = "Gym Rat",
                color = TrakYellow,
                modifier = Modifier.weight(1f)
            )
            BadgeChip(
                label = "Sleep Master",
                color = TrakPurple,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun BadgeChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .height(48.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
