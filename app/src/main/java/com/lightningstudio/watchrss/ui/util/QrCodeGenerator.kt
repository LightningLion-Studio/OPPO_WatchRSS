package com.lightningstudio.watchrss.ui.util

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeGenerator {
    fun create(text: String, size: Int): Bitmap? {
        if (text.isBlank() || size <= 0) return null
        return try {
            val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                val offset = y * size
                for (x in 0 until size) {
                    pixels[offset + x] = if (matrix[x, y]) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
                }
            }
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
                bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
            }
        } catch (_: Exception) {
            null
        }
    }
}
