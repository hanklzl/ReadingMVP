package com.littlemandarin.classics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.littlemandarin.classics.shared.AppInfo
import com.littlemandarin.classics.shared.AppInfoResourceKeys
import com.littlemandarin.classics.shared.GetAppInfoUseCase
import com.littlemandarin.classics.shared.progress.AndroidProgressServiceProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidProgressServiceProvider.initialize(applicationContext)

        setContent {
            LittleMandarinClassicsTheme {
                ReaderApp()
            }
        }
    }
}

private object ReaderRoutes {
    const val Home = "home"
}

@Composable
private fun ReaderApp() {
    val navController = rememberNavController()
    val appInfo = remember { GetAppInfoUseCase().invoke() }

    NavHost(
        navController = navController,
        startDestination = ReaderRoutes.Home,
    ) {
        composable(ReaderRoutes.Home) {
            HomeScreen(appInfo = appInfo)
        }
    }
}

@Composable
private fun HomeScreen(
    appInfo: AppInfo,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(appInfo.nameResourceKey.toStringResourceId()),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@StringRes
private fun String.toStringResourceId(): Int = when (this) {
    AppInfoResourceKeys.AppName -> R.string.app_name
    else -> R.string.app_name
}

@Composable
private fun LittleMandarinClassicsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) {
            darkColorScheme()
        } else {
            lightColorScheme()
        },
        content = content,
    )
}
