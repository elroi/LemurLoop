package com.elroi.lemurloop.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal val Context.timerSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "timer_settings"
)
