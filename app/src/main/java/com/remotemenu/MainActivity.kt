package com.remotemenu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remotemenu.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding

import com.remotemenu.ui.theme.RemoteMenuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemoteMenuTheme {
                RemoteMenuApp()
            }
        }
    }
}

@Composable
fun RemoteMenuApp(vm: MainViewModel = viewModel()) {
    var tab by remember { mutableStateOf(1) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    label = { Text("설정") },
                    icon = {}
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    label = { Text("주문") },
                    icon = {}
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    label = { Text("기록") },
                    icon = {}
                )
            }
        }
    ) { padding ->
        when (tab) {
            0 -> SettingsScreen(modifier = Modifier.padding(padding), vm = vm)
            1 -> OrderScreen(modifier = Modifier.padding(padding), vm = vm)
            2 -> HistoryScreen(modifier = Modifier.padding(padding), vm = vm)
        }
    }
}
