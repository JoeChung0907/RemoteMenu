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
 * 선택된 블루투스 기기로 데이터를 전송하여 인쇄를 수행하는 클래스.
 */
class BluetoothPrinter(private val context: Context) {

    // 표준 시리얼 포트 프로파일(SPP) UUID
    private val printerUUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /** -----------------------------
     * 블루투스 출력 실행
     * ----------------------------- */
    fun printToDevice(device: BluetoothDevice, text: String) {

        /** -----------------------------
         * 안드로이드 버전별 권한 체크
         * ----------------------------- */
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
            Log.i("BluetoothPrinter", "Connecting to device...")
            
            // RFCOMM 소켓 생성 및 연결
            socket = device.createRfcommSocketToServiceRecord(printerUUID)
            socket.connect()

            val outputStream = socket.outputStream

            /** -----------------------------
             * 데이터 전송 (UTF-8 인코딩)
             * ----------------------------- */
            outputStream.write(text.toByteArray(Charsets.UTF_8))
            
            // 용지 배출을 위한 여백 추가
            outputStream.write("\n\n\n".toByteArray())
            outputStream.flush()

            Log.i("BluetoothPrinter", "Printed successfully")

        } catch (e: IOException) {
            Log.e("BluetoothPrinter", "Printing failed: ${e.message}")
            e.printStackTrace()

        } finally {
            /** -----------------------------
             * 자원 해제 (소켓 닫기)
             * ----------------------------- */
            try {
                socket?.close()
                Log.i("BluetoothPrinter", "Socket closed")
            } catch (_: IOException) {}
        }
    }
}
