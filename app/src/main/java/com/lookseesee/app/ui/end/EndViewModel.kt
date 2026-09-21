package com.lookseesee.app.ui.end

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.lookseesee.app.data.SettingsRepository

class EndViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)

    suspend fun verifyPin(pin: String): Boolean = settingsRepository.verifyPin(pin)
}
