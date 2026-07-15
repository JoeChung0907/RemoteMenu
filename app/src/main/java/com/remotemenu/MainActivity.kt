package com.remotemenu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.remotemenu.ui.*
import com.remotemenu.ui.theme.RemoteMenuTheme

class MainActivity : ComponentActivity() {

    private val bluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Manifest.permission.BLUETOOTH_CONNECT
    } else {
        "android.permission.BLUETOOTH" // API 31 미만용 대체 권한
    }
    
    private val requestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetoothPermission()
    }

    private fun requestBluetoothPermission() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            bluetoothPermission
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            launchApp()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(bluetoothPermission),
                requestCode
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED

        if (requestCode == this.requestCode && granted) {
            launchApp()
        }
    }

    private fun launchApp() {
        setContent {
            RemoteMenuTheme {
                RemoteMenuApp()
            }
        }
    }
}

@Composable
fun RemoteMenuApp(vm: MainViewModel = viewModel()) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    /** -----------------------------
     * 앱 시작 시 1회 실행되는 초기화 블록
     * ----------------------------- */
    LaunchedEffect(Unit) {
        vm.loadBluetoothPrinters(context)
        vm.initialize(context)
    }

    /** -----------------------------
     * 보조 저장 (생명주기 기반)
     * ----------------------------- */
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                vm.forceSave(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var tab by remember { mutableIntStateOf(1) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    label = { Text(stringResource(R.string.tab_settings)) },
                    icon = {}
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    label = { Text(stringResource(R.string.tab_order)) },
                    icon = {}
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    label = { Text(stringResource(R.string.tab_history)) },
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
