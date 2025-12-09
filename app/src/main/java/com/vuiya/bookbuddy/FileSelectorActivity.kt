package com.vuiya.bookbuddy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FileSelectorActivity : AppCompatActivity(), PdfProcessor.PdfProgressListener {

    private lateinit var btnSelectFile: Button
    private lateinit var tvFileName: TextView
    private lateinit var progressBar: ProgressBar
    private var selectedFileUri: Uri? = null

    private val fileSelectorLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let {
            selectedFileUri = it
            tvFileName.text = getFileName(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_selector)

        btnSelectFile = findViewById(R.id.btn_select_pdf)
        tvFileName = findViewById(R.id.tv_file_name)
        progressBar = findViewById(R.id.progress_bar)
        progressBar.max = 100 // Initialize max progress

        btnSelectFile.setOnClickListener {
            fileSelectorLauncher.launch("*/*")
        }

        findViewById<Button>(R.id.btn_process).setOnClickListener {
            selectedFileUri?.let {
                processSelectedFile(it)
            }
        }
    }

    private fun processSelectedFile(uri: Uri) {
        val fileName = getFileName(uri)
        if (fileName.endsWith(".pdf")) {
            processPdf(uri)
        } else if (fileName.endsWith(".epub")) {
            processEpub(uri)
        } else {
            Toast.makeText(this, "Unsupported file type", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processPdf(uri: Uri) {
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0

        val pdfProcessor = PdfProcessor(this, this)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = pdfProcessor.processPdf(uri, "bengali").get() // Wait for the future to complete

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (result.success) {
                        val bookDir = File(cacheDir, "book_${System.currentTimeMillis()}")
                        bookDir.mkdirs()
                        result.extractedText?.split("--- Page \\d+ ---".toRegex())?.forEachIndexed { index, pageContent ->
                            if (pageContent.isNotBlank()) {
                                val pageFile = File(bookDir, "page_$index.txt")
                                pageFile.writeText(pageContent)
                            }
                        }

                        val intent = Intent(this@FileSelectorActivity, ReaderActivity::class.java).apply {
                            putExtra("book_path", bookDir.absolutePath)
                            putExtra("book_title", getFileName(uri))
                            putExtra("book_language", "bengali")
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@FileSelectorActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@FileSelectorActivity, "An unexpected error occurred: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                pdfProcessor.release()
            }
        }
    }

    private fun processEpub(uri: Uri) {
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0

        CoroutineScope(Dispatchers.IO).launch {
            val epubProcessor = EpubProcessor(this@FileSelectorActivity)
            when (val result = epubProcessor.processEpub(uri)) {
                is EpubProcessingResult.Success -> {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        val bookDir = File(cacheDir, "book_${System.currentTimeMillis()}")
                        bookDir.mkdirs()
                        val chunkSize = 1500
                        result.content.chunked(chunkSize).forEachIndexed { index, pageContent ->
                            val pageFile = File(bookDir, "page_$index.txt")
                            pageFile.writeText(pageContent)
                        }

                        val intent = Intent(this@FileSelectorActivity, ReaderActivity::class.java).apply {
                            putExtra("book_path", bookDir.absolutePath)
                            putExtra("book_title", getFileName(uri))
                            putExtra("book_language", "latin")
                        }
                        startActivity(intent)
                    }
                }
                is EpubProcessingResult.Error -> {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@FileSelectorActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onProgressUpdate(currentPage: Int, totalPages: Int) {
        runOnUiThread {
            if (totalPages > 0) {
                val progress = (currentPage.toFloat() / totalPages.toFloat() * 100).toInt()
                progressBar.progress = progress
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                if (cut != null) {
                    result = result?.substring(cut + 1)
                }
            }
        }
        return result ?: "Unknown"
    }
}
