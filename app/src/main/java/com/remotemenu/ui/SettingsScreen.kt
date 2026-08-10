package com.remotemenu.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.NumberPicker
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.remotemenu.MainViewModel
import com.remotemenu.R

/**
 * SettingsScreen
 * 프린터 연결 관리, 메뉴 설정(추가/삭제/가져오기), 언어 및 시스템 관리를 담당하는 설정 화면입니다.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier, vm: MainViewModel) {

    val context = LocalContext.current

    /** -----------------------------
     * 리소스 문자열 변수화 (성능 최적화)
     * ----------------------------- */
    val resetCompleteText = stringResource(R.string.reset_complete)
    val importSuccessText = stringResource(R.string.import_success)
    val importErrorText = stringResource(R.string.import_error)
    
    var showPrintConfirmDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        /** -----------------------------
         * 좌측 패널: 프린터 관리 및 메뉴 목록
         * ----------------------------- */
        Column(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            /**
             * 프린터 관리 섹션
             * 블루투스 권한 체크 및 다중 프린터 선택/테스트 기능을 제공합니다.
             */
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.printer_management), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    // 앱 시작 시 페어링된 기기 목록 자동 로드
                    LaunchedEffect(Unit) {
                        vm.loadBluetoothPrinters(context)
                    }

                    // Android 12 이상 런타임 권한 대응
                    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    } else true

                    // 현재 선택된 프린터 개수 표시
                    val selectedCount = vm.selectedPrinters.size
                    val statusText = if (selectedCount == 0) {
                        stringResource(R.string.printer_not_connected)
                    } else {
                        "${stringResource(R.string.printer_management)}: $selectedCount"
                    }

                    Text(statusText, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))

                    // 기기 목록 렌더링 (체크박스 토글 방식)
                    vm.bluetoothPrinters.forEach { device ->
                        val isSelected = vm.selectedPrinters.contains(device)
                        val deviceName = try {
                            if (hasPermission) device.name ?: device.address else device.address
                        } catch (_: SecurityException) {
                            device.address
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.togglePrinter(context, device) }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { vm.togglePrinter(context, device) }
                            )
                            Text(text = deviceName, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // 테스트 인쇄 버튼
                    Button(
                        onClick = { showPrintConfirmDialog = true },
                        modifier = Modifier.align(Alignment.End),
                        enabled = vm.selectedPrinters.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.test_print_button))
                    }

                    // 인쇄 실행 전 최종 확인 다이얼로그
                    if (showPrintConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showPrintConfirmDialog = false },
                            title = { Text(stringResource(R.string.test_print_button)) },
                            text = { Text(stringResource(R.string.confirm)) },
                            confirmButton = {
                                TextButton(onClick = {
                                    vm.printTest(context)
                                    showPrintConfirmDialog = false
                                }) { Text(stringResource(R.string.confirm)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showPrintConfirmDialog = false }) { Text(stringResource(R.string.cancel)) }
                            }
                        )
                    }
                }
            }

            /**
             * 등록된 메뉴 리스트 섹션
             * 현재 시스템에 등록된 모든 메뉴를 확인하고 개별 삭제할 수 있습니다.
             */
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
                                    Text(stringResource(R.string.price_format, item.price), style = MaterialTheme.typography.bodySmall)
                                }
                                // 메뉴 개별 삭제 아이콘
                                IconButton(onClick = { vm.removeMenu(context, item) }) { 
                                    Icon(Icons.Default.Delete, contentDescription = null) 
                                }
                            }
                        }
                    }
                }
            }
        }

        /** -----------------------------
         * 우측 패널: 메뉴 추가 및 시스템 설정
         * ----------------------------- */
        LazyColumn(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            /**
             * 메뉴 수동 추가 섹션
             */
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
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it.filter(Char::isDigit) },
                            label = { Text(stringResource(R.string.price_currency)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        val allergyItems = listOf(
                            R.string.allergy_milk, R.string.allergy_egg, R.string.allergy_peanut,
                            R.string.allergy_soy, R.string.allergy_wheat, R.string.allergy_crustacean,
                            R.string.allergy_fish, R.string.allergy_nut, R.string.allergy_sesame
                        ).map { stringResource(it) }

                        var showAllergyDialog by remember { mutableStateOf(false) }

                        // 알러지 정보 입력란 (클릭 시 다이얼로그 노출)
                        OutlinedTextField(
                            value = allergy,
                            onValueChange = {},
                            label = { Text(stringResource(R.string.allergy_info)) },
                            modifier = Modifier.weight(1f).clickable { showAllergyDialog = true },
                            readOnly = true,
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        if (showAllergyDialog) {
                            val selected = remember { mutableStateListOf<String>().apply { if (allergy.isNotEmpty()) addAll(allergy.split(", ")) } }
                            AlertDialog(
                                onDismissRequest = { showAllergyDialog = false },
                                title = { Text(stringResource(R.string.allergy_selection)) },
                                text = {
                                    Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                                        allergyItems.forEach { item ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    if (selected.contains(item)) selected.remove(item)
                                                    else selected.add(item)
                                                }
                                            ) {
                                                Checkbox(checked = selected.contains(item), onCheckedChange = null)
                                                Text(item)
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        allergy = selected.joinToString(", ")
                                        showAllergyDialog = false
                                    }) { Text(stringResource(R.string.confirm)) }
                                }
                            )
                        }

                        // 개별 옵션 추가 영역
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = optionText,
                                onValueChange = { optionText = it },
                                label = { Text(stringResource(R.string.add_option)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
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

                        // 메뉴 최종 저장 실행
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

            /**
             * 메뉴 일괄 가져오기 섹션 (JSON 기반)
             */
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.bulk_import), style = MaterialTheme.typography.titleMedium)
                        
                        var jsonText by remember { mutableStateOf("") }
                        
                        OutlinedTextField(
                            value = jsonText,
                            onValueChange = { jsonText = it },
                            label = { Text(stringResource(R.string.import_json_hint)) },
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        
                        Button(
                            onClick = {
                                val count = vm.importMenus(context, jsonText)
                                if (count >= 0) {
                                    Toast.makeText(context, importSuccessText.format(count), Toast.LENGTH_SHORT).show()
                                    jsonText = ""
                                } else {
                                    Toast.makeText(context, importErrorText, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(R.string.import_button))
                        }
                    }
                }
            }

            /**
             * 언어 및 테이블 개수 설정 섹션
             */
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.menu_settings), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))

                        // 언어 전환 토글
                        Text(stringResource(R.string.language_settings), style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ko" to R.string.lang_ko, "en" to R.string.lang_en).forEach { (code, res) ->
                                FilterChip(
                                    selected = vm.currentLanguage.value == code,
                                    onClick = { vm.setLanguage(context, code) },
                                    label = { Text(stringResource(res)) }
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // 테이블 개수 조절 (NumberPicker 사용)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.table_count))
                            Spacer(Modifier.width(12.dp))
                            AndroidView(
                                factory = { ctx ->
                                    NumberPicker(ctx).apply {
                                        minValue = 1; maxValue = 99; value = vm.tableCount.value
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

            /**
             * 시스템 데이터 전체 초기화 섹션 (주의 필요)
             */
            item {
                var showResetDialog by remember { mutableStateOf(false) }
                Button(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.reset_all_data))
                }

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
