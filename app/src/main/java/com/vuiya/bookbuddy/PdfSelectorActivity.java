package com.vuiya.bookbuddy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.concurrent.CompletableFuture;

public class PdfSelectorActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 1002;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1003;

    private Button btnChooseFile;
    private PdfProcessor pdfProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_selector);

        initViews();
        setupClickListeners();

        // Initialize PDF processor
        pdfProcessor = new PdfProcessor(this);
    }

    private void initViews() {
        btnChooseFile = findViewById(R.id.btn_choose_file);
        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnChooseFile.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(PdfSelectorActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(PdfSelectorActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                openFilePicker();
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, PICK_PDF_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri pdfUri = data.getData();
                processSelectedPdf(pdfUri);
            }
        }
    }

    private void processSelectedPdf(Uri pdfUri) {
        // Show processing message
        Toast.makeText(this, "Processing PDF...", Toast.LENGTH_SHORT).show();

        // Process PDF in background
        CompletableFuture<PdfProcessor.PdfProcessingResult> processFuture = pdfProcessor.processPdf(pdfUri);

        processFuture.thenAccept(result -> {
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    Toast.makeText(PdfSelectorActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();

                    // Pass the extracted text to ReaderActivity
                    Intent intent = new Intent(PdfSelectorActivity.this, ReaderActivity.class);
                    intent.putExtra("book_content", result.getExtractedText());
                    intent.putExtra("book_title", "PDF Document"); // You might want to extract the actual filename
                    startActivity(intent);
                } else {
                    Toast.makeText(PdfSelectorActivity.this, "Failed: " + result.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }).exceptionally(throwable -> {
            runOnUiThread(() -> {
                Toast.makeText(PdfSelectorActivity.this, "Error processing PDF: " + throwable.getMessage(),
                        Toast.LENGTH_LONG).show();
            });
            return null;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker();
            } else {
                Toast.makeText(this, "Storage permission is required to select PDF files",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfProcessor != null) {
            pdfProcessor.release();
        }
    }
}