package com.vuiya.bookbuddy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class PdfProcessor(private val context: Context, private val progressListener: PdfProgressListener) {

    private val executorService = Executors.newSingleThreadExecutor()
    private val textRecognizers = mutableMapOf<String, TextRecognizer>()

    init {
        try {
            PDFBoxResourceLoader.init(context.applicationContext)
            Log.d(TAG, "PDFBoxResourceLoader initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize PDFBoxResourceLoader.", e)
        }
    }

    private fun getTextRecognizer(language: String): TextRecognizer {
        return textRecognizers.getOrPut(language) {
            when (language) {
                "bengali" -> TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                else -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            }
        }
    }

    fun processPdf(pdfUri: Uri, language: String = "latin"): CompletableFuture<PdfProcessingResult> {
        val futureResult = CompletableFuture<PdfProcessingResult>()

        executorService.submit {
            var tempPdfFile: File? = null
            try {
                Log.d(TAG, "Starting PDF processing for URI: $pdfUri")
                tempPdfFile = copyPdfToTempFile(pdfUri)
                if (tempPdfFile == null) {
                    Log.e(TAG, "Failed to copy PDF to temporary file")
                    futureResult.complete(PdfProcessingResult(false, "Failed to copy PDF to temporary file", null))
                    return@submit
                }

                val extractedText = extractTextFromDigitalPdf(tempPdfFile)
                if (extractedText != null && extractedText.trim().isNotEmpty()) {
                    futureResult.complete(PdfProcessingResult(true, "Digital PDF processed", extractedText))
                    return@submit
                }

                val ocrText = performOcrOnScannedPdf(tempPdfFile, language)
                if (ocrText != null && ocrText.trim().isNotEmpty()) {
                    futureResult.complete(PdfProcessingResult(true, "Scanned PDF processed with OCR", ocrText))
                } else {
                    progressListener.onProgressUpdate(0, 0)
                    futureResult.complete(PdfProcessingResult(false, "Failed to process PDF - no text found", null))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during PDF processing", e)
                progressListener.onProgressUpdate(0, 0)
                futureResult.completeExceptionally(e)
            } finally {
                tempPdfFile?.delete()
            }
        }

        return futureResult
    }

    private fun copyPdfToTempFile(pdfUri: Uri): File? {
        try {
            Log.d(TAG, "Copying PDF from URI to temp file")
            val inputStream = context.contentResolver.openInputStream(pdfUri) ?: return null

            val tempFile = File.createTempFile("temp_pdf", ".pdf", context.cacheDir)
            Log.d(TAG, "Created temp file: ${tempFile.absolutePath}")
            val outputStream = FileOutputStream(tempFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "PDF copy completed successfully")
            return tempFile
        } catch (e: IOException) {
            Log.e(TAG, "Error copying PDF to temp file", e)
            return null
        }
    }

    private fun extractTextFromDigitalPdf(pdfFile: File): String? {
        var document: PDDocument? = null
        var totalPages = 0
        try {
            Log.d(TAG, "Loading PDDocument from file: ${pdfFile.absolutePath}")
            document = PDDocument.load(pdfFile)
            totalPages = document.numberOfPages
            Log.d(TAG, "PDDocument loaded successfully. Number of pages: $totalPages")

            progressListener.onProgressUpdate(0, totalPages)

            val pdfStripper = PDFTextStripper()

            val text = pdfStripper.getText(document)
            Log.d(TAG, "Text extracted successfully. Length: ${text?.length ?: 0}")

            progressListener.onProgressUpdate(totalPages, totalPages)
            return text
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract text from PDF (might be scanned or corrupted)", e)
            return null
        } finally {
            document?.close()
        }
    }

    private fun performOcrOnScannedPdf(pdfFile: File, language: String): String? {
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null
        val ocrText = StringBuilder()
        var ocrPerformedAtLeastOnce = false
        var pageCount = 0

        try {
            parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor)
            pageCount = pdfRenderer.pageCount

            if (pageCount == 0) {
                progressListener.onProgressUpdate(0, 0)
                return ""
            }

            val textRecognizer = getTextRecognizer(language)

            for (i in 0 until pageCount) {
                progressListener.onProgressUpdate(i, pageCount)
                var page: PdfRenderer.Page? = null
                var bitmap: Bitmap? = null
                try {
                    page = pdfRenderer.openPage(i)
                    val dpi = 300
                    val renderedWidth = page.width * dpi / 72
                    val renderedHeight = page.height * dpi / 72
                    bitmap = Bitmap.createBitmap(renderedWidth, renderedHeight, Bitmap.Config.ARGB_8888)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    val textResult = Tasks.await(textRecognizer.process(inputImage))
                    ocrPerformedAtLeastOnce = true

                    for (block in textResult.textBlocks) {
                        ocrText.append(block.text).append("\n")
                    }
                    ocrText.append("\n--- Page ").append(i + 1).append(" ---\n\n")

                } finally {
                    bitmap?.recycle()
                    page?.close()
                }
            }

            progressListener.onProgressUpdate(pageCount, pageCount)

            return if (ocrPerformedAtLeastOnce) ocrText.toString() else null

        } catch (e: Exception) {
            Log.e(TAG, "Error performing OCR on scanned PDF", e)
            progressListener.onProgressUpdate(pageCount, pageCount)
            return null
        } finally {
            pdfRenderer?.close()
            parcelFileDescriptor?.close()
        }
    }

    fun release() {
        textRecognizers.values.forEach { it.close() }
        textRecognizers.clear()
        if (!executorService.isShutdown) {
            executorService.shutdown()
        }
    }

    data class PdfProcessingResult(
        val success: Boolean,
        val message: String,
        val extractedText: String?
    )

    interface PdfProgressListener {
        fun onProgressUpdate(currentPage: Int, totalPages: Int)
    }

    companion object {
        private const val TAG = "PdfProcessor"
    }
}
