package com.vuiya.bookbuddy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
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

public class PdfProcessor {
    private static final String TAG = "PdfProcessor";
    private Context context;
    private TextRecognizer textRecognizer;
    private ExecutorService executorService;
    private PdfProgressListener progressListener; // Added listener

    // Interface for progress updates
    public interface PdfProgressListener {
        void onProgressUpdate(int currentPage, int totalPages);
    }

    public PdfProcessor(Context context, PdfProgressListener listener) { // Added listener to constructor
        this.context = context;
        this.progressListener = listener; // Store the listener

        try {
            PDFBoxResourceLoader.init(context.getApplicationContext());
            Log.d(TAG, "PDFBoxResourceLoader initialized successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize PDFBoxResourceLoader.", e);
        }

        this.textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public CompletableFuture<PdfProcessingResult> processPdf(Uri pdfUri) {
        CompletableFuture<PdfProcessingResult> futureResult = new CompletableFuture<>();

        executorService.submit(() -> {
            File tempPdfFile = null;
            try {
                Log.d(TAG, "Starting PDF processing for URI: " + pdfUri.toString());
                tempPdfFile = copyPdfToTempFile(pdfUri);
                if (tempPdfFile == null) {
                    Log.e(TAG, "Failed to copy PDF to temporary file");
                    futureResult.complete(new PdfProcessingResult(false, "Failed to copy PDF to temporary file", null));
                    return;
                }
                Log.d(TAG, "PDF copied to temporary file: " + tempPdfFile.getAbsolutePath());

                Log.d(TAG, "Attempting to extract text from digital PDF...");
                String extractedText = extractTextFromDigitalPdf(tempPdfFile);
                if (extractedText != null && !extractedText.trim().isEmpty()) {
                    Log.d(TAG, "Digital PDF detected, extracted text directly");
                    futureResult.complete(new PdfProcessingResult(true, "Digital PDF processed", extractedText));
                    return;
                }

                Log.d(TAG, "Scanned PDF detected or digital extraction failed, performing OCR on pages...");
                String ocrText = performOcrOnScannedPdf(tempPdfFile);
                if (ocrText != null && !ocrText.trim().isEmpty()) {
                    Log.d(TAG, "Scanned PDF processed with OCR successfully");
                    futureResult.complete(new PdfProcessingResult(true, "Scanned PDF processed with OCR", ocrText));
                } else {
                    Log.w(TAG, "Failed to process PDF - no text found after OCR attempt");
                    if (progressListener != null) { 
                        progressListener.onProgressUpdate(0, 0); 
                    }
                    futureResult.complete(new PdfProcessingResult(false, "Failed to process PDF - no text found", null));
                }
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during PDF processing", e);
                if (progressListener != null) { 
                    progressListener.onProgressUpdate(0, 0); 
                }
                futureResult.completeExceptionally(e);
            } finally {
                if (tempPdfFile != null) {
                    tempPdfFile.delete();
                    Log.d(TAG, "Deleted temporary PDF file: " + tempPdfFile.getAbsolutePath());
                }
            }
        });

        return futureResult;
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
        int totalPages = 0;
        try {
            Log.d(TAG, "Loading PDDocument from file: " + pdfFile.getAbsolutePath());
            document = PDDocument.load(pdfFile);
            totalPages = document.getNumberOfPages();
            Log.d(TAG, "PDDocument loaded successfully. Number of pages: " + totalPages);

            if (progressListener != null) {
                progressListener.onProgressUpdate(0, totalPages); 
            }

            PDFTextStripper pdfStripper;
            try {
                pdfStripper = new PDFTextStripper();
            } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
                Log.e(TAG, "Failed to initialize PDFTextStripper.", e);
                return null;
            }

            String text = pdfStripper.getText(document);
            Log.d(TAG, "Text extracted successfully. Length: " + (text != null ? text.length() : 0));

            if (progressListener != null) {
                progressListener.onProgressUpdate(totalPages, totalPages); 
            }
            return text;
        } catch (NoClassDefFoundError | ExceptionInInitializerError | UnsatisfiedLinkError e) {
            Log.w(TAG, "Failed to extract text from PDF (Class/Native loading issue)", e);
            return null;
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract text from PDF (might be scanned or corrupted)", e);
            return null;
        } finally {
            if (document != null) {
                try {
                    document.close();
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
        boolean ocrPerformedAtLeastOnce = false;
        int pageCount = 0;

        try {
            Log.d(TAG, "Starting OCR on scanned PDF: " + pdfFile.getAbsolutePath());
            parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
            pageCount = pdfRenderer.getPageCount();
            Log.d(TAG, "PDF has " + pageCount + " pages for OCR.");

            if (pageCount == 0) {
                Log.w(TAG, "PDF has 0 pages, nothing to OCR.");
                if (progressListener != null) {
                    progressListener.onProgressUpdate(0, 0); 
                }
                return "";
            }
            
            for (int i = 0; i < pageCount; i++) { 
                if (progressListener != null) {
                    progressListener.onProgressUpdate(i, pageCount); 
                }
                Log.d(TAG, "Processing page " + (i + 1) + "/" + pageCount);
                PdfRenderer.Page page = null; 
                Bitmap bitmap = null; 
                try {
                    page = pdfRenderer.openPage(i);
                    int renderedWidth = page.getWidth() * 2;
                    int renderedHeight = page.getHeight() * 2;
                    bitmap = Bitmap.createBitmap(renderedWidth, renderedHeight, Bitmap.Config.ARGB_8888);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                    InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
                    Text textResult = Tasks.await(textRecognizer.process(inputImage));
                    ocrPerformedAtLeastOnce = true;

                    for (Text.TextBlock block : textResult.getTextBlocks()) {
                        ocrText.append(block.getText()).append("\n");
                    }
                    ocrText.append("\n--- Page ").append(i + 1).append(" ---\n\n");
                    Log.d(TAG, "Page " + (i + 1) + " OCR complete.");

                } finally {
                    if (bitmap != null) bitmap.recycle();
                    if (page != null) page.close();
                    Log.d(TAG, "Closed page " + (i + 1));
                }
            }

            if (progressListener != null) {
                progressListener.onProgressUpdate(pageCount, pageCount); 
            }

            if (ocrPerformedAtLeastOnce) {
                return ocrText.toString();
            } else {
                Log.w(TAG, "No OCR was performed on any page, though pages were present.");
                return null; 
            }

        } catch (Exception e) {
            Log.e(TAG, "Error performing OCR on scanned PDF", e);
            if (progressListener != null) {
                progressListener.onProgressUpdate(pageCount, pageCount); 
            }
            return null;
        } finally {
            if (pdfRenderer != null) pdfRenderer.close();
            if (parcelFileDescriptor != null) try { parcelFileDescriptor.close(); } catch (IOException ignored) {}
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

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getExtractedText() { return extractedText; }
    }
}
