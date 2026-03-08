package com.example.jarvisandroid

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages Short-term and Long-term memory for Jarvis.
 * Simulates semantic memory using localized preferences and interaction history.
 */
class MemoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("JarvisMemory", Context.MODE_PRIVATE)

    // Long-term: User Preferences and Routines
    fun savePreference(key: String, value: String) {
        prefs.edit().putString("pref_$key", value).apply()
    }

    fun getPreference(key: String): String? = prefs.getString("pref_$key", null)

    // Short-term: Conversational Context
    private val conversationHistory = mutableListOf<JSONObject>()

    fun addInteraction(userQuery: String, jarvisResponse: String) {
        val interaction = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("user", userQuery)
            put("jarvis", jarvisResponse)
        }
        conversationHistory.add(interaction)
        if (conversationHistory.size > 20) conversationHistory.removeAt(0)
        
        // Persist history snippet for long-term "learning"
        saveHistory(conversationHistory)
    }

    fun getHistory(): JSONArray = JSONArray(conversationHistory)

    private fun saveHistory(history: List<JSONObject>) {
        val array = JSONArray(history)
        prefs.edit().putString("short_term_history", array.toString()).apply()
    }

    fun loadMemory() {
        val historyStr = prefs.getString("short_term_history", "[]")
        val array = JSONArray(historyStr)
        conversationHistory.clear()
        for (i in 0 until array.length()) {
            conversationHistory.add(array.getJSONObject(i))
        }
    }

    fun getSystemContext(): JSONObject {
        return JSONObject().apply {
            put("user_name", getPreference("name") ?: "Sir")
            put("last_interaction", conversationHistory.lastOrNull()?.optLong("timestamp") ?: 0L)
            put("interaction_count", conversationHistory.size)
        }
    }
}