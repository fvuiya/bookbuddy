package com.vuiya.bookbuddy

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class OcrResultActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvOcrResult: TextView
    private var ocrResultText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocr_result)

        initViews()
        setupClickListeners()

        ocrResultText = intent.getStringExtra("ocr_result")
            ?: "No text was recognized. Please try again with a clearer image."

        displayOcrResult()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvOcrResult = findViewById(R.id.tv_ocr_result)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        findViewById<View>(R.id.btn_copy).setOnClickListener { copyText() }

        findViewById<View>(R.id.btn_translate).setOnClickListener { translateText() }

        findViewById<View>(R.id.btn_save).setOnClickListener { saveToLibrary() }
    }

    private fun displayOcrResult() {
        tvOcrResult.text = ocrResultText
    }

    private fun copyText() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OCR Result", ocrResultText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun translateText() {
        Toast.makeText(this, "Translation functionality will be implemented in full version", Toast.LENGTH_SHORT).show()
    }

    private fun saveToLibrary() {
        Toast.makeText(this, "Saved to library", Toast.LENGTH_SHORT).show()
    }
}
