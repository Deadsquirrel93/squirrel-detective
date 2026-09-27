package com.packagespy.app.presentation

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.packagespy.app.presentation.detail.DetailScreen
import com.packagespy.app.presentation.main.MainScreen
import com.packagespy.app.presentation.picker.AppPickerScreen
import com.packagespy.app.presentation.settings.SettingsScreen
import com.packagespy.app.presentation.theme.NutSpyTheme
import com.packagespy.app.presentation.welcome.WelcomeScreen
import com.packagespy.app.presentation.wiki.WikiScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutSpyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NutSpyNavHost()
                }
            }
        }
    }
}

private object Routes {
    const val Welcome = "welcome"
    const val Settings = "settings"
    const val Wiki = "wiki"
    const val Picker = "picker"
    const val Results = "results/{includeSystem}"
    const val Detail = "detail/{packageName}"

    fun results(includeSystem: Boolean): String = "results/$includeSystem"
    fun detail(pkg: String): String = "detail/${Uri.encode(pkg)}"
}

@Composable
private fun NutSpyNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.Welcome) {
        composable(Routes.Welcome) {
            WelcomeScreen(
                onStartFullScan = { includeSystem ->
                    navController.navigate(Routes.results(includeSystem))
                },
                onChooseApp = { navController.navigate(Routes.Picker) },
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onOpenWiki = { navController.navigate(Routes.Wiki) },
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Wiki) {
            WikiScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Picker) {
            AppPickerScreen(
                onBack = { navController.popBackStack() },
                onAppSelected = { pkg -> navController.navigate(Routes.detail(pkg)) },
            )
        }
        composable(
            route = Routes.Results,
            arguments = listOf(navArgument("includeSystem") { type = NavType.BoolType }),
        ) { entry ->
            val includeSystem = entry.arguments?.getBoolean("includeSystem") ?: false
            MainScreen(
                includeSystem = includeSystem,
                onAppClick = { pkg -> navController.navigate(Routes.detail(pkg)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.Detail,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType }),
        ) {
            DetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
