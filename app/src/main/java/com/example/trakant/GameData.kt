package com.example.trakant

import android.content.Context
import com.google.gson.Gson

data class Mission(
    val id: String,
    val name: String,
    val coef: Int
)

data class Quest(
    val id: String,
    val missionId: String,
    val title: String,
    val baseXp: Int
)

data class BadgeDef(
    val id: String,
    val name: String,
    val description: String
)

data class GameData(
    val missions: List<Mission>,
    val quests: List<Quest>,
    val badges: List<BadgeDef>
)

fun readAssetFile(context: Context, fileName: String): String {
    return context.assets.open(fileName).bufferedReader().use { it.readText() }
}

fun loadGameData(context: Context): GameData {
    val json = readAssetFile(context, "game_data.json")
    return Gson().fromJson(json, GameData::class.java)
}