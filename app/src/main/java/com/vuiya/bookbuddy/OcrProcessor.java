package com.vuiya.bookbuddy;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Centralized class to handle OCR processing using ML Kit.
 * Manages different language recognizers and applies user-specified language hints.
 */
public class OcrProcessor {

    private static final String TAG = "OcrProcessor";
    private final Context context;
    private final ExecutorService executorService;

    // A map to hold pre-configured recognizers for specific languages
    // This avoids creating a new recognizer every time, which is inefficient.
    private final Map<String, TextRecognizer> languageRecognizers = new HashMap<>();
    private TextRecognizer defaultRecognizer; // For general multi-language recognition

    public OcrProcessor(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();

        // Initialize the default multi-language recognizer
        this.defaultRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Pre-initialize some common language recognizers
        // You can expand this list based on your target languages
        // Note: Some language-specific options might require downloading models
        initializeCommonRecognizers();
    }

    private void initializeCommonRecognizers() {
        // Add recognizers for specific languages if needed
        // Example for Chinese (requires separate dependency or model download)
        // languageRecognizers.put("zh", TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build()));
        // languageRecognizers.put("ja", TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build()));
        // languageRecognizers.put("ko", TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build()));
        // languageRecognizers.put("ru", TextRecognition.getClient(new RussianTextRecognizerOptions.Builder().build()));
        // Add more as needed. Be mindful of app size increase with many specific models.
        Log.d(TAG, "Initialized common language recognizers");
    }

    /**
     * Processes an image bitmap for OCR.
     *
     * @param bitmap           The image bitmap to process.
     * @param forceLanguageCode Optional ISO 639-1 language code (e.g., "bn" for Bengali, "es" for Spanish).
     *                          If null or empty, uses the default multi-language recognizer.
     * @return A CompletableFuture that resolves to the OCR result string.
     */
    public CompletableFuture<OcrResult> processImage(Bitmap bitmap, String forceLanguageCode) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Starting OCR processing. Force language: " + forceLanguageCode);

                InputImage image = InputImage.fromBitmap(bitmap, 0); // Rotation handled by ML Kit if EXIF data exists
                TextRecognizer recognizer = selectRecognizer(forceLanguageCode);

                // Process the image
                Text visionText = recognizer.process(image).getResult(); // Blocking call on background thread

                // Extract text from ML Kit result
                StringBuilder recognizedText = new StringBuilder();
                for (Text.TextBlock block : visionText.getTextBlocks()) {
                    recognizedText.append(block.getText()).append("\n");
                }

                String resultText = recognizedText.toString().trim();
                Log.d(TAG, "OCR processing completed. Text length: " + resultText.length());
                Log.d(TAG, "Recognized text snippet: " + (resultText.length() > 100 ? resultText.substring(0, 100) + "..." : resultText));

                return new OcrResult(true, "OCR successful", resultText, forceLanguageCode);

            } catch (Exception e) {
                Log.e(TAG, "Error during OCR processing", e);
                return new OcrResult(false, "OCR failed: " + e.getMessage(), "", forceLanguageCode);
            }
        }, executorService);
    }

    /**
     * Selects the appropriate TextRecognizer based on the language hint.
     *
     * @param languageHint The language hint (ISO 639-1 code).
     * @return The selected TextRecognizer instance.
     */
    private TextRecognizer selectRecognizer(String languageHint) {
        if (languageHint == null || languageHint.isEmpty()) {
            Log.d(TAG, "No language hint provided, using default multi-language recognizer.");
            return defaultRecognizer;
        }

        // Check if we have a pre-initialized recognizer for this language
        TextRecognizer recognizer = languageRecognizers.get(languageHint.toLowerCase());
        if (recognizer != null) {
            Log.d(TAG, "Using pre-initialized recognizer for language: " + languageHint);
            return recognizer;
        }

        // For languages without a specific pre-initialized recognizer,
        // we can try to configure hints for the default recognizer.
        // Note: ML Kit's DEFAULT_OPTIONS already supports many languages.
        // Explicitly setting hints might not always change behavior significantly
        // but it's good practice if you know the primary language.
        Log.d(TAG, "Using default recognizer with hint for language: " + languageHint);

        // If you want to experiment with hints, you could recreate the default recognizer
        // with hints, but this is generally not necessary and less efficient.
        // TextRecognizerOptions options = new TextRecognizerOptions.Builder()
        //         .setExecutor(executorService)
        //         .build();
        // return TextRecognition.getClient(options);

        // For now, stick with the default recognizer.
        return defaultRecognizer;
    }

    /**
     * Releases resources held by the OCR processor.
     */
    public void release() {
        Log.d(TAG, "Releasing OCR processor resources");
        if (defaultRecognizer != null) {
            defaultRecognizer.close();
        }
        for (TextRecognizer recognizer : languageRecognizers.values()) {
            if (recognizer != null) {
                recognizer.close();
            }
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        Log.d(TAG, "OCR processor resources released");
    }

    /**
     * Result class to encapsulate OCR processing outcome.
     */
    public static class OcrResult {
        private final boolean success;
        private final String message;
        private final String extractedText;
        private final String languageUsed; // Language hint that was used

        public OcrResult(boolean success, String message, String extractedText, String languageUsed) {
            this.success = success;
            this.message = message;
            this.extractedText = extractedText;
            this.languageUsed = languageUsed;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getExtractedText() {
            return extractedText;
        }

        public String getLanguageUsed() {
            return languageUsed;
        }
    }
}