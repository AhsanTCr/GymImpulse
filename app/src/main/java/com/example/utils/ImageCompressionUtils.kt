package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Compresses user-selected profile pictures from Android Photo Picker into
 * lightweight <= 200px JPEG data URIs that store safely in Firestore (<= 100 KB).
 */
object ImageCompressionUtils {

    private const val MAX_DIMENSION_PX = 200
    private const val JPEG_QUALITY = 76
    private const val MAX_BASE64_LENGTH = 110_000

    suspend fun compressUriToDataUri(context: Context, uriString: String?): String? = withContext(Dispatchers.IO) {
        if (uriString.isNullOrBlank()) return@withContext null
        if (uriString.startsWith("data:image/") && uriString.length <= MAX_BASE64_LENGTH) {
            return@withContext uriString
        }
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return@withContext null

        return@withContext runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

            var sampleSize = 1
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            if (longest > MAX_DIMENSION_PX) {
                while ((longest / sampleSize) > MAX_DIMENSION_PX * 2) {
                    sampleSize *= 2
                }
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return@runCatching null

            val scaled = scaleBitmap(decoded, MAX_DIMENSION_PX)
            if (scaled != decoded) decoded.recycle()

            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
            scaled.recycle()

            val encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            val dataUri = "data:image/jpeg;base64,$encoded"
            if (dataUri.length <= MAX_BASE64_LENGTH) dataUri else null
        }.getOrNull()
    }

    private fun scaleBitmap(source: Bitmap, maxDim: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxDim && h <= maxDim) return source
        val ratio = maxDim.toFloat() / maxOf(w, h).toFloat()
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }

    fun decodeBase64DataUri(dataUri: String?): Bitmap? {
        if (dataUri.isNullOrBlank() || !dataUri.startsWith("data:image/")) return null
        val commaIndex = dataUri.indexOf(',')
        if (commaIndex < 0) return null
        val base64Part = dataUri.substring(commaIndex + 1)
        return runCatching {
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
}
