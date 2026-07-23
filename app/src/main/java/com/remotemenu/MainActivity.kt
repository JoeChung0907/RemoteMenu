package com.remotemenu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import java.util.*

/**
 * MainActivity
 * 앱의 진입점 및 권한 관리를 담당하는 액티비티.
 */
class MainActivity : ComponentActivity() {

    private val requestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Log.d("PermissionCheck", "Android 10 이하 기기이므로 권한 요청을 건너뜁니다.")
            launchApp()
        } else {
            requestRequiredPermissions()
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
            permissions += Manifest.permission.READ_MEDIA_IMAGES
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        }

        val needRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needRequest.toTypedArray(), requestCode)
        } else {
            launchApp()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        launchApp()
    }

    private fun launchApp() {
        setContent {
            val vm: MainViewModel = viewModel()
            val language by vm.currentLanguage
            
            val context = LocalContext.current
            val configuration = context.resources.configuration
            val locale = Locale(language)
            
            if (configuration.locales[0] != locale) {
                Locale.setDefault(locale)
                configuration.setLocale(locale)
                context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            }

            RemoteMenuTheme {
                RemoteMenuApp(vm)
            }
        }
    }
}

@Composable
fun RemoteMenuApp(vm: MainViewModel) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        vm.loadBluetoothPrinters(context)
        vm.initialize(context)
    }

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
