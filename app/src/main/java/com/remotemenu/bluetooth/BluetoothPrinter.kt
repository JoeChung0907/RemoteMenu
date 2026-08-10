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
 * 선택된 블루투스 기기(Epson 규격)로 데이터를 전송하여 실제 영수증을 출력하는 기능을 담당합니다.
 */
class BluetoothPrinter(private val context: Context) {

    // 표준 시리얼 포트 프로파일(SPP) UUID
    private val printerUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /**
     * printToDevice
     * 지정된 블루투스 기기로 텍스트 데이터를 전송하여 인쇄합니다.
     * @param device 인쇄할 대상 블루투스 기기
     * @param text 출력할 영수증 텍스트 전문
     */
    suspend fun printToDevice(device: BluetoothDevice, text: String) = withContext(Dispatchers.IO) {
        /** -----------------------------
         * 권한 체크 (Android 12 이상 대응)
         * ----------------------------- */
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true

        if (!hasPermission) {
            Log.e("BluetoothPrinter", "BLUETOOTH_CONNECT permission missing")
            return@withContext
        }

        var socket: BluetoothSocket? = null
        try {
            Log.i("BluetoothPrinter", "Connecting to device: ${device.address}")
            
            // RFCOMM 소켓 생성 및 연결 시도
            socket = device.createRfcommSocketToServiceRecord(printerUUID)
            socket.connect()

            socket.outputStream.use { stream ->
                // 영국/유럽 Epson 프린터 표준 인코딩 적용
                val charset = Charset.forName("windows-1252")
                
                /** -----------------------------
                 * ESC/POS 프린터 명령어 설정
                 * ----------------------------- */
                val escInit = byteArrayOf(0x1B, 0x40)               // 프린터 초기화
                val selectWpc1252 = byteArrayOf(0x1B, 0x74, 0x10)  // 서구권 문자표(WPC1252) 선택
                val fontSizeLarge = byteArrayOf(0x1D, 0x21, 0x11)    // 2배 확대 (가로/세로)
                val fontSizeNormal = byteArrayOf(0x1D, 0x21, 0x00)   // 일반 크기 복구
                
                // 1. 설정 명령 전송
                stream.write(escInit)
                stream.write(selectWpc1252)
                stream.write(fontSizeLarge)
                
                // 2. 본문 인쇄
                stream.write(text.toByteArray(charset))
                
                // 3. 설정 초기화 및 용지 배출(Feed)
                stream.write(fontSizeNormal)
                val tailPadding = "\n".repeat(10) // 잘 뜯기도록 하단 여백 추가
                stream.write(tailPadding.toByteArray(charset))
                
                stream.flush()
            }
            Log.i("BluetoothPrinter", "Printed successfully to ${device.name ?: device.address}")
        } catch (e: IOException) {
            Log.e("BluetoothPrinter", "Printing failed: ${e.message}")
        } finally {
            // 소켓 자원 해제
            try { socket?.close() } catch (_: IOException) {}
        }
    }
}
