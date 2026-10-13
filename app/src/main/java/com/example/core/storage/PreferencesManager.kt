package com.example.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.model.OmniRouteConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "codepilot_settings")

class PreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val GATEWAY_BASE_URL = stringPreferencesKey("gateway_base_url")
        val GATEWAY_API_KEY = stringPreferencesKey("gateway_api_key")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val FALLBACK_MODEL = stringPreferencesKey("fallback_model")
        val AUTO_FALLBACK = booleanPreferencesKey("auto_fallback")
        val TIMEOUT_SECONDS = intPreferencesKey("timeout_seconds")
        val CONTEXT_BUDGET = intPreferencesKey("context_budget")
        val STREAM_RESPONSE = booleanPreferencesKey("stream_response")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
        val ACTIVE_PROJECT_ID = stringPreferencesKey("active_project_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val REMOTE_WORKER_URL = stringPreferencesKey("remote_worker_url")
    }

    val configFlow: Flow<OmniRouteConfig> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            OmniRouteConfig(
                baseUrl = preferences[PreferencesKeys.GATEWAY_BASE_URL] ?: "http://10.0.2.2:20128/v1",
                apiKey = CredentialCipher.decrypt(preferences[PreferencesKeys.GATEWAY_API_KEY] ?: ""),
                selectedModel = preferences[PreferencesKeys.SELECTED_MODEL] ?: "auto/coding",
                autoFallback = preferences[PreferencesKeys.AUTO_FALLBACK] ?: true,
                fallbackModel = preferences[PreferencesKeys.FALLBACK_MODEL] ?: "auto",
                timeoutSeconds = preferences[PreferencesKeys.TIMEOUT_SECONDS] ?: 60,
                contextBudgetTokens = preferences[PreferencesKeys.CONTEXT_BUDGET] ?: 16384,
                streamResponse = preferences[PreferencesKeys.STREAM_RESPONSE] ?: true,
                temperature = preferences[PreferencesKeys.TEMPERATURE] ?: 0.2f
            )
        }

    val githubTokenFlow: Flow<String> = context.dataStore.data.map { preferences ->
        CredentialCipher.decrypt(preferences[PreferencesKeys.GITHUB_TOKEN] ?: "")
    }

    val activeProjectIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACTIVE_PROJECT_ID]
    }

    val remoteWorkerUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.REMOTE_WORKER_URL] ?: ""
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "dark"
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_LANGUAGE] ?: "en"
    }

    suspend fun updateConfig(config: OmniRouteConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GATEWAY_BASE_URL] = config.baseUrl
            preferences[PreferencesKeys.GATEWAY_API_KEY] = CredentialCipher.encrypt(config.apiKey)
            preferences[PreferencesKeys.SELECTED_MODEL] = config.selectedModel
            preferences[PreferencesKeys.FALLBACK_MODEL] = config.fallbackModel
            preferences[PreferencesKeys.AUTO_FALLBACK] = config.autoFallback
            preferences[PreferencesKeys.TIMEOUT_SECONDS] = config.timeoutSeconds
            preferences[PreferencesKeys.CONTEXT_BUDGET] = config.contextBudgetTokens
            preferences[PreferencesKeys.STREAM_RESPONSE] = config.streamResponse
            preferences[PreferencesKeys.TEMPERATURE] = config.temperature
        }
    }

    suspend fun updateGithubToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GITHUB_TOKEN] = CredentialCipher.encrypt(token)
        }
    }

    suspend fun updateActiveProjectId(projectId: String?) {
        context.dataStore.edit { preferences ->
            if (projectId == null) {
                preferences.remove(PreferencesKeys.ACTIVE_PROJECT_ID)
            } else {
                preferences[PreferencesKeys.ACTIVE_PROJECT_ID] = projectId
            }
        }
    }

    suspend fun updateRemoteWorkerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMOTE_WORKER_URL] = url
        }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun updateLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = lang
        }
    }

    suspend fun clearCredentials() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GATEWAY_API_KEY] = ""
            preferences[PreferencesKeys.GITHUB_TOKEN] = ""
        }
    }
}
