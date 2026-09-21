package com.lookseesee.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lookseesee.app.data.SettingsRepository
import com.lookseesee.app.kiosk.DndManager
import com.lookseesee.app.kiosk.LockTaskManager
import com.lookseesee.app.ui.navigation.AppNavigation
import com.lookseesee.app.ui.theme.LookSeeSeeTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val lockTaskManager by lazy { LockTaskManager(this) }
    private val dndManager by lazy { DndManager(this) }
    private val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LookSeeSeeTheme {
                AppNavigation(
                    onEnterKiosk = ::enterKiosk,
                    onExitKiosk = ::exitKiosk,
                )
            }
        }
    }

    private fun enterKiosk() {
        lockTaskManager.pin()
        lifecycleScope.launch {
            if (settingsRepository.current().silenceNotifications) {
                dndManager.beginSilence()
            }
        }
    }

    private fun exitKiosk() {
        dndManager.endSilence()
        lockTaskManager.unpin()
    }

    override fun onDestroy() {
        if (lockTaskManager.isPinned()) {
            exitKiosk()
        }
        super.onDestroy()
    }
}
