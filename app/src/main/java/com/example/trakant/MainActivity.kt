package com.example.trakant


import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import android.app.Application
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- VIEWMODEL (Pont entre UI et Database) ---
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val questDao = AppDatabase.getDatabase(application).questDao()
    val quests: Flow<List<Quest>> = questDao.getAllQuests()

    fun addQuest(type: QuestType, note: String?) {
        viewModelScope.launch { questDao.insertQuest(Quest(type = type, note = if (note.isNullOrBlank()) null else note)) }
    }
    fun deleteQuest(quest: Quest) {
        viewModelScope.launch { questDao.deleteQuest(quest) }
    }
    fun toggleQuest(quest: Quest, onStatusChanged: (Boolean) -> Unit) {
        viewModelScope.launch {
            val newStatus = !quest.isCompleted
            questDao.updateQuest(quest.copy(isCompleted = newStatus))
            onStatusChanged(newStatus)
        }
    }
}

// --- MAIN ACTIVITY ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CRÉATION DU CANAL DE NOTIFICATION (doit être fait au démarrage)
        createNotificationChannel(this)

        setContent {
            MaterialTheme {
                TrakAntApp()
            }
        }
    }
}

// NOUVEAU: Ajout de l'onglet CHARTS
enum class TrakTab(val label: String, val icon: ImageVector) {
    HOME("Accueil", Icons.Default.Home),
    QUESTS("Quêtes", Icons.Default.Check),
    CHARTS("Statistiques", Icons.AutoMirrored.Filled.TrendingUp), // Nouveau
    SETTINGS("Paramètres", Icons.Default.Settings)
}

