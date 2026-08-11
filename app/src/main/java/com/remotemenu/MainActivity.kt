package com.remotemenu

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.remotemenu.ui.*
import com.remotemenu.ui.theme.RemoteMenuTheme
import java.util.*

class MainActivity : ComponentActivity() {

    private val requestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRequiredPermissions()
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_CONNECT
            permissions += Manifest.permission.BLUETOOTH_SCAN
        } else {
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
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

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        launchApp()
    }

    private fun launchApp() {
        setContent {
            val vm: MainViewModel = viewModel()
            val language by vm.currentLanguage
            val context = LocalContext.current
            val currentConfig = LocalConfiguration.current
            val configuration = remember(language) {
                Configuration(currentConfig).apply {
                    @Suppress("DEPRECATION")
                    val locale = Locale(language)
                    Locale.setDefault(locale)
                    setLocale(locale)
                }
            }
            
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalContext provides context.createConfigurationContext(configuration)
            ) {
                RemoteMenuTheme {
                    RemoteMenuApp(vm)
                }
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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var tab by remember { mutableIntStateOf(1) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, label = { Text(stringResource(R.string.tab_settings)) }, icon = {})
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, label = { Text(stringResource(R.string.tab_order)) }, icon = {})
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, label = { Text(stringResource(R.string.tab_history)) }, icon = {})
                }
            }
        ) { padding ->
            when (tab) {
                0 -> SettingsScreen(modifier = Modifier.padding(padding), vm = vm)
                1 -> OrderScreen(modifier = Modifier.padding(padding), vm = vm)
                2 -> HistoryScreen(modifier = Modifier.padding(padding), vm = vm)
            }
        }

        /** -----------------------------
         * 안드로이드 10 보안 에러 방지용 글로벌 에러 오버레이
         * ----------------------------- */
        vm.globalErrorMessage.value?.let { errorMsg ->
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { /* block */ },
                contentAlignment = Alignment.Center
            ) {
                Card(modifier = Modifier.fillMaxWidth(0.8f).padding(16.dp)) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.error_dialog_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        SelectionContainer {
                            Text(text = errorMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { vm.globalErrorMessage.value = null }) { Text(stringResource(R.string.error_confirm)) }
                    }
                }
            }
        }
    }
}
