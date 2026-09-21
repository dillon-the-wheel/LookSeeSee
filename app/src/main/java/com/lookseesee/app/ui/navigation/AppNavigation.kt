package com.lookseesee.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lookseesee.app.ui.end.EndScreen
import com.lookseesee.app.ui.session.SessionScreen
import com.lookseesee.app.ui.setup.SetupScreen

private object Routes {
    const val SETUP = "setup"
    const val SESSION = "session"
    const val END = "end"
}

@Composable
fun AppNavigation(
    onEnterKiosk: () -> Unit,
    onExitKiosk: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SETUP) {
        composable(Routes.SETUP) {
            SetupScreen(
                onBeginSession = { navController.navigate(Routes.SESSION) },
            )
        }
        composable(Routes.SESSION) {
            SessionScreen(
                onEnterKiosk = onEnterKiosk,
                onSessionEnded = { navController.navigate(Routes.END) },
            )
        }
        composable(Routes.END) {
            EndScreen(
                onPlayAgain = {
                    navController.navigate(Routes.SESSION) {
                        popUpTo(Routes.SETUP) { inclusive = false }
                    }
                },
                onGoHome = {
                    onExitKiosk()
                    navController.navigate(Routes.SETUP) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
            )
        }
    }
}
