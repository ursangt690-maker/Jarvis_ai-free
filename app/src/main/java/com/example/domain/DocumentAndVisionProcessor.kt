package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LoadedDocumentInfo(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val extractedText: String,
    val previewBitmap: Bitmap? = null,
    val qrOrBarcodeValue: String? = null
)

class DocumentAndVisionProcessor(private val context: Context) {

    suspend fun loadUriDocumentOrImage(uri: Uri): Result<LoadedDocumentInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "application/octet-stream"
            var fileName = "document"
            var sizeBytes = 0L

            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex >= 0) sizeBytes = cursor.getLong(sizeIndex)
                }
            }

            when {
                mimeType.startsWith("image/") -> {
                    val bitmap = resolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    } ?: throw IllegalStateException("Could not decode image file.")
                    val scaled = scaleBitmapIfNeeded(bitmap, 1024)
                    val qrText = decodeBarcodeOrQr(scaled)
                    LoadedDocumentInfo(
                        fileName = fileName,
                        mimeType = mimeType,
                        sizeBytes = sizeBytes,
                        extractedText = if (qrText != null) "Detected QR/Barcode Payload: $qrText" else "",
                        previewBitmap = scaled,
                        qrOrBarcodeValue = qrText
                    )
                }

                mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true) -> {
                    val pfd: ParcelFileDescriptor = resolver.openFileDescriptor(uri, "r")
                        ?: throw IllegalStateException("Unable to open PDF descriptor.")
                    pfd.use { descriptor ->
                        val renderer = PdfRenderer(descriptor)
                        renderer.use { pdf ->
                            val pageCount = pdf.pageCount
                            var firstPageBitmap: Bitmap? = null
                            if (pageCount > 0) {
                                val page = pdf.openPage(0)
                                page.use { p ->
                                    val bmp = Bitmap.createBitmap(
                                        p.width.coerceAtLeast(1),
                                        p.height.coerceAtLeast(1),
                                        Bitmap.Config.ARGB_8888
                                    )
                                    bmp.eraseColor(Color.WHITE)
                                    p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    firstPageBitmap = scaleBitmapIfNeeded(bmp, 1024)
                                }
                            }
                            LoadedDocumentInfo(
                                fileName = fileName,
                                mimeType = "application/pdf",
                                sizeBytes = sizeBytes,
                                extractedText = "PDF Document ($pageCount pages). Page 1 rendered for Gemini Vision OCR & document summarization.",
                                previewBitmap = firstPageBitmap
                            )
                        }
                    }
                }

                else -> {
                    // Read text / source code / markdown / JSON / log file
                    val text = resolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream)).use { reader ->
                            val sb = StringBuilder()
                            var line: String?
                            while (reader.readLine().also { line = it } != null && sb.length < 18000) {
                                sb.appendLine(line)
                            }
                            sb.toString()
                        }
                    } ?: ""
                    if (text.isBlank()) {
                        throw IllegalStateException("Unsupported binary format or empty file ($mimeType).")
                    }
                    LoadedDocumentInfo(
                        fileName = fileName,
                        mimeType = mimeType,
                        sizeBytes = sizeBytes,
                        extractedText = text.take(16000)
                    )
                }
            }
        }
    }

    fun decodeBarcodeOrQr(bitmap: Bitmap): String? = runCatching {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val result = MultiFormatReader().decode(binaryBitmap)
        result.text
    }.getOrNull()

    fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val (newW, newH) = if (ratio > 1f) {
            maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
