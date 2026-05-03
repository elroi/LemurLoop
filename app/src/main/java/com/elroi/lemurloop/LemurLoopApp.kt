package com.elroi.lemurloop

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.elroi.lemurloop.domain.manager.SettingsManager
import androidx.hilt.work.HiltWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Same store name as SettingsManager so we read the app's chosen language before Hilt is available. */
private val Context.localeDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

private val APP_LANGUAGE_KEY = stringPreferencesKey("app_language")

@HiltAndroidApp
class LemurLoopApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsManager: SettingsManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun attachBaseContext(base: Context) {
        val wrapped = runCatching {
            val lang = runBlocking(Dispatchers.IO) {
                base.localeDataStore.data.first()[APP_LANGUAGE_KEY] ?: ""
            }
            val langNorm = if (lang == "iw") "he" else lang
            if (langNorm in listOf("he", "iw", "en")) {
                // Use "iw" for Hebrew so resources load from values-iw (works reliably on all devices).
                val tagForLocale = if (langNorm == "he") "iw" else langNorm
                val locale = Locale.forLanguageTag(tagForLocale)
                Locale.setDefault(locale)
                val config = android.content.res.Configuration(base.resources.configuration).apply {
                    setLocale(locale)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        setLocales(android.os.LocaleList(locale))
                    }
                }
                base.createConfigurationContext(config)
            } else {
                base
            }
        }.getOrElse { base }
        super.attachBaseContext(wrapped)
    }

    override fun onCreate() {
        super.onCreate()
        seedDevApiKeysFromBuildConfig()
        FirebaseApp.initializeApp(this)
        setupCrashHandler()
    }

    /**
     * Debug builds only: copies keys from root `local.properties` (via BuildConfig) into DataStore
     * when the corresponding setting is still empty, so device installs pick up dev keys after install.
     * Does not overwrite keys already saved in settings.
     */
    private fun seedDevApiKeysFromBuildConfig() {
        if (!BuildConfig.DEBUG) return
        val gemini = BuildConfig.DEV_GEMINI_API_KEY.trim()
        val tts = BuildConfig.DEV_CLOUD_TTS_API_KEY.trim()
        if (gemini.isEmpty() && tts.isEmpty()) return
        runBlocking(Dispatchers.IO) {
            if (gemini.isNotEmpty() && settingsManager.geminiApiKeyFlow.first().isBlank()) {
                settingsManager.saveGeminiApiKey(gemini)
                settingsManager.saveIsCloudAiEnabled(true)
            }
            if (tts.isNotEmpty() && settingsManager.cloudTtsApiKeyFlow.first().isBlank()) {
                settingsManager.saveCloudTtsApiKey(tts)
                settingsManager.saveIsCloudTtsEnabled(true)
            }
        }
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                // Get stack trace
                val sw = StringWriter()
                exception.printStackTrace(PrintWriter(sw))
                val stackTrace = sw.toString()

                // Generate filename with timestamp
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val filename = "crash_$timestamp.txt"
                
                // Write to cache directory (accessible via adb run-as or Device File Explorer without root)
                val crashFile = File(cacheDir, filename)
                crashFile.writeText("Crash in thread ${thread.name}\n\n$stackTrace")
            } catch (e: Exception) {
                // Ignore errors during crash handling to avoid infinite loops
            } finally {
                // Pass to default handler so Android still shows the crash dialogue / kills the process
                defaultHandler?.uncaughtException(thread, exception)
            }
        }
    }
}
