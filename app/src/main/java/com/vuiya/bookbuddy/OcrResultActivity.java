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
    private String ocrResultText = "This is a sample OCR result. In the full implementation, this would show the text extracted from your scanned image.\n\n" +
            "The OCR functionality would use ML Kit or Tesseract to recognize text in the image.\n\n" +
            "You would be able to copy this text, translate it, or save it to your library.\n\n" +
            "For now, this is just a placeholder to demonstrate the app flow.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_result);

        initViews();
        setupClickListeners();
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