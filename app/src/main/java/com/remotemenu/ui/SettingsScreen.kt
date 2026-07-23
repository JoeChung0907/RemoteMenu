package com.remotemenu.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.NumberPicker
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.remotemenu.MainViewModel
import androidx.compose.ui.Alignment
import androidx.compose.material3.AssistChip
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.remotemenu.R
import com.remotemenu.bluetooth.BluetoothPrinter

/**
 * SettingsScreen
 * 프린터 연결, 메뉴 관리, 언어 설정 등 앱의 전반적인 설정을 담당하는 화면 컴포저블.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier, vm: MainViewModel) {

    val context = LocalContext.current

    /** -----------------------------
     * UI 상태 및 리소스 변수
     * ----------------------------- */
    var appliedFontScale by remember { mutableFloatStateOf(1.0f) }
    val testPrintText = stringResource(R.string.test_print_text)
    val resetCompleteText = stringResource(R.string.reset_complete)

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        /** -----------------------------
         * 좌측 패널 (프린터 관리 + 메뉴 리스트)
         * ----------------------------- */
        Column(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            /** -----------------------------
             * 프린터 관리 섹션
             * ----------------------------- */
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.printer_management), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    LaunchedEffect(Unit) {
                        vm.loadBluetoothPrinters(context)
                    }

                    // 블루투스 권한 확인 (버전별 분기)
                    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    } else true

                    val printerName = if (hasPermission) {
                        vm.selectedPrinter.value?.name ?: stringResource(R.string.printer_not_connected)
                    } else stringResource(R.string.printer_not_connected)

                    // 현재 연결 상태 표시
                    val statusText = if (vm.selectedPrinter.value == null) {
                        stringResource(R.string.printer_not_connected)
                    } else {
                        "${stringResource(R.string.printer_management)}: $printerName"
                    }

                    Text(statusText, style = MaterialTheme.typography.bodyMedium)
                    
                    Spacer(Modifier.height(12.dp))

                    // 페어링된 블루투스 기기 목록
                    vm.bluetoothPrinters.forEach { device ->
                        val deviceName = try {
                            if (hasPermission) device.name ?: device.address else device.address
                        } catch (_: SecurityException) {
                            device.address
                        }
                        
                        Text(
                            text = deviceName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.selectPrinter(context, device) }
                                .padding(vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // 테스트 인쇄 버튼
                    Button(
                        onClick = {
                            val printer = vm.selectedPrinter.value
                            if (printer != null) {
                                BluetoothPrinter(context).printToDevice(printer, testPrintText)
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.print_test_order))
                    }
                }
            }

            /** -----------------------------
             * 등록된 메뉴 리스트 섹션
             * ----------------------------- */
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.menu_list), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    LazyColumn {
                        items(vm.menuItems) { item ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                    Text("£${item.price}", style = MaterialTheme.typography.bodySmall)
                                }
                                Button(onClick = { vm.removeMenu(context, item) }) { Text(stringResource(R.string.delete)) }
                            }
                        }
                    }
                }
            }
        }

        /** -----------------------------
         * 우측 패널 (메뉴 추가 + 앱 설정)
         * ----------------------------- */
        LazyColumn(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            /** -----------------------------
             * 메뉴 추가 섹션
             * ----------------------------- */
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.add_menu), style = MaterialTheme.typography.titleMedium)

                        var name by remember { mutableStateOf("") }
                        var price by remember { mutableStateOf("") }
                        var allergy by remember { mutableStateOf("") }
                        var optionText by remember { mutableStateOf("") }
                        val options = remember { mutableStateListOf<String>() }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.menu_name)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it.filter(Char::isDigit) },
                            label = { Text(stringResource(R.string.price_currency)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 알러지 목록 정의
                        val allergyItems = listOf(
                            stringResource(R.string.allergy_milk),
                            stringResource(R.string.allergy_egg),
                            stringResource(R.string.allergy_peanut),
                            stringResource(R.string.allergy_soy),
                            stringResource(R.string.allergy_wheat),
                            stringResource(R.string.allergy_crustacean),
                            stringResource(R.string.allergy_fish),
                            stringResource(R.string.allergy_nut),
                            stringResource(R.string.allergy_sesame)
                        )
                        var showAllergyDialog by remember { mutableStateOf(false) }
                        val selectedAllergies = remember { mutableStateListOf<String>() }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = allergy,
                                onValueChange = {},
                                label = { Text(stringResource(R.string.allergy_info)) },
                                modifier = Modifier.weight(1f),
                                readOnly = true
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { showAllergyDialog = true }) { Text(stringResource(R.string.select)) }
                        }

                        // 알러지 선택 다이얼로그
                        if (showAllergyDialog) {
                            AlertDialog(
                                onDismissRequest = { showAllergyDialog = false },
                                title = { Text(stringResource(R.string.allergy_selection)) },
                                text = {
                                    Column(
                                        modifier = Modifier
                                            .heightIn(max = 400.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        allergyItems.forEach { item ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    if (selectedAllergies.contains(item)) selectedAllergies.remove(item)
                                                    else selectedAllergies.add(item)
                                                }
                                            ) {
                                                Checkbox(
                                                    checked = selectedAllergies.contains(item),
                                                    onCheckedChange = { checked ->
                                                        if (checked) selectedAllergies.add(item)
                                                        else selectedAllergies.remove(item)
                                                    }
                                                )
                                                Text(item)
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        allergy = selectedAllergies.joinToString(", ")
                                        showAllergyDialog = false
                                    }) { Text(stringResource(R.string.confirm)) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAllergyDialog = false }) { Text(stringResource(R.string.cancel)) }
                                }
                            )
                        }

                        // 옵션 추가
                        Row {
                            OutlinedTextField(
                                value = optionText,
                                onValueChange = { optionText = it },
                                label = { Text(stringResource(R.string.add_option)) },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = {
                                if (optionText.isNotBlank()) {
                                    options.add(optionText)
                                    optionText = ""
                                }
                            }) { Text(stringResource(R.string.add)) }
                        }

                        if (options.isNotEmpty()) {
                            Text(stringResource(R.string.options_prefix, options.joinToString()), style = MaterialTheme.typography.bodySmall)
                        }

                        // 메뉴 저장 버튼
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = {
                                val p = price.toIntOrNull()
                                if (name.isNotBlank() && p != null) {
                                    vm.addMenu(context, name, p, allergy, options.toList())
                                    name = ""; price = ""; allergy = ""; options.clear()
                                }
                            }) { Text(stringResource(R.string.save_menu)) }
                        }
                    }
                }
            }

            /** -----------------------------
             * 시스템 설정 섹션 (언어 및 테이블)
             * ----------------------------- */
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.menu_settings), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))

                        // 언어 설정 전환
                        Text(stringResource(R.string.language_settings), style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { vm.setLanguage(context, "ko") },
                                label = { Text(stringResource(R.string.lang_ko)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (vm.currentLanguage.value == "ko") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            )
                            AssistChip(
                                onClick = { vm.setLanguage(context, "en") },
                                label = { Text(stringResource(R.string.lang_en)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (vm.currentLanguage.value == "en") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // 테이블 개수 설정 (NumberPicker)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.table_count))
                            Spacer(Modifier.width(12.dp))
                            AndroidView(
                                factory = { ctx ->
                                    NumberPicker(ctx).apply {
                                        minValue = 1
                                        maxValue = 99
                                        value = vm.tableCount.value
                                        setOnValueChangedListener { _, _, newVal ->
                                            vm.updateTableCount(context, newVal)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            /** -----------------------------
             * 데이터 관리 섹션 (초기화)
             * ----------------------------- */
            item {
                var showResetDialog by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.system_management), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { showResetDialog = true }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)) {
                            Text(stringResource(R.string.reset_all_data))
                        }
                    }
                }

                // 초기화 확인 다이얼로그
                if (showResetDialog) {
                    AlertDialog(
                        onDismissRequest = { showResetDialog = false },
                        title = { Text(stringResource(R.string.reset_dialog_title)) },
                        text = { Text(stringResource(R.string.reset_dialog_message)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    vm.resetAllData(context) {
                                        Toast.makeText(context, resetCompleteText, Toast.LENGTH_SHORT).show()
                                    }
                                    showResetDialog = false
                                }
                            ) {
                                Text(stringResource(R.string.reset_confirm), color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.cancel)) }
                        }
                    )
                }
            }
        }
    }
}
