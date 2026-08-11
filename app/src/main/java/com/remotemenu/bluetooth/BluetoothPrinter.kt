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
 * 가상 환경 및 특정 하드웨어에서 발생하는 'read ret: -1' 및 소켓 타임아웃 오류를 방지하기 위해
 * 리플렉션 기반의 3단계 소켓 생성 로직과 안정화 지연 시간이 적용되었습니다.
 */
class BluetoothPrinter(private val context: Context) {

    private val printerUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    suspend fun printToDevice(device: BluetoothDevice, text: String) = withContext(Dispatchers.IO) {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true

        if (!hasPermission) throw SecurityException("Bluetooth permission denied (BLUETOOTH_CONNECT)")

        var socket: BluetoothSocket? = null
        
        try {
            /** -----------------------------
             * 1. 3단계 강력 소켓 생성 전략
             * ----------------------------- */
            socket = try {
                // 시도 1: 표준 보안 소켓
                device.createRfcommSocketToServiceRecord(printerUUID)
            } catch (e1: Exception) {
                try {
                    // 시도 2: 표준 비보안 소켓
                    device.createInsecureRfcommSocketToServiceRecord(printerUUID)
                } catch (e2: Exception) {
                    // 시도 3: 리플렉션 기반 직접 소켓 생성 (가상 환경/저가형 기기 최후의 수단)
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    method.invoke(device, 1) as BluetoothSocket
                }
            }

            /** -----------------------------
             * 2. 연결 및 안정화 (Connect & Stabilize)
             * ----------------------------- */
            try {
                socket?.connect()
                // 연결 직후 소켓이 안정화될 때까지 짧은 대기 (read ret: -1 방지)
                delay(200) 
            } catch (e: IOException) {
                throw IOException("Connection failed. Check if the printer is on: ${e.message}")
            }

            val stream = socket?.outputStream ?: throw IOException("Could not open output stream")
            val charset = Charset.forName("windows-1252")
            
            // 3. 프린터 초기화 명령
            val escInit = byteArrayOf(0x1B, 0x40)
            val selectWpc1252 = byteArrayOf(0x1B, 0x74, 0x10)
            val fontSizeLarge = byteArrayOf(0x1D, 0x21, 0x11)
            val fontSizeNormal = byteArrayOf(0x1D, 0x21, 0x00)
            
            stream.write(escInit)
            stream.write(selectWpc1252)
            stream.write(fontSizeLarge)

            /** -----------------------------
             * 4. 본문 분할 전송 (Chunking)
             * ----------------------------- */
            val fullBytes = text.toByteArray(charset)
            val chunkSize = 256 // 더 작은 조각으로 나누어 안정성 강화
            var offset = 0
            
            while (offset < fullBytes.size) {
                val count = Math.min(chunkSize, fullBytes.size - offset)
                stream.write(fullBytes, offset, count)
                offset += count
                // 프린터 하드웨어가 데이터를 소화할 시간 제공 (버퍼 타임아웃 방지)
                delay(50) 
            }

            // 5. 마무리 명령 및 용지 배출
            stream.write(fontSizeNormal)
            val padding = "\n\n\n\n\n\n\n\n\n\n".toByteArray(charset)
            stream.write(padding)
            
            stream.flush()

        } catch (e: Exception) {
            // 상세한 원인을 포함한 에러를 상위로 전달
            throw IOException("Printing failed: ${e.message}")
        } finally {
            try {
                socket?.close()
            } catch (_: IOException) {}
        }
    }
}
