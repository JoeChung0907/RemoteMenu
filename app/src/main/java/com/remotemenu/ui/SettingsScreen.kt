package com.remotemenu.ui

import android.widget.NumberPicker
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
import com.remotemenu.bluetooth.BluetoothPrinter

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, vm: MainViewModel) {

    // 접근성 상태 (미리보기 / 적용 분리)
    var previewDarkMode by remember { mutableStateOf(false) }
    var appliedDarkMode by remember { mutableStateOf(false) }

    var previewFontScale by remember { mutableStateOf(1.0f) }
    var appliedFontScale by remember { mutableStateOf(1.0f) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ---------------------------------------------------------
        // 좌측 패널 : 메뉴 리스트 + 프린터 관리
        // ---------------------------------------------------------
        Column(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 메뉴 리스트
            Card(
                modifier = Modifier.wrapContentSize().fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("메뉴 리스트", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 600.dp) // 300 → 600
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
                                Button(onClick = { vm.removeMenu(item) }) { Text("삭제") }
                            }
                        }
                    }
                }
            }
            // ---------------------------------------------------------
            // 촤측 상단 패널 : 프린터 관리 메뉴
            // ---------------------------------------------------------
            Card(
                modifier = Modifier
                    .wrapContentSize()
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("프린터 관리", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    val context = LocalContext.current

                    //
                    LaunchedEffect(Unit) {
                        vm.loadBluetoothPrinters(context)
                    }

                    // 연결 상태 표시 (아직 선택 기능 없으니 기본값)
                    val printerName = vm.selectedPrinter.value?.name ?: "연결 안됨"

                    Text(
                        "프린터 연결 상태: $printerName",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(12.dp))

                    //  페어링된 프린터 목록 표시
                    Text("페어링된 프린터 목록", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))

                    if (vm.bluetoothPrinters.isEmpty()) {
                        Text("검색된 프린터 없음")
                    } else {
                        vm.bluetoothPrinters.forEach { device ->
                            Text(
                                text = device.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vm.selectPrinter(device)
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
                            bp.printToDevice(printer, "테스트 출력입니다.")
                        } }) {
                        Text("주문 출력")
                    }
                }
            }

            Spacer(Modifier.weight(1f))
        }

        // ---------------------------------------------------------
        // 우측 패널 : 메뉴 추가 + 메뉴 설정 + 접근성
        // ---------------------------------------------------------
        LazyColumn(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // -----------------------------
            // 1) 메뉴 추가 (초기 버전 그대로 + 폰트 적용만)
            // -----------------------------
            item {
                Card(
                    modifier = Modifier.wrapContentSize().fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        Text("메뉴 추가", style = MaterialTheme.typography.titleMedium)

                        var name by remember { mutableStateOf("") }
                        var price by remember { mutableStateOf("") }
                        var allergy by remember { mutableStateOf("") }
                        var optionText by remember { mutableStateOf("") }
                        val options = remember { mutableStateListOf<String>() }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("메뉴 이름") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it.filter(Char::isDigit) },
                            label = { Text("가격 (통화 : £ )") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        val allergyItems = listOf("우유","계란","땅콩","대두","밀","갑각류","생선","견과류","참깨")
                        var showAllergyDialog by remember { mutableStateOf(false) }
                        val selectedAllergies = remember { mutableStateListOf<String>() }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = allergy,
                                onValueChange = {},
                                label = { Text("알러지 정보") },
                                modifier = Modifier.weight(1f),
                                readOnly = true
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { showAllergyDialog = true }) { Text("선택") }
                        }

                        if (showAllergyDialog) {
                            AlertDialog(
                                onDismissRequest = { showAllergyDialog = false },
                                title = { Text("알러지 선택") },
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
                                    }) { Text("확인") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAllergyDialog = false }) { Text("취소") }
                                }
                            )
                        }

                        Row {
                            OutlinedTextField(
                                value = optionText,
                                onValueChange = { optionText = it },
                                label = { Text("옵션 추가") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = {
                                if (optionText.isNotBlank()) {
                                    options.add(optionText)
                                    optionText = ""
                                }
                            }) { Text("추가") }
                        }

                        if (options.isNotEmpty()) {
                            Text("옵션: ${options.joinToString()}", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = {
                                val p = price.toIntOrNull()
                                if (name.isNotBlank() && p != null) {
                                    vm.addMenu(name, p, allergy, options.toList())
                                    name = ""; price = ""; allergy = ""; options.clear()
                                }
                            }) { Text("메뉴 저장") }
                        }
                    }
                }
            }

            // -----------------------------
            // 2) 메뉴 설정
            // -----------------------------
            item {
                // 분류 / 테이블 개수 : 미리보기 / 적용 분리
                var appliedSort by remember { mutableStateOf("이름순") }
                var previewSort by remember { mutableStateOf(appliedSort) }

                var appliedTableCount by remember { mutableStateOf(vm.tableCount.value) }
                var previewTableCount by remember { mutableStateOf(appliedTableCount) }

                Card(
                    modifier = Modifier.wrapContentSize().fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("메뉴 설정", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))

                        // 분류 기준 텍스트
                        Text("분류 기준", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))

                        val sortOptions = listOf("이름순", "가격순", "최근 추가순")

                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sortOptions.forEach { opt ->
                                AssistChip(
                                    onClick = {when (appliedSort) {
                                        "이름순" -> vm.menuItems.sortBy { it.name }
                                        "가격순" -> vm.menuItems.sortBy { it.price }
                                        "최근 추가순" -> vm.menuItems.sortByDescending { it.id }
                                    }
                                        previewSort = opt },

                                    label = { Text(opt) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor =
                                            if (appliedSort == opt) // 현재 분류방식 기준으로 색 입힘
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("현재 분류 방식: $appliedSort", style = MaterialTheme.typography.bodySmall)

                        Spacer(Modifier.height(12.dp))

                        // 테이블 개수 (미리보기 / 적용 분리)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("테이블 개수")
                            Spacer(Modifier.width(12.dp))
                            AndroidView(
                                factory = { ctx ->
                                    NumberPicker(ctx).apply {
                                        minValue = 1
                                        maxValue = 99
                                        value = previewTableCount
                                        setOnValueChangedListener { _, _, newVal ->
                                            previewTableCount = newVal // 미리보기만 변경
                                        }
                                    }
                                }
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("현재 테이블 개수: $appliedTableCount", style = MaterialTheme.typography.bodySmall)

                        Spacer(Modifier.height(12.dp))

                        // 적용 버튼 : 이걸 눌러야만 실제 변경
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(onClick = {
                                appliedSort = previewSort
                                appliedTableCount = previewTableCount
                                vm.tableCount.value = appliedTableCount
                                // 분류 방식은 실제 정렬 로직에서 appliedSort 사용하면 됨
                            }) {
                                Text("적용")
                            }
                        }
                    }
                }
            }

            // -----------------------------
            // 3) 접근성
            // -----------------------------
            if (false) { // 비활성화 감싸기
            item {
                Card(
                    modifier = Modifier.wrapContentSize().fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("접근성", style = MaterialTheme.typography.titleMedium)

                        // 다크모드 (미리보기 / 적용 분리)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("다크모드")
                            Switch(
                                checked = previewDarkMode,
                                onCheckedChange = { previewDarkMode = it } // 미리보기만 변경
                            )
                        }
                        Text(
                            "현재 다크모드: ${if (appliedDarkMode) "켜짐" else "꺼짐"}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        // 글자 크기 슬라이더 (미리보기 / 적용 분리)
                        Text("글자 크기 조절")
                        Slider(
                            value = previewFontScale,
                            onValueChange = { previewFontScale = it },
                            valueRange = 0.8f..1.6f
                        )

                        // 실시간 미리보기 (미리보기 기준)
                        Text(
                            "예시 텍스트 미리보기",
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize * previewFontScale
                        )

                        Text(
                            "현재 적용된 글자 크기 배율: x${"%.2f".format(appliedFontScale)}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        // 적용 버튼 : 다크모드 + 폰트 크기 둘 다 적용
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    appliedDarkMode = previewDarkMode
                                    appliedFontScale = previewFontScale

                                }
                            ) {
                                Text("적용")
                            }
                        }
                    }
                }
            }
            } // 감싸는 부분
        }
    }
}
