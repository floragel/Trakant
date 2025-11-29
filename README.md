# 🐜 **TrakAnt — Application Mobile de Suivi de Productivité Gamifiée**  
_App Android développée pour le Hackathon HackDécouverte_

---

## 🎯 **Concept général**

**TrakAnt** est une application mobile Android qui transforme la productivité personnelle en un **jeu**, où chaque bonne habitude fait grandir votre **colonie de fourmis**.

Chaque action quotidienne — fitness, étude, sommeil, lecture — permet :

- de gagner de l’**XP**  
- de monter de **niveau**  
- d’agrandir une **colonie de fourmis** (1 niveau = 1 fourmi)  
- d’obtenir un **streak dédié** par catégorie (4 streaks indépendants)  
- d’obtenir des **badges**  
- de visualiser un **historique des activités (log history)**

L’objectif : rendre la discipline et la productivité **engagées, motivantes et amusantes**.

---

## 📱 **Fonctionnalités principales**

### 🏠 1. Home  
- Niveau + XP actuel  
- Taille de la colonie  
- Progression claire pour les 4 catégories :
  - **Fitness**
  - **Study**
  - **Sleep**
  - **Book**  
- Une planète en **pixel art** représentant la colonie  
- Les streaks individuels pour chaque catégorie  
- Accès aux badges débloqués

---

### 📘 2. Quests  
- Quêtes quotidiennes  
- Quêtes par catégorie  
- Gain d'XP selon le coefficient de la mission  
- Marquage "completed" et intégration au log

---

### 📊 3. Graphs  
- Visualisation des progrès dans chaque domaine  
- Pourcentage par catégorie  
- Courbes d’évolution (WIP)

---

### 🧾 4. Log History  
Un écran dédié qui affiche :

- les quêtes complétées  
- la date et l’heure  
- la mission associée  
- l’XP gagné  
- les streaks mis à jour  

Cela permet à l’utilisateur de suivre concrètement ses efforts.

---

### ⚙️ 5. Settings  
- Changer le **nom du compte**
- Changer l’âge  
- Activer/désactiver les **notifications**  
- Bouton : **Test Notification now**  
- Gestion multi-profils via LocalStorage  
- (Fonctionnalité Biomes désactivée pour cette version)

> ❗ Les styles de badges et les biomes seront ajoutés dans une version future.

---

## 🔔 **Notifications**

TrakAnt envoie 3 rappels par jour :

- **8h00** → Morning Check  
- **12h00** → Midday Motivation  
- **20h00** → Evening Wrap-up  

Basées sur :

- `AlarmManager`  
- `BroadcastReceiver`  
- `NotificationChannel` (Android 8+)  

Un bouton permet aussi d'envoyer une **notification instantanée** pour les tests.

---

## 📦 **Stockage des données (LocalStorage)**

L’application utilise **du LocalStorage** (fichiers locaux + SharedPreferences) pour sauvegarder :

- les comptes utilisateurs  
- l'XP  
- les niveaux  
- les quêtes complétées  
- les streaks  
- les notifications activées  
- le log history complet

Aucun backend n’est utilisé : l’app est **offline-first**.

---

## 🔢 **Logique XP / Level**

### XP par niveau (progression doublée)  
\`\`\`
Level 1 = 10 XP
Level 2 = 20 XP
Level 3 = 40 XP
Level 4 = 80 XP
...
\`\`\`

### Taille de colonie  
\`\`\`
Colony size = level
\`\`\`

### Streaks  
Chaque catégorie possède un streak séparé :

- 1 streak **Fitness**  
- 1 streak **Study**  
- 1 streak **Sleep**  
- 1 streak **Book**

Chaque complétion augmente uniquement le streak de la mission correspondante.

---

## 🎨 **Design & Pixel Art**

L’app utilise un style **pixel art modernisé** + **flat design** pastel.

### Images générées exclusivement avec **ChatGPT**
Nous avons utilisé **ChatGPT** pour créer :

✔ Les fourmis pixelisées  
✔ L’icône de l’application  

Toutes les images artistiques intégrées à l’app (fourmis + icône) viennent de ChatGPT.

---

## 🤖 **Usage de l’IA dans le projet**

### 🧠 ChatGPT (OpenAI)
- Génération des images (fourmis + icône)  
- Aide UI/UX  
- Conseils techniques  
- Génération du README  
- Aide au débogage et à la réflexion sur l’architecture

### 🤖 Gemini (Google, intégré à Android Studio)
- Génération de code Kotlin  
- Aide au débogage  
- Suggestions UI/UX  
- Implémentation de parties du code (notifications, écrans, logique, etc.)

> ChatGPT et Gemini ont été utilisés **ensemble** :  
> – ChatGPT pour le design, le texte, les idées, l’organisation  
> – Gemini pour l’assistance directe dans Android Studio (code Kotlin, corrections, complétion)

---

## 🛠️ **Technos utilisées**

- **Android Studio** (projet Kotlin)  
- **Jetpack Compose** (UI déclarative)  
- **Material 3**  
- **LocalStorage** via SharedPreferences et fichiers locaux  
- **GitHub** pour la collaboration :
  - Travail en équipe via **branches**  
  - Répartition des tâches (UI, logique, DB, notifications, README, etc.)  

---

## 🧪 **Tests effectués**

Les tests ont été réalisés sur les **téléphones mobiles réels** des membres de l’équipe :

- Notifications (programmées + test immédiat)  
- Création et mise à jour des comptes  
- Sauvegarde des données en LocalStorage  
- Progression XP et montée de niveau  
- Mise à jour des streaks par catégorie  
- Affichage et mise à jour du Log History  
- Comportement général de l’UI (Home, Quests, Graphs, Settings)

---

## 👥 **Équipe**

- **Alban**  
- **Timothée**  
- **Nayl**

---

## 📁 Structure du projet

\`\`\`
app/
 ├── src/
 │    └── main/
 │         ├── java/com/example/trakant/
 │         │       ├── MainActivity.kt
 │         │       ├── NotificationLogic.kt
 │         │       ├── UserManager.kt
 │         │       ├── AppDatabase.kt
 │         │       ├── models/
 │         │       │     ├── Quest.kt
 │         │       │     ├── QuestType.kt
 │         │       │     ├── UserData.kt
 │         │       ├── ui/
 │         │       │     ├── HomeScreen.kt
 │         │       │     ├── QuestScreen.kt
 │         │       │     ├── SettingsScreen.kt
 │         │       │     ├── GraphsScreen.kt
 │         │       │     ├── LogHistoryScreen.kt (si séparé)
 │         │
 │         ├── assets/
 │         │       ├── game_data.json   (config des quêtes & missions)
 │         │
 │         └── res/
 │               ├── drawable/
 │               ├── mipmap/           (icône de l’app générée via ChatGPT)
 │               └── values/
 │
 └── build.gradle
\`\`\`

---

# 🏁 **Conclusion**

**TrakAnt** est une app mobile fun, motivante et accessible, qui transforme la discipline en un jeu visuel autour d’une colonie de fourmis.  

Développée en Kotlin avec Android Studio, enrichie par l’aide de **ChatGPT** et **Gemini**, elle illustre comment l’IA peut être utilisée de façon créative et efficace pour concevoir :

- une mécanique de jeu motivante  
- une expérience utilisateur claire  
- un design cohérent  
- et un code fonctionnel, testé sur des appareils réels.