@Composable
fun TrakAntApp() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(context.applicationContext as Application) as T
    })

    // Charger et observer les UserData
    var userData by remember { mutableStateOf(UserManager.loadUser(context)) }

    // Callback pour l'XP et le Logbook
    val updateXp = { amount: Int, source: String, date: String, add: Boolean ->
        userData = if (add) UserManager.addXp(context, userData, amount, source, date)
        else UserManager.removeXp(context, userData, amount)
    }

    // Callback pour le Profil et les Notifications
    val saveProfile = { name: String, age: Int, notif: Boolean ->
        val oldNotif = userData.notificationsEnabled
        userData = UserManager.updateProfile(context, userData, name, age, notif)

        // LOGIQUE DE NOTIFICATION : Planifier ou Annuler
        if (notif && !oldNotif) {
            scheduleDailyNotifications(context)
            Toast.makeText(context, "Notifications planifiées pour 19h30 !", Toast.LENGTH_SHORT).show()
        } else if (!notif && oldNotif) {
            cancelDailyNotifications(context)
            Toast.makeText(context, "Notifications annulées.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Profil sauvegardé.", Toast.LENGTH_SHORT).show()
        }
    }

    // NOUVEAUX Callbacks pour le Debug
    val updateXpRaw = { amount: Int -> userData = UserManager.addRawXp(context, userData, amount) }
    val resetUser = {
        userData = UserManager.resetUser(context, userData)
        Toast.makeText(context, "Utilisateur réinitialisé !", Toast.LENGTH_SHORT).show()
    }

    var currentTab by rememberSaveable { mutableStateOf(TrakTab.HOME) }

    Scaffold(
        containerColor = TrakBackground,
        bottomBar = {
            NavigationBar(containerColor = TrakCreamCard, tonalElevation = 4.dp) {
                TrakTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == currentTab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (currentTab) {
                TrakTab.HOME -> HomeScreen(
                    userData = userData,
                    onRetroactiveAdd = { type, date ->
                        updateXp(type.baseXp, type.title, date, true)
                        Toast.makeText(context, "Activité passée ajoutée au Logbook !", Toast.LENGTH_SHORT).show()
                    }
                )
                TrakTab.QUESTS -> QuestsScreen(
                    viewModel = viewModel,
                    onXpEvent = { amount, typeName, add ->
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        updateXp(amount, typeName, today, add)
                    }
                )
                TrakTab.CHARTS -> ChartsScreen(userData = userData)
                TrakTab.SETTINGS -> SettingsScreen(
                    userData = userData,
                    onSave = saveProfile,
                    onDebugXp = updateXpRaw, // PASS NOUVEAU CALLBACK
                    onReset = resetUser // PASS NOUVEAU CALLBACK
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// --- COMPOSANTS BADGES ET HOME (SIMPLIFIÉS/PRÉCÉDENTS) ---
// ------------------------------------------------------------------

data class DisplayBadge(val label: String, val color: Color)

@Composable
fun BadgeChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(14.dp), modifier = modifier.height(48.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            Text(label, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun DynamicBadgesGrid(userData: UserData) {
    // Logique simplifiée : badges basés sur le niveau et l'activité.
    val badges = mutableListOf<DisplayBadge>()
    if (userData.level >= 5) {
        badges.add(DisplayBadge("Fourmi Ouvrière (Lv 5+)", TrakYellow))
    }
    if (userData.level >= 10) {
        badges.add(DisplayBadge("Fourmi Soldat (Lv 10+)", TrakRed))
    }
    // Vérification de 3 jours d'activité
    val uniqueDays = userData.history.map { it.date }.distinct().size
    if (uniqueDays >= 3) {
        badges.add(DisplayBadge("Première Colonie (3 Jours)", TrakBlue))
    }

    if (badges.isEmpty()) {
        Text("Aucun badge débloqué pour l'instant.", fontStyle = FontStyle.Italic, color = TrakTextDark.copy(0.7f), modifier = Modifier.padding(16.dp))
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Layout en 2 colonnes
        badges.chunked(2).forEach { rowBadges ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowBadges.forEach { badge ->
                    BadgeChip(label = badge.label, color = badge.color, modifier = Modifier.weight(1f))
                }
                // Remplir l'espace s'il n'y a qu'un badge sur la ligne
                if (rowBadges.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}


// ------------------------------------------------------------------
// --- ÉCRAN HOME (SIMPLIFIÉ) -----------
// ------------------------------------------------------------------
@Composable
fun HomeScreen(userData: UserData, onRetroactiveAdd: (QuestType, String) -> Unit) {
    var showRetroDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        item {
            LevelHeader(level = userData.level, xp = userData.xp)
            Spacer(Modifier.height(16.dp))
            Text("Salut, ${userData.name}", color = TrakTextDark, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))
            MainStatsCard(userData)

            Spacer(Modifier.height(24.dp))

            // AFFICHAGE DES BADGES DYNAMIQUES
            Text("Mes Badges", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TrakTextDark)
            Spacer(Modifier.height(12.dp))
            DynamicBadgesGrid(userData)

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { showRetroDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = TrakTextDark.copy(alpha=0.1f), contentColor = TrakTextDark),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Voyage dans le temps (Ajout rétroactif)")
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            SoilStrip()
            Spacer(Modifier.height(40.dp)) // Padding pour le bas
        }
    }

    if (showRetroDialog) {
        RetroactiveDialog(onDismiss = { showRetroDialog = false }, onConfirm = onRetroactiveAdd)
    }
}


// ------------------------------------------------------------------
// --- ÉCRAN GRAPHIQUES (Ajout de la courbe et correction du DailyChartCard) ---
// ------------------------------------------------------------------

data class XpPoint(val date: String, val cumulativeXp: Int)

@Composable
fun XpHistoryLineChart(data: List<XpPoint>) {
    val lineColor = TrakLevelBar

    Card(
        colors = CardDefaults.cardColors(containerColor = TrakCreamCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(250.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {

            if (data.size <= 1) {
                Text("Plus de données sont nécessaires pour afficher la courbe.", fontStyle = FontStyle.Italic, color = TrakTextDark.copy(0.7f), modifier = Modifier.align(Alignment.Center))
            } else {
                val maxCumulativeXp = data.maxOf { it.cumulativeXp }.toFloat()

                Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 20.dp, end = 20.dp)) {
                    val points = data.mapIndexed { index, point ->
                        val x = (index.toFloat() / (data.size - 1)) * size.width
                        val y = size.height - (point.cumulativeXp.toFloat() / maxCumulativeXp) * size.height
                        Offset(x, y)
                    }

                    // Dessin de la ligne
                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = lineColor,
                            start = points[i],
                            end = points[i+1],
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Dessin des points
                    points.forEach { point ->
                        drawCircle(
                            color = lineColor,
                            radius = 6.dp.toPx(),
                            center = point
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = point
                        )
                    }
                }
            }
        }
    }
}

// --- NOUVEAU COMPOSANT : DailyChartCard ---
@Composable
fun DailyChartCard(date: String, typeCounts: Map<String, Int>, totalXp: Int, questTypeColors: Map<String, Color>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TrakCreamCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Formater la date pour la rendre plus lisible si elle est en format yyyy-MM-dd
                val displayDate = try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("EEEE d MMMM", Locale("fr", "FR")) // Ex: samedi 29 novembre
                    outputFormat.format(inputFormat.parse(date)!!)
                } catch (e: Exception) {
                    date
                }
                Text(displayDate.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TrakTextDark)
                Text("+${totalXp} XP", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = TrakLevelBar)
            }

            Spacer(Modifier.height(12.dp))

            if (typeCounts.isNotEmpty()) {
                Text("Détail des activités :", fontSize = 14.sp, color = TrakTextDark.copy(0.8f))
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    typeCounts.forEach { (typeName, count) ->
                        val color = questTypeColors[typeName] ?: Color.Gray
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                            Spacer(Modifier.width(8.dp))
                            Text("$typeName : $count complété(s)", fontSize = 14.sp, color = TrakTextDark)
                        }
                    }
                }
            } else {
                Text("Aucune quête terminée ce jour.", fontStyle = FontStyle.Italic, fontSize = 14.sp, color = TrakTextDark.copy(0.7f))
            }
        }
    }
}


@Composable
fun ChartsScreen(userData: UserData) {
    // 1. Agrégation des données par jour
    val completedQuestsByDay = remember(userData.history) {
        userData.history
            .groupBy { it.date }
            .toSortedMap() // Tri par date pour un affichage chronologique
    }

    // 2. Calcul de l'XP cumulée par jour (pour la courbe)
    val xpDataForChart = remember(userData.history) {
        val dailyXp = userData.history
            .groupBy { it.date }
            .mapValues { (_, entries) -> entries.sumOf { it.xpGained } }
            .toSortedMap()

        var cumulativeXp = 0
        dailyXp.map { (date, xpGained) ->
            cumulativeXp += xpGained
            XpPoint(date, cumulativeXp)
        }.toList()
    }

    // Assurez-vous d'avoir la définition de TrakAntColors et QuestType pour cette ligne
    val questTypeColors = QuestType.entries.associate { it.name to Color(it.colorHex) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Statistiques des Quêtes", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TrakTextDark, modifier = Modifier.padding(top = 16.dp))
            Spacer(Modifier.height(16.dp))
        }

        if (xpDataForChart.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = TrakCreamCard), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text("Complétez des quêtes pour voir vos statistiques !",
                        modifier = Modifier.padding(16.dp),
                        fontStyle = FontStyle.Italic,
                        color = TrakTextDark.copy(0.7f)
                    )
                }
            }
        } else {
            // --- COURBE D'XP CUMULÉE ---
            item {
                Text("Progression de l'XP", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TrakTextDark)
                Spacer(Modifier.height(8.dp))
                XpHistoryLineChart(data = xpDataForChart)
                Spacer(Modifier.height(24.dp))

                Divider(color = TrakLevelBar.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                Text("Détail par Jour et Type", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TrakTextDark)
                Spacer(Modifier.height(12.dp))
            }

            // Légende des couleurs
            item {
                Card(colors = CardDefaults.cardColors(containerColor = TrakCreamCard), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Légende des Catégories", fontWeight = FontWeight.Bold, color = TrakTextDark)
                        Spacer(Modifier.height(8.dp))
                        QuestType.entries.forEach { type ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(type.colorHex)))
                                Spacer(Modifier.width(8.dp))
                                Text(type.title, fontSize = 14.sp, color = TrakTextDark)
                            }
                        }
                    }
                }
            }

            // Affichage des cartes DailyChartCard
            items(completedQuestsByDay.toList().asReversed()) { (date, entries) ->
                val typeCounts = entries.groupingBy { it.typeName }.eachCount()
                DailyChartCard(date = date, typeCounts = typeCounts, totalXp = entries.sumOf { it.xpGained }, questTypeColors = questTypeColors)
                Spacer(Modifier.height(12.dp))
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

// ------------------------------------------------------------------
// --- ECRAN SETTINGS (Section Debug) --------------
// ------------------------------------------------------------------

@Composable
fun SettingsScreen(
    userData: UserData,
    onSave: (newName: String, newAge: Int, notificationsEnabled: Boolean) -> Unit,
    onDebugXp: (Int) -> Unit, // NOUVEAU
    onReset: () -> Unit // NOUVEAU
) {
    val context = LocalContext.current // Récupérer le contexte ici
    var nameState by remember(userData.name) { mutableStateOf(userData.name) }
    var ageState by remember(userData.age) { mutableStateOf(userData.age.toString()) }
    var notificationsState by remember(userData.notificationsEnabled) { mutableStateOf(userData.notificationsEnabled) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Paramètres Utilisateur", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TrakTextDark)
            Spacer(Modifier.height(24.dp))
        }

        // --- SECTION PROFIL ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TrakCreamCard),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Mon Profil", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TrakTextDark)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nameState,
                        onValueChange = { nameState = it },
                        label = { Text("Pseudo d'AntKeeper") },
                        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = ageState,
                        onValueChange = { if (it.all { char -> char.isDigit() }) ageState = it },
                        label = { Text("Âge") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Notifications Quotidiennes", fontSize = 16.sp, color = TrakTextDark)
                        Switch(
                            checked = notificationsState,
                            onCheckedChange = { notificationsState = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = TrakLevelBar, checkedTrackColor = TrakLevelBar.copy(alpha=0.4f))
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val ageInt = ageState.toIntOrNull() ?: userData.age
                            onSave(nameState, ageInt, notificationsState)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TrakLevelBar),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sauvegarder les modifications", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    // Bouton de Test de Notification
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            sendTestNotification(context) // Appel à la fonction de test
                            Toast.makeText(context, "Tentative d'envoi d'une notification de test.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TrakTextDark),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(TrakLevelBar.copy(alpha = 0.5f)))
                    ) {
                        Text("Tester la Notification", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // --- SECTION LOGBOOK (Historique) ---
        item {
            Spacer(Modifier.height(16.dp))
            Divider(color = TrakLevelBar.copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(Modifier.height(16.dp))
            Text("Logbook (Historique d'XP)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TrakTextDark)
            Spacer(Modifier.height(12.dp))
        }

        val reversedHistory = userData.history.asReversed() // Les plus récents en haut
        if (reversedHistory.isEmpty()) {
            item {
                Text("Aucune activité enregistrée.", fontStyle = FontStyle.Italic, color = TrakTextDark.copy(0.6f), modifier = Modifier.padding(8.dp))
            }
        } else {
            items(reversedHistory) { entry ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = TrakCreamCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(entry.typeName, fontWeight = FontWeight.SemiBold, color = TrakTextDark)
                            Text(entry.date, fontSize = 12.sp, color = TrakTextDark.copy(alpha=0.7f))
                        }
                        Text("+${entry.xpGained} XP", fontWeight = FontWeight.Bold, color = TrakLevelBar)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // --- SECTION DEBUG ---
        item {
            Spacer(Modifier.height(16.dp))
            Divider(color = TrakLevelBar.copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(Modifier.height(16.dp))
            DebugSection(
                currentLevel = userData.level,
                onAddXp = onDebugXp,
                onResetUser = onReset
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun DebugSection(currentLevel: Int, onAddXp: (Int) -> Unit, onResetUser: () -> Unit) {
    var xpAmount by remember { mutableStateOf("100") }

    Card(
        colors = CardDefaults.cardColors(containerColor = TrakRed.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("⚙️ Section Debug 🐜", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TrakTextDark)
            Text("Utilisation : Ajuster rapidement l'état du joueur.", fontSize = 12.sp, color = TrakTextDark.copy(0.7f))
            Spacer(Modifier.height(16.dp))

            // 1. Ajouter XP
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = xpAmount,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() || (it.startsWith("-") && it.length > 1) }) xpAmount = it },
                    label = { Text("Quantité d'XP (+ ou -)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).background(Color.White, RoundedCornerShape(8.dp))
                )
                Button(
                    onClick = { onAddXp(xpAmount.toIntOrNull() ?: 0) },
                    colors = ButtonDefaults.buttonColors(containerColor = TrakLevelBar)
                ) {
                    Text("Ajuster XP")
                }
            }
            Spacer(Modifier.height(12.dp))

            // 2. Info Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Level actuel : $currentLevel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TrakTextDark)
                Button(
                    onClick = { onAddXp(500) },
                    colors = ButtonDefaults.buttonColors(containerColor = TrakYellow)
                ) {
                    Text("Gain +500 XP")
                }
            }
            Spacer(Modifier.height(16.dp))

            // 3. Réinitialiser
            Divider(color = TrakRed.copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onResetUser,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TrakRed),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(TrakRed))
            ) {
                Text("Réinitialiser tout (XP, Level, Logbook)", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


// ------------------------------------------------------------------
// --- ECRAN QUESTS et Dialogues (SIMPLIFIÉS/PRÉCÉDENTS) --------------
// ------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetroactiveDialog(onDismiss: () -> Unit, onConfirm: (QuestType, String) -> Unit) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf<QuestType?>(null) }
    var selectedDate by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            // Format AAAA-MM-JJ pour le stockage dans le Logbook
            selectedDate = "$year-${String.format("%02d", month + 1)}-${String.format("%02d", day)}"
        },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )
    datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Voyage dans le temps") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (selectedDate.isEmpty()) "Choisir la date" else selectedDate)
                }
                Column(Modifier.height(150.dp).fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp))) {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(QuestType.entries) { type ->
                            Row(
                                Modifier.fillMaxWidth().clickable { selectedType = type }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (selectedType == type), onClick = { selectedType = type })
                                Text(type.title)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // La variable 'note' est maintenant déclarée.
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") })
            }
        },
        confirmButton = {
            Button(
                onClick = { if (selectedType != null && selectedDate.isNotEmpty()) { onConfirm(selectedType!!, selectedDate); onDismiss() } },
                enabled = selectedType != null && selectedDate.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = TrakLevelBar)
            ) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        containerColor = TrakCreamCard
    )
}


@Composable
fun QuestsScreen(viewModel: MainViewModel, onXpEvent: (Int, String, Boolean) -> Unit) {
    val quests by viewModel.quests.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text("Quêtes Journalières", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TrakTextDark)
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(quests, key = { it.id }) { quest ->
                    QuestItemView(
                        quest = quest,
                        onToggle = {
                            viewModel.toggleQuest(quest) { isCompleted ->
                                onXpEvent(quest.type.baseXp, quest.type.title, isCompleted)
                            }
                        },
                        onDelete = { viewModel.deleteQuest(quest) }
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = TrakLevelBar,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) { Icon(Icons.Default.Add, "Ajouter") }
    }
    if (showAddDialog) AddQuestDialog({ showAddDialog = false }) { t, n -> viewModel.addQuest(t, n); showAddDialog = false }
}

@Composable
fun QuestItemView(quest: Quest, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (quest.isCompleted) TrakCreamCard.copy(alpha = 0.6f) else TrakCreamCard)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = quest.isCompleted, onCheckedChange = { onToggle() }, colors = CheckboxDefaults.colors(checkedColor = TrakLevelBar))
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(quest.type.title, fontWeight = FontWeight.Bold, color = TrakTextDark, style = androidx.compose.ui.text.TextStyle(textDecoration = if(quest.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null))
                if (!quest.note.isNullOrBlank()) Text(quest.note, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = TrakTextDark.copy(0.7f))
            }
            Surface(color = Color(quest.type.colorHex), shape = RoundedCornerShape(8.dp)) { Text("+${quest.type.baseXp} XP", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(6.dp, 2.dp)) }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, "Suppr.", tint = TrakTextDark.copy(0.4f)) }
        }
    }
}

