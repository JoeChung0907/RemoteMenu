package com.remotemenu.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.util.UUID

class BluetoothPrinter {

    // 대부분의 블루투스 프린터가 사용하는 기본 SPP UUID
    private val printerUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun print(text: String) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            println("Bluetooth is disabled or not available")
            return
        }

        // 이미 페어링된 기기 목록에서 프린터 찾기
        val pairedDevices: Set<BluetoothDevice> = adapter.bondedDevices
        val printerDevice = pairedDevices.firstOrNull {
            // 프린터 이름이 "Printer", "POS", "BT" 등일 가능성이 높음
            it.name.contains("POS", ignoreCase = true) ||
                    it.name.contains("Printer", ignoreCase = true) ||
                    it.name.contains("BT", ignoreCase = true)
        }

        if (printerDevice == null) {
            println("No paired Bluetooth printer found")
            return
        }

        var socket: BluetoothSocket? = null

        try {
            // 소켓 연결
            socket = printerDevice.createRfcommSocketToServiceRecord(printerUUID)
            socket.connect()

            val outputStream = socket.outputStream

            // 텍스트 전송
            outputStream.write(text.toByteArray(Charsets.UTF_8))

            // 프린터 줄바꿈 (필수)
            outputStream.write("\n\n\n".toByteArray())

            outputStream.flush()

            println("Printed successfully")

        } catch (e: IOException) {
            e.printStackTrace()
            println("Printing failed: ${e.message}")

        } finally {
            try {
                socket?.close()
            } catch (_: IOException) {}
        }
    }
}
