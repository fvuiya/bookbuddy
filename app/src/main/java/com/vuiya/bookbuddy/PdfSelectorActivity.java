package com.vuiya.bookbuddy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PdfSelectorActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 1002;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1003;

    private Button btnChooseFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_selector);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        btnChooseFile = findViewById(R.id.btn_choose_file);
    }

    private void setupClickListeners() {
        btnChooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check for storage permission
                if (ContextCompat.checkSelfPermission(PdfSelectorActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Request storage permission
                    ActivityCompat.requestPermissions(PdfSelectorActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            STORAGE_PERMISSION_REQUEST_CODE);
                } else {
                    // Permission already granted, open file picker
                    openFilePicker();
                }
            }
        });

        // Back button functionality
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
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
                // TODO: Process the selected PDF file
                Toast.makeText(this, "PDF selected: " + pdfUri.toString(), Toast.LENGTH_SHORT).show();

                // For now, just go back to main screen
                // In future, we'll pass this URI to a PDF processing activity
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open file picker
                openFilePicker();
            } else {
                // Permission denied, show a message
                Toast.makeText(this, "Storage permission is required to select PDF files",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}