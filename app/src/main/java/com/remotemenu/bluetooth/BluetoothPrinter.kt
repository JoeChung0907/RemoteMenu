package com.remotemenu.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.UUID

/**
 * BluetoothPrinter
 * 선택된 BluetoothDevice로 텍스트를 출력하는 기능만 담당.
 */
class BluetoothPrinter(private val context: Context) {

    private val printerUUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /** -----------------------------
     * 선택된 BluetoothDevice로 텍스트 출력
     * ----------------------------- */
    fun printToDevice(device: BluetoothDevice, text: String) {

        // Android 12+ 런타임 권한 체크 (안드로이드 10 이하는 무조건 true)
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!hasPermission) {
            Log.e("BluetoothPrinter", "BLUETOOTH_CONNECT permission missing")
            return
        }

        var socket: BluetoothSocket? = null

        try {
            Log.i("BluetoothPrinter", "Starting connection to ${device.name ?: device.address}")
            
            // RFCOMM 소켓 연결 시도
            socket = device.createRfcommSocketToServiceRecord(printerUUID)
            socket.connect()

            val outputStream = socket.outputStream

            // 텍스트 전송 (EUC-KR 인코딩 권장 - 한글 프린터일 경우)
            // 영어만 쓸 경우 UTF-8도 무관하지만, 프린터 호환성을 위해 체크 필요
            outputStream.write(text.toByteArray(Charsets.UTF_8))
            outputStream.write("\n\n\n".toByteArray())
            outputStream.flush()

            Log.i("BluetoothPrinter", "Printed successfully: $text")

        } catch (e: IOException) {
            Log.e("BluetoothPrinter", "Printing failed: ${e.message}")
            e.printStackTrace()

        } finally {
            try {
                socket?.close()
                Log.i("BluetoothPrinter", "Socket closed")
            } catch (_: IOException) {}
        }
    }
}
