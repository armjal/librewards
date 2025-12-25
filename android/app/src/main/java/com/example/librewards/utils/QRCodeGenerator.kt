package com.example.librewards.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import java.io.IOException
import java.nio.charset.Charset
import java.util.Hashtable

class QRCodeGenerator {
    // Function to create the QR code
    @Throws(WriterException::class, IOException::class)
    fun createQR(
        data: String, height: Int,
        width: Int,
    ): Bitmap? {
        val charset: Charset = Charsets.UTF_8
        val hints: Hashtable<EncodeHintType, String> = Hashtable(2)
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        val bits = MultiFormatWriter().encode(
            String(data.toByteArray(charset), charset),
            BarcodeFormat.QR_CODE,
            width,
            height,
            hints,
        )
        return generateQrBitmap(height, width, bits)
    }
}

private fun generateQrBitmap(height: Int, width: Int, bits: BitMatrix): Bitmap? {
    var bitmap: Bitmap? = null
    try {
        bitmap = createBitmap(width, height, Bitmap.Config.RGB_565).also {
            for (x in 0 until width) {
                for (y in 0 until height) {
                    it[x, y] = if (bits[x, y]) Color.BLACK else Color.WHITE
                }
            }
        }
    } catch (e: Exception) {
        Log.e("TAG", "Failed to create image: $e")
    }
    return bitmap
}
