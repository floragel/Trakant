package com.example.trakant

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class Account(
    val id: String,
    val name: String
)

private const val PREFS_NAME = "trakant_prefs"
private const val KEY_ACCOUNTS_LIST = "accounts_list"
private const val KEY_CURRENT_USER_ID = "current_user_id"

// ---------- GESTION DES COMPTES VIA SHAREDPREFERENCES ----------

/**
 * Charge la liste des comptes stockée en SharedPreferences (JSON).
 */
fun loadAccounts(context: Context): List<Account> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_ACCOUNTS_LIST, null)
    
    if (json.isNullOrBlank()) {
        return emptyList()
    }

    return try {
        val type = object : TypeToken<List<Account>>() {}.type
        Gson().fromJson(json, type)
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * Sauvegarde la liste complète des comptes dans SharedPreferences.
 */
private fun saveAccountsList(context: Context, accounts: List<Account>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = Gson().toJson(accounts)
    prefs.edit().putString(KEY_ACCOUNTS_LIST, json).apply()
}

// ---------- USER COURANT ----------

fun loadCurrentUserId(context: Context): String? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_CURRENT_USER_ID, null)
}

private fun saveCurrentUserId(context: Context, userId: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_CURRENT_USER_ID, userId).apply()
}

// ---------- CRÉATION / RÉCUPÉRATION ----------

fun createAccount(context: Context, name: String): UserData {
    val id = UUID.randomUUID().toString()

    // Nouveau UserData par défaut
    val userData = UserData(
        userId = id,
        name = name,
        age = 18, // Valeur par défaut
        xp = 0,
        level = 1,
        colonySize = 1,
        completedQuests = mutableListOf(),
        badgesUnlocked = mutableListOf(),
        settings = UserSettings()
    )

    // 1. Sauvegarder les données spécifiques du user (via UserData.kt)
    saveUserDataFor(context, userData)

    // 2. Mettre à jour la liste des comptes
    val accounts = loadAccounts(context).toMutableList()
    accounts.add(Account(id = id, name = name))
    saveAccountsList(context, accounts)

    // 3. Définir comme user courant
    saveCurrentUserId(context, id)

    return userData
}

fun updateAccountName(context: Context, userId: String, newName: String) {
    // 1. Mettre à jour la liste des comptes
    val accounts = loadAccounts(context).toMutableList()
    val index = accounts.indexOfFirst { it.id == userId }
    if (index != -1) {
        accounts[index] = accounts[index].copy(name = newName)
        saveAccountsList(context, accounts)
    }
    
    // 2. Mettre à jour UserData
    val userData = loadUserDataFor(context, userId)
    userData.name = newName
    saveUserDataFor(context, userData)
}

fun loadUserDataFor(context: Context, userId: String): UserData {
    // Délègue simplement au stockage SharedPreferences géré dans UserData.kt
    return loadUserData(context, userId)
}

fun saveUserDataFor(context: Context, userData: UserData) {
    // Délègue simplement au stockage SharedPreferences géré dans UserData.kt
    saveUserData(context, userData)
}
