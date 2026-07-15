package com.remotemenu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remotemenu.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import com.remotemenu.ui.theme.RemoteMenuTheme

class MainActivity : ComponentActivity() {
    private val bluetoothPermission = Manifest.permission.BLUETOOTH_CONNECT
    private val requestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 블루투스 권한 요청
        checkBluetoothPermission()

        setContent {
            RemoteMenuTheme {
                RemoteMenuApp()
            }
        }
    }

    private fun checkBluetoothPermission() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            bluetoothPermission
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(bluetoothPermission),
                requestCode
            )
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
