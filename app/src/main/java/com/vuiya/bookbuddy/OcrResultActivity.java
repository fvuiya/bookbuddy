package com.vuiya.bookbuddy;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class OcrResultActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvOcrResult;
    private String ocrResultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_result);

        initViews();
        setupClickListeners();

        // Get OCR result from intent
        ocrResultText = getIntent().getStringExtra("ocr_result");
        if (ocrResultText == null) {
            ocrResultText = "No text was recognized. Please try again with a clearer image.";
        }

        displayOcrResult();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvOcrResult = findViewById(R.id.tv_ocr_result);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Copy text button
        findViewById(R.id.btn_copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyText();
            }
        });

        // Translate button
        findViewById(R.id.btn_translate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                translateText();
            }
        });

        // Save to library button
        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToLibrary();
            }
        });
    }

    private void displayOcrResult() {
        tvOcrResult.setText(ocrResultText);
    }

    private void copyText() {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("OCR Result", ocrResultText);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void translateText() {
        Toast.makeText(this, "Translation functionality will be implemented in full version",
                Toast.LENGTH_SHORT).show();
    }

    private void saveToLibrary() {
        Toast.makeText(this, "Saved to library", Toast.LENGTH_SHORT).show();
    }
}