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

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, vm: MainViewModel) {

    val context = LocalContext.current

    /** -----------------------------
     * UI 상태 및 리소스 변수 (경고 해결을 위해 상단 배치)
     * ----------------------------- */
    var appliedFontScale by remember { mutableFloatStateOf(1.0f) }
    
    // onClick 내에서 context.getString() 사용 경고를 피하기 위해 미리 선언
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
             * 프린터 관리
             * ----------------------------- */
            Card(
                modifier = Modifier
                    .wrapContentSize()
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.printer_management), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    LaunchedEffect(Unit) {
                        vm.loadBluetoothPrinters(context)
                    }

                    // Android 12 이상일 경우에만 BLUETOOTH_CONNECT 권한 체크
                    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }

                    val printerName = if (hasPermission) {
                        vm.selectedPrinter.value?.name ?: stringResource(R.string.printer_not_connected)
                    } else {
                        stringResource(R.string.printer_not_connected)
                    }

                    Text(
                        stringResource(R.string.printer_status, printerName),
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(stringResource(R.string.paired_printers), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))

                    if (vm.bluetoothPrinters.isEmpty()) {
                        Text(stringResource(R.string.no_printers_found))
                    } else {
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
                                    .clickable {
                                        vm.selectPrinter(context, device)
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(onClick = {
                        val printer = vm.selectedPrinter.value
                        if (printer != null) {
                            val bp = BluetoothPrinter(context)
                            bp.printToDevice(printer, testPrintText)
                        } }) {
                        Text(stringResource(R.string.print_test_order))
                    }
                }
            }

            /** -----------------------------
             * 메뉴 리스트 (생략 - 기존 로직 유지)
             * ----------------------------- */
            Card(
                modifier = Modifier.wrapContentSize().fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.menu_list), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 600.dp)
                    ) {
                        items(vm.menuItems) { item ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, fontSize = MaterialTheme.typography.bodyMedium.fontSize * appliedFontScale)
                                    Text(
                                        "£${item.price}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize * appliedFontScale
                                    )
                                }
                                Button(onClick = { vm.removeMenu(context, item) }) { Text(stringResource(R.string.delete)) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
        }

        /** -----------------------------
         * 우측 패널 (메뉴 추가 + 설정 + 초기화)
         * ----------------------------- */
        LazyColumn(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            /** 메뉴 추가 로직 (기존 유지) */
            item {
                Card(
                    modifier = Modifier.wrapContentSize().fillMaxWidth(),
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

                        if (showAllergyDialog) {
                            AlertDialog(
                                onDismissRequest = { showAllergyDialog = false },
                                title = { Text(stringResource(R.string.allergy_selection)) },
                                text = {
                                    Column {
                                        allergyItems.forEach { item ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
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

            /** 메뉴 설정 (기존 유지) */
            item {
                val sortName = stringResource(R.string.sort_name)
                val sortPrice = stringResource(R.string.sort_price)
                val sortRecent = stringResource(R.string.sort_recent)
                
                var appliedSort by remember { mutableStateOf(sortName) }
                var previewSort by remember { mutableStateOf(appliedSort) }

                var appliedTableCount by remember { mutableIntStateOf(vm.tableCount.value) }
                var previewTableCount by remember { mutableIntStateOf(appliedTableCount) }

                Card(
                    modifier = Modifier.wrapContentSize().fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.menu_settings), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.sort_criteria), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))

                        val sortOptions = listOf(sortName, sortPrice, sortRecent)
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sortOptions.forEach { opt ->
                                AssistChip(
                                    onClick = {
                                        when (opt) {
                                            sortName -> vm.menuItems.sortBy { it.name }
                                            sortPrice -> vm.menuItems.sortBy { it.price }
                                            sortRecent -> vm.menuItems.sortByDescending { it.id }
                                        }
                                        previewSort = opt
                                    },
                                    label = { Text(opt) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (appliedSort == opt) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.current_sort_format, appliedSort), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.table_count))
                            Spacer(Modifier.width(12.dp))
                            AndroidView(
                                factory = { ctx ->
                                    NumberPicker(ctx).apply {
                                        minValue = 1
                                        maxValue = 99
                                        value = previewTableCount
                                        setOnValueChangedListener { _, _, newVal -> previewTableCount = newVal }
                                    }
                                }
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.current_table_count_format, appliedTableCount.toString()), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = {
                                appliedSort = previewSort
                                appliedTableCount = previewTableCount
                                vm.updateTableCount(context, appliedTableCount)
                            }) {
                                Text(stringResource(R.string.apply))
                            }
                        }
                    }
                }
            }

            /** 전체 데이터 초기화 (기존 유지) */
            item {
                var showResetDialog by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.wrapContentSize().fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.system_management), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.reset_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { showResetDialog = true }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)) {
                            Text(stringResource(R.string.reset_all_data))
                        }
                    }
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
