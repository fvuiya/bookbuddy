package com.vuiya.bookbuddy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.model.KeyPath;
import com.airbnb.lottie.value.SimpleLottieValueCallback;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PdfSelectorActivity extends AppCompatActivity implements PdfProcessor.PdfProgressListener {

    private static final String TAG = "PdfSelectorActivity";
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1003;

    private Button btnChooseFile;
    private PdfProcessor pdfProcessor;
    private boolean isProcessing = false;
    private LottieAnimationView lottieLoading;
    private View layoutContent;
    private ActivityResultLauncher<Intent> pdfPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_selector);

        initViews();
        setupClickListeners();
        applyLottieThemeColors();

        pdfProcessor = new PdfProcessor(this, this);

        pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri pdfUri = data.getData();
                            processSelectedPdf(pdfUri);
                        }
                    }
                });

        lottieLoading.addLottieOnCompositionLoadedListener(
                composition -> logAllKeyPaths(lottieLoading)
        );
    }

    private void initViews() {
        btnChooseFile = findViewById(R.id.btn_choose_file);
        lottieLoading = findViewById(R.id.lottie_loading);
        layoutContent = findViewById(R.id.layout_content);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void logAllKeyPaths(LottieAnimationView view) {
        if (view.getComposition() == null) {
            Log.w(TAG, "Lottie composition not loaded yet.");
            return;
        }
        List<KeyPath> keyPaths = view.resolveKeyPath(new KeyPath("**"));
        Log.d(TAG, "==== Lottie KeyPaths ====");
        for (KeyPath keyPath : keyPaths) {
            Log.d(TAG, keyPath.toString());
        }
        Log.d(TAG, "==========================");
    }

    private void setupClickListeners() {
        btnChooseFile.setOnClickListener(v -> {
            if (isProcessing) {
                Toast.makeText(this, "A PDF is already being processed. Please wait.", Toast.LENGTH_SHORT).show();
                return;
            }

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
        pdfPickerLauncher.launch(intent);
    }

    private void processSelectedPdf(Uri pdfUri) {
        if (isProcessing) {
            Log.w(TAG, "processSelectedPdf called while already processing");
            return;
        }

        isProcessing = true;
        layoutContent.setVisibility(View.GONE);
        lottieLoading.setVisibility(View.VISIBLE);
        lottieLoading.playAnimation();

        Toast.makeText(this, "Processing PDF...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Starting PDF processing for URI: " + pdfUri.toString());

        CompletableFuture<PdfProcessor.PdfProcessingResult> processFuture = pdfProcessor.processPdf(pdfUri);

        processFuture.thenAccept(result -> {
            runOnUiThread(() -> {
                lottieLoading.cancelAnimation();
                lottieLoading.setVisibility(View.GONE);
                layoutContent.setVisibility(View.VISIBLE);
                isProcessing = false;

                if (result.isSuccess()) {
                    Log.d(TAG, "PDF processing successful: " + result.getMessage());
                    Toast.makeText(PdfSelectorActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(PdfSelectorActivity.this, ReaderActivity.class);
                    intent.putExtra("book_content", result.getExtractedText());
                    intent.putExtra("book_title", "PDF Document");
                    startActivity(intent);
                } else {
                    Log.e(TAG, "PDF processing failed: " + result.getMessage());
                    Toast.makeText(PdfSelectorActivity.this, "Failed: " + result.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        }).exceptionally(throwable -> {
            runOnUiThread(() -> {
                lottieLoading.cancelAnimation();
                lottieLoading.setVisibility(View.GONE);
                layoutContent.setVisibility(View.VISIBLE);
                isProcessing = false;
                Log.e(TAG, "Error processing PDF (in CompletableFuture chain)", throwable);
                Toast.makeText(PdfSelectorActivity.this, "Error processing PDF: " + throwable.getMessage(),
                        Toast.LENGTH_LONG).show();
            });
            return null;
        });
    }

    private void applyLottieThemeColors() {
        if (lottieLoading != null) {
            int primaryColor = ContextCompat.getColor(this, R.color.primary_color);
            int secondaryColor = ContextCompat.getColor(this, R.color.secondary_color);
            int accentColor = ContextCompat.getColor(this, R.color.accent_color);
            int errorColor = ContextCompat.getColor(this, R.color.error_color);
            int primaryVariantColor = ContextCompat.getColor(this, R.color.primary_variant);

            int fireInnerColor = secondaryColor;
            int fireOuterColor = errorColor;

            // Robot's Body
            lottieLoading.addValueCallback(
                    new KeyPath("body", "Group 3", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            );

            // Head
            lottieLoading.addValueCallback(
                    new KeyPath("head", "Group 3", "Group 1", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("head", "Group 3", "Group 2", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            );

            // Arms and hands
            lottieLoading.addValueCallback(
                    new KeyPath("l hand", "Group 1", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(secondaryColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("r hand", "Group 1", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(secondaryColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("l arm", "Group 1", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("r arm", "Group 1", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            );

            // Book
            lottieLoading.addValueCallback(
                    new KeyPath("book", "Group 1", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
            );

            // Fire
            lottieLoading.addValueCallback(
                    new KeyPath("fire", "Group 1", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(fireInnerColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("fire", "Group 2", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(fireOuterColor, PorterDuff.Mode.SRC_IN)
            );

            // Bookshelf and objects on it
            // Horizontal shelves
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 2", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 6", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 8", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            );

            // Vertical supports
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 4", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryVariantColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 17", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryVariantColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 21", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryVariantColor, PorterDuff.Mode.SRC_IN)
            );

            // Plant pot
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 1", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
            );
            // Books
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 9", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(secondaryColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 10", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryVariantColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 11", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 12", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(secondaryColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 13", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 14", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 15", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryVariantColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 16", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            );

            // Plant leaves
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 3", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryVariantColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 5", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
            );

            // Other details on the shelf
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 18", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(secondaryColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 19", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 20", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 22", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 23", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 24", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 25", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 26", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            );
            lottieLoading.addValueCallback(
                    new KeyPath("Flat Color", "Group 27", "Path 1"),
                    LottieProperty.COLOR_FILTER,
                    frameInfo -> new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            );

            Log.d(TAG, "Applied theme colors to Lottie animation");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyLottieThemeColors();
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

    @Override
    public void onProgressUpdate(int currentPage, int totalPages) {
        runOnUiThread(() -> {
            if (totalPages > 0) {
                int progress = (int) (((float) currentPage / totalPages) * 100);
                Log.d(TAG, "PDF Processing Progress: " + progress + "%");
            }
        });
    }
}
