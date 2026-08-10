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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.remotemenu.ui.*
import com.remotemenu.ui.theme.RemoteMenuTheme
import java.util.*

/**
 * MainActivity
 * 앱의 진입점으로서 권한 관리 및 실시간 언어(Locale) 전환 로직을 제어합니다.
 */
class MainActivity : ComponentActivity() {

    private val requestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        /** -----------------------------
         * 안드로이드 버전별 권한 분기
         * ----------------------------- */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // Android 10 이하는 설치 시 자동 승인이므로 즉시 실행
            launchApp()
        } else {
            requestRequiredPermissions()
        }
    }

    /**
     * requestRequiredPermissions
     * Android 12 이상에서 필요한 런타임 권한을 요청합니다.
     */
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

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // 결과와 상관없이 앱 실행 시도 (필요한 기능 사용 시점에서 다시 체크)
        launchApp()
    }

    /**
     * launchApp
     * Compose UI 환경을 설정하고 앱의 메인 콘텐츠를 화면에 띄웁니다.
     */
    private fun launchApp() {
        setContent {
            val vm: MainViewModel = viewModel()
            val language by vm.currentLanguage
            
            /** -----------------------------
             * 실시간 언어 변경 주입 엔진
             * ----------------------------- */
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
            
            // 새 Configuration을 하위 컴포저블 트리 전체에 강제 주입
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

/**
 * RemoteMenuApp
 * 앱의 메인 레이아웃(Scaffold)과 하단 네비게이션, 그리고 글로벌 에러 시스템을 관리합니다.
 */
@Composable
fun RemoteMenuApp(vm: MainViewModel) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    /** -----------------------------
     * 초기 로드 및 백그라운드 자동 저장 설정
     * ----------------------------- */
    LaunchedEffect(Unit) {
        vm.loadBluetoothPrinters(context)
        vm.initialize(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // 앱이 백그라운드로 전환될 때 최신 상태 강제 저장
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
        /** -----------------------------
         * 글로벌 에러 진단 다이얼로그 (중요)
         * ----------------------------- */
        vm.globalErrorMessage.value?.let { errorMsg ->
            AlertDialog(
                onDismissRequest = { vm.globalErrorMessage.value = null },
                title = { Text(stringResource(R.string.error_dialog_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.error_copy_guide))
                        Spacer(Modifier.height(8.dp))
                        SelectionContainer { 
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { vm.globalErrorMessage.value = null }) {
                        Text(stringResource(R.string.error_confirm))
                    }
                }
            )
        }

        /** -----------------------------
         * 메인 탭 화면 전환
         * ----------------------------- */
        when (tab) {
            0 -> SettingsScreen(modifier = Modifier.padding(padding), vm = vm)
            1 -> OrderScreen(modifier = Modifier.padding(padding), vm = vm)
            2 -> HistoryScreen(modifier = Modifier.padding(padding), vm = vm)
        }
    }
}
