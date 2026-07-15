package com.remotemenu.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import java.io.IOException
import java.util.UUID

/**
 * BluetoothPrinter
 * - 실제 블루투스 프린터로 텍스트를 출력하는 기능만 담당하는 클래스
 * - 프린터 목록 탐색/선택은 MainViewModel에서 처리함
 * - 이 클래스는 "선택된 프린터"를 받아서 출력만 수행함
 */
class BluetoothPrinter(private val context: Context) {

    // 대부분의 블루투스 프린터가 사용하는 기본 SPP(UUID)
    private val printerUUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /**
     * 선택된 BluetoothDevice로 텍스트 출력
     * @param device 사용자가 선택한 프린터(BluetoothDevice)
     * @param text 출력할 문자열
     */
    fun printToDevice(device: BluetoothDevice, text: String) {

        // Android 12+ 권한 체크
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.e("BluetoothPrinter", "BLUETOOTH_CONNECT permission missing")
            return
        }

        var socket: BluetoothSocket? = null

        try {
            // 선택된 프린터와 RFCOMM 소켓 연결
            socket = device.createRfcommSocketToServiceRecord(printerUUID)
            socket.connect()

            val outputStream = socket.outputStream

            // 텍스트 전송
            outputStream.write(text.toByteArray(Charsets.UTF_8))

            // 프린터는 마지막 줄이 잘리는 경우가 많아서 줄바꿈 추가
            outputStream.write("\n\n\n".toByteArray())

            outputStream.flush()

            Log.i("BluetoothPrinter", "Printed successfully")

        } catch (e: IOException) {
            Log.e("BluetoothPrinter", "Printing failed: ${e.message}")
            e.printStackTrace()

        } finally {
            // 소켓 닫기 (메모리 누수 방지)
            try {
                socket?.close()
            } catch (_: IOException) {}
        }
    }
}
