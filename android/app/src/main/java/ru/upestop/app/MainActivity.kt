package ru.upestop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.*
import ru.upestop.app.ui.theme.UpeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { UpeTheme {
            val nav = rememberNavController()
            val entry by nav.currentBackStackEntryAsState()
            val tabs = listOf("home" to R.string.home, "checklist" to R.string.checklist, "reference" to R.string.reference, "trainer" to R.string.trainer)
            Scaffold(bottomBar = { NavigationBar { tabs.forEach { (route, label) ->
                NavigationBarItem(selected = entry?.destination?.route == route,
                    onClick = { nav.navigate(route) { launchSingleTop = true } }, icon = {}, label = { Text(stringResource(label)) })
            } } }) { padding ->
                NavHost(nav, "home", Modifier.padding(padding)) {
                    tabs.forEach { (route, label) -> composable(route) { Text(stringResource(label)) } }
                }
            }
        } }
    }
}