@Composable
fun AddQuestDialog(onDismiss: () -> Unit, onAdd: (QuestType, String?) -> Unit) {
    var selectedType by remember { mutableStateOf(QuestType.SPORT) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle Quête") },
        text = {
            Column {
                Column(Modifier.height(150.dp).fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp))) {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(QuestType.entries) { type ->
                            Row(Modifier.fillMaxWidth().clickable { selectedType = type }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = (selectedType == type), onClick = { selectedType = type })
                                Text(type.title)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") })
            }
        },
        confirmButton = { Button(onClick = { onAdd(selectedType, note) }, colors = ButtonDefaults.buttonColors(containerColor = TrakLevelBar)) { Text("Ajouter") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        containerColor = TrakCreamCard
    )
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
fun AntPlanet(modifier: Modifier = Modifier, antNumber: Int = 10) {
    BoxWithConstraints(modifier) {
        val planetRadius = maxWidth / 2
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(TrakPlanetGrass),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("file:///android_asset/house_lvl_1.png")
                    .build(),
                contentDescription = "Pixel House",
                modifier = Modifier
                    .align(Alignment.Center)
            )
            // petites "fourmis" pixel art
            val size = 16.dp
            for (i in 1..antNumber) {
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

    var targetOffsetX by remember { mutableStateOf(Random.nextInt(-10, 10).dp) }
    var targetOffsetY by remember { mutableStateOf(Random.nextInt(-10, 10).dp) }
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
            targetOffsetX = Random.nextInt(-5, 5).dp
            targetOffsetY = Random.nextInt(-5, 5).dp

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
