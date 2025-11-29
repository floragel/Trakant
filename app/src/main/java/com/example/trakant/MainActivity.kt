package com.example.trakant

import android.app.Application
import android.app.DatePickerDialog
import androidx.compose.ui.graphics.SolidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

enum class TrakTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    QUESTS("Quêtes", Icons.Default.Check),
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
                TrakTab.SETTINGS -> SettingsScreen(
                    userData = userData,
                    onSave = saveProfile
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// --- ECRAN SETTINGS (Amélioré avec Logbook et Bouton de Test) -----
// ------------------------------------------------------------------
@Composable
fun SettingsScreen(
    userData: UserData,
    onSave: (newName: String, newAge: Int, notificationsEnabled: Boolean) -> Unit
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

                    // NOUVEAU: Bouton de Test de Notification
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            sendTestNotification(context) // Appel à la nouvelle fonction
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
        item { Spacer(Modifier.height(40.dp)) } // Padding pour le bas
    }
}


// ------------------------------------------------------------------
// --- Autres Écrans et Dialogues (inchangés) -----------------------
// ------------------------------------------------------------------

@Composable
fun HomeScreen(userData: UserData, onRetroactiveAdd: (QuestType, String) -> Unit) {
    var showRetroDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LevelHeader(level = userData.level, xp = userData.xp)
            Spacer(Modifier.height(16.dp))
            Text("Salut, ${userData.name}", color = TrakTextDark, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))
            MainStatsCard(userData)

            Spacer(Modifier.height(16.dp))
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
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            BadgesGrid()
            Spacer(Modifier.height(16.dp))
            SoilStrip()
        }
    }

    if (showRetroDialog) {
        RetroactiveDialog(onDismiss = { showRetroDialog = false }, onConfirm = onRetroactiveAdd)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetroactiveDialog(onDismiss: () -> Unit, onConfirm: (QuestType, String) -> Unit) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf<QuestType?>(null) }
    var selectedDate by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            // Format JJ/MM/AAAA pour l'affichage, mais AAAA-MM-JJ pour le stockage dans le Logbook
            selectedDate = "$year-${month + 1}-${String.format("%02d", day)}"
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
                                Text(type.title, fontSize = 14.sp)
                            }
                        }
                    }
                }
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