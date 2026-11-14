package com.appnova.novatube.config

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NovaConfigRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<NovaConfig> = _config.asStateFlow()

    private fun loadConfig(): NovaConfig =
        NovaConfig.fromJson(prefs.getString(KEY_CONFIG_JSON, null))

    fun update(transform: (NovaConfig) -> NovaConfig) {
        val updated = transform(_config.value)
        _config.value = updated
        prefs.edit().putString(KEY_CONFIG_JSON, updated.toJson()).apply()
    }

    fun resetToDefaults() {
        val defaults = NovaConfig()
        _config.value = defaults
        prefs.edit().putString(KEY_CONFIG_JSON, defaults.toJson()).apply()
    }

    companion object {
        const val PREFS_NAME = "novatube_prefs"
        const val KEY_CONFIG_JSON = "config_json"
    }
}
