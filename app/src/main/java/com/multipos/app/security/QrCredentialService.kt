package com.multipos.app.security

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.UUID

object QrCredentialService {
    private const val PREFIX = "multipos://credito/v1"

    data class Payload(val companyId: String, val credentialId: String)

    fun newCredentialId(): String = UUID.randomUUID().toString()

    fun buildPayload(companyId: String, credentialId: String): String = "$PREFIX/$companyId/$credentialId"

    fun parsePayload(raw: String): Payload? {
        val parts = raw.trim().split('/')
        if (parts.size != 6 || parts[0] != "multipos:" || parts[2] != "credito" || parts[3] != "v1") return null
        val companyId = parts[4].takeIf(String::isNotBlank) ?: return null
        val credentialId = parts[5].takeIf(String::isNotBlank) ?: return null
        return Payload(companyId, credentialId)
    }

    fun createBitmap(payload: String, size: Int = 640): Bitmap {
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size)
        val pixels = IntArray(size * size)
        for (y in 0 until size) for (x in 0 until size) pixels[y * size + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply { setPixels(pixels, 0, size, 0, 0, size, size) }
    }
}
