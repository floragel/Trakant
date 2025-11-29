package com.example.trakant

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- COULEURS TRAKANT (pastel fourmilière) ----------

private val TrakBackground = Color(0xFFCDECC4)   // vert pastel fond
private val TrakLevelBar   = Color(0xFF4B9C6A)   // vert plus foncé
private val TrakCreamCard  = Color(0xFFF7F0D9)   // carte beige
private val TrakPlanetGrass = Color(0xFF99D46D)  // herbe planète
private val TrakPlanetSoil  = Color(0xFFB77746)  // terre planète
private val TrakSoilStrip   = Color(0xFF8B5A3C)  // bande de sol
private val TrakTextDark    = Color(0xFF233221)

private val TrakBlue   = Color(0xFF4AA7C9)
private val TrakRed    = Color(0xFFF28B82)
private val TrakYellow = Color(0xFFF6C453)
private val TrakPurple = Color(0xFFB39DDB)

// ---------- ACTIVITÉ PRINCIPALE ----------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Créer le canal de notification au lancement
        createNotificationChannel(this)
        // Planifier les alarmes si elles ne sont pas déjà planifiées
        scheduleDailyNotifications(this)

        setContent {
            MaterialTheme {
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
    GRAPHS("Graphs", Icons.AutoMirrored.Filled.TrendingUp),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun TrakAntHomeApp() {
    val context = LocalContext.current

    // Charger GameData pour pouvoir lier quêtes <-> missions
    // Utilisation de remember pour ne charger qu'une fois
    val gameData = remember {
        try {
            loadGameData(context)
        } catch (e: Exception) {
            // Fallback si fichier manquant
            GameData(emptyList(), emptyList(), emptyList())
        }
    }

    // State pour userData
    val userDataState = remember { mutableStateOf<UserData?>(null) }
    
    // Callback pour rafraîchir les données
    val refreshData = {
        val currentId = loadCurrentUserId(context)
        if (currentId != null) {
            userDataState.value = loadUserDataFor(context, currentId)
        }
    }

    LaunchedEffect(Unit) {
        // 1. Essayer de lire l'utilisateur courant
        var currentId = loadCurrentUserId(context)
        
        // 2. Si pas d'utilisateur courant, on essaie de récupérer les comptes existants
        if (currentId == null) {
            val accounts = loadAccounts(context)
            if (accounts.isNotEmpty()) {
                currentId = accounts.first().id
                val prefs = context.getSharedPreferences("trakant_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("current_user_id", currentId).apply()
            } else {
                // Si aucun compte, on crée "Root" par défaut
                val newUser = createAccount(context, "Root")
                currentId = newUser.userId
            }
        }
        
        // 3. Maintenant qu'on a un ID, on charge ses données
        if (currentId != null) {
            userDataState.value = loadUserDataFor(context, currentId)
        }
    }

    val userData = userDataState.value
    var currentTab by rememberSaveable { mutableStateOf(TrakTab.HOME) }

    if (userData != null) {
        TrakAntAppContent(
            userData = userData,
            gameData = gameData,
            currentTab = currentTab,
            onTabSelected = { currentTab = it },
            onSettingsChanged = {
                 refreshData()
            }
        )
    } else {
        // Ecran de chargement simple
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun TrakAntAppContent(
    userData: UserData,
    gameData: GameData,
    currentTab: TrakTab,
    onTabSelected: (TrakTab) -> Unit,
    onSettingsChanged: () -> Unit
) {
    val context = LocalContext.current

    // Gestion du bouton Retour : Si on n'est pas sur la Home, on y retourne
    BackHandler(enabled = currentTab != TrakTab.HOME) {
        onTabSelected(TrakTab.HOME)
    }

    Scaffold(
        containerColor = TrakBackground,
        bottomBar = {
            TrakBottomBar(
                current = currentTab,
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                TrakTab.HOME -> TrakAntHomeScreen(
                    userData = userData,
                    gameData = gameData
                )
                TrakTab.QUESTS -> QuestsScreen()
                TrakTab.GRAPHS -> GraphsScreen()
                TrakTab.SETTINGS -> SettingsScreen(
                    userData = userData,
                    onSave = { newName, newAge, notificationsEnabled -> 
                        // 1. On met à jour l'objet localement d'abord
                        val oldName = userData.name
                        userData.name = newName
                        userData.age = newAge
                        userData.settings = userData.settings.copy(notificationsEnabled = notificationsEnabled)
                        
                        // 2. On sauvegarde l'objet complet (Source de vérité)
                        saveUserDataFor(context, userData)
                        
                        // 3. Si le nom a changé, on met à jour la liste des comptes
                        if (oldName != newName) {
                            updateAccountName(context, userData.userId, newName)
                        }
                        
                        // 4. On rafraîchit l'interface
                        onSettingsChanged()
                        
                        // 5. Feedback et navigation
                        Toast.makeText(context, "Data Saved", Toast.LENGTH_SHORT).show()
                        onTabSelected(TrakTab.HOME)
                    }
                )
            }
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
fun TrakAntHomeScreen(
    userData: UserData,
    gameData: GameData
) {
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
            LevelHeader(level = userData.level, xp = userData.xp)

            Spacer(Modifier.height(16.dp))

            // --- Salut utilisateur ---
            Text(
                text = "Hi, ${userData.name}",
                color = TrakTextDark,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(16.dp))

            // --- Grande carte centrale avec Stats réelles ---
            MainStatsCard(userData = userData, gameData = gameData)
        }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(Modifier.height(16.dp))

            // --- Badges ---
            BadgesGrid()

            Spacer(Modifier.height(16.dp))

            // --- Bande de sol pixel style ---
            SoilStrip()
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
fun MainStatsCard(
    userData: UserData,
    gameData: GameData
) {
    // Fonction helper pour calculer le score/progrès par mission
    // Ici on fait un ratio simple : (nombre de quêtes finies de ce type) / 10 (cap arbitraire)
    fun calculateProgress(missionId: String): Float {
        val count = userData.completedQuests.count { completed ->
            val questDef = gameData.quests.find { it.id == completed.questId }
            questDef?.missionId == missionId
        }
        // On cap à 1.0f (10 quêtes = 100%)
        return (count / 10f).coerceIn(0f, 1f)
    }

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
                    progress = calculateProgress("fitness"),
                    color = TrakLevelBar
                )
                HabitWithProgress(
                    title = "Study",
                    progress = calculateProgress("study"),
                    color = TrakRed
                )
                HabitWithProgress(
                    title = "Sleep",
                    progress = calculateProgress("sleep"),
                    color = TrakPurple
                )
                HabitWithProgress(
                    title = "Book",
                    progress = calculateProgress("book"),
                    color = TrakYellow
                )
            }

            // --- Droite : planète colonie ---
            Column(
                modifier = Modifier
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AntPlanet(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Colony: ${userData.colonySize} ants",
                    color = TrakTextDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = TrakTextDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            // Affiche le % pour voir que ça bouge
            Text(
                text = "${(progress * 100).toInt()}%",
                color = TrakTextDark.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
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

// Planète colonie en pixel-style simplifié (vert + sol + points fourmis)
@Composable
fun AntPlanet(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(TrakPlanetGrass)
    ) {
        // sol marron en bas
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(40.dp)
                .background(TrakPlanetSoil)
        )

        // petites "fourmis" abstraites
        val antColor = Color(0xFF1C1C1C)
        val size = 8.dp

        AntDot(antColor, size, Alignment.TopStart, offsetX = 12.dp, offsetY = 12.dp)
        AntDot(antColor, size, Alignment.TopCenter, offsetY = 10.dp)
        AntDot(antColor, size, Alignment.TopEnd, offsetX = (-12).dp, offsetY = 16.dp)
        AntDot(antColor, size, Alignment.Center, offsetX = 0.dp, offsetY = 0.dp)
        AntDot(antColor, size, Alignment.CenterStart, offsetX = 8.dp, offsetY = 20.dp)
        AntDot(antColor, size, Alignment.CenterEnd, offsetX = (-6).dp, offsetY = (-4).dp)
        AntDot(antColor, size, Alignment.BottomCenter, offsetY = (-12).dp)
    }
}

// simple helper extension to align with offset in AntPlanet
@Composable
private fun BoxScope.AntDot(
    color: Color,
    size: Dp,
    baseAlignment: Alignment,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .align(
                when (baseAlignment) {
                    Alignment.TopStart -> Alignment.TopStart
                    Alignment.TopCenter -> Alignment.TopCenter
                    Alignment.TopEnd -> Alignment.TopEnd
                    Alignment.CenterStart -> Alignment.CenterStart
                    Alignment.Center -> Alignment.Center
                    Alignment.CenterEnd -> Alignment.CenterEnd
                    Alignment.BottomStart -> Alignment.BottomStart
                    Alignment.BottomCenter -> Alignment.BottomCenter
                    Alignment.BottomEnd -> Alignment.BottomEnd
                    else -> baseAlignment
                }
            )
            .offset(x = offsetX, y = offsetY)
            .clip(CircleShape)
            .background(color)
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

@Composable
fun SoilStrip() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .background(TrakSoilStrip),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Ant colony biome",
            color = Color(0xFFF5E3C7),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ---------- ÉCRANS DE L'APPLICATION ----------

@Composable
fun QuestsScreen() {
    // Page simple pour les quêtes
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Page des Quêtes", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TrakTextDark)
    }
}

@Composable
fun GraphsScreen() {
    // Page simple pour les graphiques
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Page des Graphiques", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TrakTextDark)
    }
}

@Composable
fun SettingsScreen(
    userData: UserData,
    onSave: (newName: String, newAge: Int, notificationsEnabled: Boolean) -> Unit
) {
    var nameState by remember(userData.name) { mutableStateOf(userData.name) }
    var ageState by remember(userData.age) { mutableStateOf(userData.age.toString()) }
    var notificationsState by remember(userData.settings.notificationsEnabled) { mutableStateOf(userData.settings.notificationsEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "User Settings", 
            fontSize = 24.sp, 
            fontWeight = FontWeight.Bold, 
            color = TrakTextDark
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Champ Nom
        OutlinedTextField(
            value = nameState,
            onValueChange = { nameState = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Champ Age
        OutlinedTextField(
            value = ageState,
            onValueChange = { newValue ->
                // On n'autorise que les chiffres
                if (newValue.all { it.isDigit() }) {
                    ageState = newValue
                }
            },
            label = { Text("Age") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Switch Notifications
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Enable Daily Notifications", fontSize = 16.sp, color = TrakTextDark)
            Switch(
                checked = notificationsState,
                onCheckedChange = { notificationsState = it }
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = {
                val ageInt = ageState.toIntOrNull() ?: 18
                onSave(nameState, ageInt, notificationsState)
            },
            colors = ButtonDefaults.buttonColors(containerColor = TrakLevelBar)
        ) {
            Text("Save Changes", color = Color.White)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrakAntAppPreview() {
    val fakeUser = UserData(
        userId = "preview_user",
        name = "Preview Lumi",
        age = 20,
        xp = 430,
        level = 12,
        colonySize = 12,
        completedQuests = mutableListOf(),
        badgesUnlocked = mutableListOf(),
        settings = UserSettings(notificationsEnabled = true)
    )
    
    // Fake GameData pour la preview
    val fakeGameData = GameData(
        missions = listOf(),
        quests = listOf(),
        badges = listOf()
    )

    MaterialTheme {
        TrakAntAppContent(
            userData = fakeUser,
            gameData = fakeGameData,
            currentTab = TrakTab.SETTINGS,
            onTabSelected = {},
            onSettingsChanged = {}
        )
    }
}
