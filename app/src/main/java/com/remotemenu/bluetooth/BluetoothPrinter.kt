package com.remotemenu.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.Charset
import java.util.UUID

/**
 * BluetoothPrinter
 * 영국 Epson 프린터 규격에 맞춰 인코딩(windows-1252) 및 여백을 최적화합니다.
 */
class BluetoothPrinter(private val context: Context) {

    private val printerUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    suspend fun printToDevice(device: BluetoothDevice, text: String) = withContext(Dispatchers.IO) {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true

        if (!hasPermission) return@withContext

        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(printerUUID)
            socket.connect()

            socket.outputStream.use { stream ->
                // 영국/유럽 Epson 프린터 표준 인코딩
                val charset = Charset.forName("windows-1252")
                
                /** -----------------------------
                 * ESC/POS 명령어 설정 (Epson 전용)
                 * ----------------------------- */
                val ESC_INIT = byteArrayOf(0x1B, 0x40)               // 프린터 초기화
                val SELECT_WPC1252 = byteArrayOf(0x1B, 0x74, 0x10)  // 서구권 문자표(WPC1252) 선택
                val FONT_SIZE_LARGE = byteArrayOf(0x1D, 0x21, 0x11)    // 2배 확대
                val FONT_SIZE_NORMAL = byteArrayOf(0x1D, 0x21, 0x00)   // 일반 크기
                
                // 1. 초기화 및 문자표 설정 (파운드 기호 호환성 확보)
                stream.write(ESC_INIT)
                stream.write(SELECT_WPC1252)
                
                // 2. 글자 크기 설정 및 본문 인쇄
                stream.write(FONT_SIZE_LARGE)
                stream.write(text.toByteArray(charset))
                
                // 3. 설정 복원
                stream.write(FONT_SIZE_NORMAL)
                
                /** -----------------------------
                 * 영수증 길이 확보 (10줄 여백)
                 * ----------------------------- */
                val tailPadding = "\n".repeat(10)
                stream.write(tailPadding.toByteArray(charset))
                
                stream.flush()
            }
            Log.i("BluetoothPrinter", "Printed successfully to ${device.name ?: device.address}")
        } catch (e: IOException) {
            Log.e("BluetoothPrinter", "Printing failed: ${e.message}")
        } finally {
            try { socket?.close() } catch (_: IOException) {}
        }
    }
}
