package com.vuiya.bookbuddy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.android.gms.tasks.Tasks;

public class PdfProcessor {
    private static final String TAG = "PdfProcessor";
    private Context context;
    private TextRecognizer textRecognizer;
    private ExecutorService executorService;

    public PdfProcessor(Context context) {
        this.context = context;
        this.textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public CompletableFuture<PdfProcessingResult> processPdf(Uri pdfUri) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Starting PDF processing for URI: " + pdfUri.toString());
                // Copy the PDF to a temporary file if needed
                File tempPdfFile = copyPdfToTempFile(pdfUri);
                if (tempPdfFile == null) {
                    Log.e(TAG, "Failed to copy PDF to temporary file");
                    return new PdfProcessingResult(false, "Failed to copy PDF to temporary file", null);
                }
                Log.d(TAG, "PDF copied to temporary file: " + tempPdfFile.getAbsolutePath());

                // Try to extract text directly first (for digital PDFs)
                Log.d(TAG, "Attempting to extract text from digital PDF...");
                String extractedText = extractTextFromDigitalPdf(tempPdfFile);
                if (extractedText != null && !extractedText.trim().isEmpty()) {
                    Log.d(TAG, "Digital PDF detected, extracted text directly");
                    return new PdfProcessingResult(true, "Digital PDF processed", extractedText);
                }

                // If text extraction failed, treat as scanned PDF and perform OCR
                Log.d(TAG, "Scanned PDF detected or digital extraction failed, performing OCR on pages...");
                String ocrText = performOcrOnScannedPdf(tempPdfFile);
                if (ocrText != null && !ocrText.trim().isEmpty()) {
                    Log.d(TAG, "Scanned PDF processed with OCR successfully");
                    return new PdfProcessingResult(true, "Scanned PDF processed with OCR", ocrText);
                } else {
                    Log.w(TAG, "Failed to process PDF - no text found after OCR attempt");
                    return new PdfProcessingResult(false, "Failed to process PDF - no text found", null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing PDF", e);
                return new PdfProcessingResult(false, "Error processing PDF: " + e.getMessage(), null);
            }
        }, executorService);
    }

    private File copyPdfToTempFile(Uri pdfUri) {
        try {
            Log.d(TAG, "Copying PDF from URI to temp file");
            InputStream inputStream = context.getContentResolver().openInputStream(pdfUri);
            if (inputStream == null) {
                Log.e(TAG, "InputStream is null for URI: " + pdfUri.toString());
                return null;
            }

            File tempFile = File.createTempFile("temp_pdf", ".pdf", context.getCacheDir());
            Log.d(TAG, "Created temp file: " + tempFile.getAbsolutePath());
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();
            Log.d(TAG, "PDF copy completed successfully");
            return tempFile;
        } catch (IOException e) {
            Log.e(TAG, "Error copying PDF to temp file", e);
            return null;
        }
    }

    private String extractTextFromDigitalPdf(File pdfFile) {
        PDDocument document = null;
        try {
            Log.d(TAG, "Loading PDDocument from file: " + pdfFile.getAbsolutePath());
            document = PDDocument.load(pdfFile);
            Log.d(TAG, "PDDocument loaded successfully. Number of pages: " + document.getNumberOfPages());

            Log.d(TAG, "Creating PDFTextStripper...");
            // Wrap PDFTextStripper creation in a try-catch to isolate the specific failure
            PDFTextStripper pdfStripper;
            try {
                pdfStripper = new PDFTextStripper();
                Log.d(TAG, "PDFTextStripper created successfully.");
            } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
                Log.e(TAG, "Failed to initialize PDFTextStripper. This might be a MultiDex or ProGuard issue.", e);
                // Return null to indicate failure, will try OCR
                return null;
            }

            Log.d(TAG, "Extracting text using PDFTextStripper...");
            String text = pdfStripper.getText(document);
            Log.d(TAG, "Text extracted successfully. Length: " + (text != null ? text.length() : 0));
            return text;
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            // Catch specific errors related to class loading/initialization
            Log.w(TAG, "Failed to extract text from PDF (Class loading issue - might be scanned or MultiDex/ProGuard problem)", e);
            return null; // Return null to indicate failure, will try OCR
        } catch (UnsatisfiedLinkError e) {
            // Catch native library loading errors
            Log.w(TAG, "Failed to extract text from PDF (Native library issue)", e);
            return null; // Return null to indicate failure, will try OCR
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract text from PDF (might be scanned or corrupted)", e);
            return null; // Return null to indicate failure, will try OCR
        } finally {
            if (document != null) {
                try {
                    document.close();
                    Log.d(TAG, "PDDocument closed successfully.");
                } catch (IOException e) {
                    Log.w(TAG, "Failed to close PDDocument", e);
                }
            }
        }
    }


    private String performOcrOnScannedPdf(File pdfFile) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        PdfRenderer pdfRenderer = null;
        StringBuilder ocrText = new StringBuilder();
        boolean ocrPerformed = false;

        try {
            Log.d(TAG, "Starting OCR on scanned PDF: " + pdfFile.getAbsolutePath());
            parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);

            int pageCount = pdfRenderer.getPageCount();
            Log.d(TAG, "PDF has " + pageCount + " pages.");

            if (pageCount == 0) {
                Log.w(TAG, "PDF has 0 pages, nothing to OCR.");
                return "";
            }

            // Process a limited number of pages to prevent excessive processing
            int pagesToProcess = Math.min(pageCount, 10); // Process max 10 pages for now
            Log.d(TAG, "Will process first " + pagesToProcess + " pages.");

            for (int i = 0; i < pagesToProcess; i++) {
                Log.d(TAG, "Processing page " + (i + 1));
                PdfRenderer.Page page = pdfRenderer.openPage(i);

                try {
                    // Render page to bitmap with reasonable density
                    // Using higher density for better OCR accuracy, but not too high to avoid OOM
                    int renderedWidth = page.getWidth() * 2; // Density 2x
                    int renderedHeight = page.getHeight() * 2;
                    Log.d(TAG, "Rendering page " + (i + 1) + " to bitmap (" + renderedWidth + "x" + renderedHeight + ")");

                    Bitmap bitmap = Bitmap.createBitmap(
                            renderedWidth,
                            renderedHeight,
                            Bitmap.Config.ARGB_8888
                    );

                    // Render the page to the bitmap
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                    // Perform OCR on the bitmap
                    Log.d(TAG, "Performing OCR on page " + (i + 1) + " bitmap");
                    InputImage inputImage = InputImage.fromBitmap(bitmap, 0);

                    // Use await() to make the call synchronous on the background thread
                    Text text = Tasks.await(textRecognizer.process(inputImage));
                    ocrPerformed = true;

                    // Append text from this page
                    int blocksFound = 0;
                    for (Text.TextBlock block : text.getTextBlocks()) {
                        ocrText.append(block.getText()).append("\n");
                        blocksFound++;
                    }
                    Log.d(TAG, "Page " + (i + 1) + " OCR complete. Found " + blocksFound + " text blocks.");

                    // Add page separator
                    ocrText.append("\n--- Page ").append(i + 1).append(" ---\n\n");

                    // Recycle bitmap to free memory
                    bitmap.recycle();
                } finally {
                    page.close();
                    Log.d(TAG, "Closed page " + (i + 1));
                }
            }

            if (ocrPerformed) {
                Log.d(TAG, "OCR processing completed successfully. Total text length: " + ocrText.length());
                return ocrText.toString();
            } else {
                Log.w(TAG, "No OCR was performed on any page.");
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error performing OCR on scanned PDF", e);
            return null;
        } finally {
            // Close resources in reverse order
            if (pdfRenderer != null) {
                try {
                    pdfRenderer.close();
                    Log.d(TAG, "PdfRenderer closed.");
                } catch (Exception e) {
                    Log.w(TAG, "Error closing PdfRenderer", e);
                }
            }
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                    Log.d(TAG, "ParcelFileDescriptor closed.");
                } catch (Exception e) {
                    Log.w(TAG, "Error closing ParcelFileDescriptor", e);
                }
            }
        }
    }


    public void release() {
        Log.d(TAG, "Releasing PdfProcessor resources");
        if (textRecognizer != null) {
            textRecognizer.close();
            Log.d(TAG, "TextRecognizer closed");
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            Log.d(TAG, "ExecutorService shutdown initiated");
        }
    }

    public static class PdfProcessingResult {
        private boolean success;
        private String message;
        private String extractedText;

        public PdfProcessingResult(boolean success, String message, String extractedText) {
            this.success = success;
            this.message = message;
            this.extractedText = extractedText;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getExtractedText() { return extractedText; }
    }
}