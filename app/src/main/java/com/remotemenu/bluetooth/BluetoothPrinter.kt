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
import java.util.UUID

/**
 * BluetoothPrinter
 * 저사양 기기 대응을 위해 모든 네트워크/IO 작업을 IO 쓰레드에서 처리합니다.
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
            socket.connect() // blocking call

            socket.outputStream.use { stream ->
                stream.write(text.toByteArray(Charsets.UTF_8))
                stream.write("\n\n\n".toByteArray())
                stream.flush()
            }
            Log.i("BluetoothPrinter", "Printed successfully to ${device.address}")
        } catch (e: IOException) {
            Log.e("BluetoothPrinter", "Printing failed: ${e.message}")
        } finally {
            try { socket?.close() } catch (_: IOException) {}
        }
    }
}
