package com.remotemenu.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.Charset
import java.util.UUID

/**
 * BluetoothPrinter
 * 연속 인쇄 및 장시간 데이터 전송 시의 안정성을 극대화한 버전입니다.
 */
class BluetoothPrinter(private val context: Context) {

    private val printerUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /**
     * printToDevice
     * 지정된 기기로 데이터를 전송합니다. 리플렉션 및 단계별 소켓 생성 로직을 포함합니다.
     */
    suspend fun printToDevice(device: BluetoothDevice, text: String) = withContext(Dispatchers.IO) {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true

        if (!hasPermission) throw SecurityException("Bluetooth permission denied (BLUETOOTH_CONNECT)")

        var socket: BluetoothSocket? = null
        
        try {
            // 1. 소켓 생성 (3단계 전략)
            socket = try {
                device.createRfcommSocketToServiceRecord(printerUUID)
            } catch (e: Exception) {
                try {
                    device.createInsecureRfcommSocketToServiceRecord(printerUUID)
                } catch (e2: Exception) {
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    method.invoke(device, 1) as BluetoothSocket
                }
            }

            // 2. 연결 및 안정화 (충분한 시간 부여)
            socket?.connect()
            delay(200) 

            val stream = socket?.outputStream ?: throw IOException("Could not open output stream")
            val charset = Charset.forName("windows-1252")
            
            // 3. 프린터 설정 및 초기화
            val escInit = byteArrayOf(0x1B, 0x40)
            val selectWpc1252 = byteArrayOf(0x1B, 0x74, 0x10)
            val fontSizeLarge = byteArrayOf(0x1D, 0x21, 0x11)
            val fontSizeNormal = byteArrayOf(0x1D, 0x21, 0x00)
            
            stream.write(escInit)
            stream.write(selectWpc1252)
            stream.write(fontSizeLarge)

            // 4. 본문 전송 (Chunking 최적화)
            val fullBytes = text.toByteArray(charset)
            val chunkSize = 256
            var offset = 0
            while (offset < fullBytes.size) {
                val count = Math.min(chunkSize, fullBytes.size - offset)
                stream.write(fullBytes, offset, count)
                offset += count
                delay(40) // 버퍼 안정화를 위한 지연
            }

            // 5. 마감 처리
            stream.write(fontSizeNormal)
            stream.write(byteArrayOf(0x1B, 0x64, 0x06)) // Paper Feed
            stream.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // Paper Cut
            
            stream.flush()
            delay(500) // 하드웨어 처리 대기

        } catch (e: Exception) {
            throw IOException("Printing failed: ${e.message}")
        } finally {
            // 소켓 및 스트림 완벽 정리
            try {
                socket?.outputStream?.close()
                socket?.close()
            } catch (_: Exception) {}
        }
    }
}
