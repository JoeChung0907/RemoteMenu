package com.remotemenu.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.NumberPicker
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.remotemenu.MainViewModel
import com.remotemenu.R

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, vm: MainViewModel) {

    val context = LocalContext.current
    val resetCompleteText = stringResource(R.string.reset_complete)
    val importSuccessText = stringResource(R.string.import_success)
    val importErrorText = stringResource(R.string.import_error)
    
    var showPrintConfirmOverlay by remember { mutableStateOf(false) }
    var showAllergyOverlay by remember { mutableStateOf(false) }
    var showResetOverlay by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            /** -----------------------------
             * 좌측 패널: 프린터 관리 및 메뉴 목록
             * ----------------------------- */
            Column(
                modifier = Modifier.weight(0.45f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.printer_management), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))

                        LaunchedEffect(Unit) { vm.loadBluetoothPrinters(context) }

                        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                        } else true

                        val selectedCount = vm.selectedPrinters.size
                        val statusText = if (selectedCount == 0) stringResource(R.string.printer_not_connected)
                        else "${stringResource(R.string.printer_management)}: $selectedCount"

                        Text(statusText, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))

                        vm.bluetoothPrinters.forEach { device ->
                            val isSelected = vm.selectedPrinters.any { it.address == device.address }
                            val deviceName = try { if (hasPermission) device.name ?: device.address else device.address } catch (_: SecurityException) { device.address }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { vm.togglePrinter(context, device) }.padding(vertical = 4.dp)
                            ) {
                                Checkbox(checked = isSelected, onCheckedChange = { vm.togglePrinter(context, device) })
                                Text(text = deviceName, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = { showPrintConfirmOverlay = true },
                            modifier = Modifier.align(Alignment.End),
                            enabled = vm.selectedPrinters.isNotEmpty()
                        ) {
                            Text(stringResource(R.string.test_print_button))
                        }
                    }
                }

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
                modifier = Modifier.weight(0.55f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                                onValueChange = { input -> if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) price = input },
                                label = { Text(stringResource(R.string.price_currency)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = allergy,
                                onValueChange = {},
                                label = { Text(stringResource(R.string.allergy_info)) },
                                modifier = Modifier.fillMaxWidth().clickable { showAllergyOverlay = true },
                                readOnly = true,
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = optionText,
                                    onValueChange = { optionText = it },
                                    label = { Text(stringResource(R.string.add_option)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = { if (optionText.isNotBlank()) { options.add(optionText); optionText = "" } }) { Text(stringResource(R.string.add)) }
                            }

                            if (options.isNotEmpty()) {
                                Text(stringResource(R.string.options_prefix, options.joinToString()), style = MaterialTheme.typography.bodySmall)
                            }

                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = {
                                    val p = price.toDoubleOrNull()
                                    if (name.isNotBlank() && p != null) {
                                        vm.addMenu(context, "General", name, p, allergy, options.toList())
                                        name = ""; price = ""; allergy = ""; options.clear()
                                    }
                                }) { Text(stringResource(R.string.save_menu)) }
                            }
                            
                            if (showAllergyOverlay) {
                                val allergyItems = listOf(R.string.allergy_milk, R.string.allergy_egg, R.string.allergy_peanut, R.string.allergy_soy, R.string.allergy_wheat, R.string.allergy_crustacean, R.string.allergy_fish, R.string.allergy_nut, R.string.allergy_sesame).map { stringResource(it) }
                                val selected = remember { mutableStateListOf<String>().apply { if (allergy.isNotEmpty()) addAll(allergy.split(", ")) } }
                                
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { /* block */ }, contentAlignment = Alignment.Center) {
                                    Card(Modifier.fillMaxWidth(0.7f).padding(16.dp)) {
                                        Column(Modifier.padding(16.dp)) {
                                            Text(stringResource(R.string.allergy_selection), style = MaterialTheme.typography.titleMedium)
                                            Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                                                allergyItems.forEach { item ->
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { if (selected.contains(item)) selected.remove(item) else selected.add(item) }) {
                                                        Checkbox(checked = selected.contains(item), onCheckedChange = null)
                                                        Text(item)
                                                    }
                                                }
                                            }
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                TextButton(onClick = { showAllergyOverlay = false }) { Text(stringResource(R.string.cancel)) }
                                                Button(onClick = { allergy = selected.joinToString(", "); showAllergyOverlay = false }) { Text(stringResource(R.string.confirm)) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.bulk_import), style = MaterialTheme.typography.titleMedium)
                            var jsonText by remember { mutableStateOf("") }
                            OutlinedTextField(value = jsonText, onValueChange = { jsonText = it }, label = { Text(stringResource(R.string.import_json_hint)) }, modifier = Modifier.fillMaxWidth().height(150.dp), textStyle = MaterialTheme.typography.bodySmall)
                            Button(onClick = { val count = vm.importMenus(context, jsonText); if (count >= 0) { Toast.makeText(context, importSuccessText.format(count), Toast.LENGTH_SHORT).show(); jsonText = "" } else { Toast.makeText(context, importErrorText, Toast.LENGTH_SHORT).show() } }, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.import_button)) }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.menu_settings), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.language_settings), style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("ko" to R.string.lang_ko, "en" to R.string.lang_en).forEach { (code, res) ->
                                    FilterChip(selected = vm.currentLanguage.value == code, onClick = { vm.setLanguage(context, code) }, label = { Text(stringResource(res)) })
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.table_count))
                                Spacer(Modifier.width(12.dp))
                                AndroidView(factory = { ctx -> NumberPicker(ctx).apply { minValue = 1; maxValue = 99; value = vm.tableCount.value; setOnValueChangedListener { _, _, newVal -> vm.updateTableCount(context, newVal) } } })
                            }
                        }
                    }
                }

                item {
                    Button(onClick = { showResetOverlay = true }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.reset_all_data)) }
                }
            }
        }

        /** -----------------------------
         * 오버레이들 (In-screen Dialogs)
         * ----------------------------- */
        if (showPrintConfirmOverlay) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { /* block */ }, contentAlignment = Alignment.Center) {
                Card(Modifier.fillMaxWidth(0.6f).padding(16.dp)) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.test_print_button), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp)); Text(stringResource(R.string.confirm)); Spacer(Modifier.height(24.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showPrintConfirmOverlay = false }) { Text(stringResource(R.string.cancel)) }
                            Button(onClick = { vm.printTest(context); showPrintConfirmOverlay = false }, modifier = Modifier.padding(start = 8.dp)) { Text(stringResource(R.string.confirm)) }
                        }
                    }
                }
            }
        }

        if (showResetOverlay) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { /* block */ }, contentAlignment = Alignment.Center) {
                Card(Modifier.fillMaxWidth(0.6f).padding(16.dp)) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.reset_dialog_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp)); Text(stringResource(R.string.reset_dialog_message)); Spacer(Modifier.height(24.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showResetOverlay = false }) { Text(stringResource(R.string.cancel)) }
                            Button(onClick = { vm.resetAllData(context) { Toast.makeText(context, resetCompleteText, Toast.LENGTH_SHORT).show() }; showResetOverlay = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.padding(start = 8.dp)) { Text(stringResource(R.string.reset_confirm)) }
                        }
                    }
                }
            }
        }
    }
}
