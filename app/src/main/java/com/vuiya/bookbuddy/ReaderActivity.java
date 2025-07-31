package com.vuiya.bookbuddy;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReaderActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "ReaderActivity";
    private ImageButton btnBack;
    private TextView tvTitle, tvContent, tvPageIndicator;
    private ScrollView scrollContent;
    private View btnPrevious, btnNext, btnCopy, btnShare, btnTranslate;

    // TTS Views
    private LinearLayout layoutTtsControls;
    private ImageButton btnTtsPrevious, btnTtsPlayPause, btnTtsStop, btnTtsNext;

    private String bookTitle = "Sample Book";
    private List<String> pagesContent = new ArrayList<>(); // Store content for each page
    private int currentPageIndex = 0; // 0-based index

    // TTS Engine
    private TextToSpeech textToSpeech;
    private boolean ttsInitialized = false;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        initViews();
        setupClickListeners();

        // Initialize TTS Engine
        textToSpeech = new TextToSpeech(this, this);

        // Get content from Intent (either from Camera OCR or PDF processing)
        String fullContent = getIntent().getStringExtra("book_content");
        Log.d(TAG, "Received content from intent, length: " + (fullContent != null ? fullContent.length() : 0));

        bookTitle = getIntent().getStringExtra("book_title");
        Log.d(TAG, "Received title from intent: " + bookTitle);

        if (bookTitle == null || bookTitle.isEmpty()) {
            bookTitle = "Untitled Document";
        }

        // If we have content, split it into pages
        // For now, we'll split by a simple heuristic (e.g., every 500 words or "--- Page X ---" markers)
        // In a real implementation, you might have more sophisticated page splitting logic
        if (fullContent != null && !fullContent.isEmpty()) {
            Log.d(TAG, "Splitting received content into pages");
            splitContentIntoPages(fullContent);
        } else {
            Log.d(TAG, "No content received, using default page");
            // Add a default page if no content
            pagesContent.add("No content available. This is a placeholder page.");
        }

        displayCurrentPage();
        updatePageIndicator();
        updateNavigationButtons();
        updateTtsButtons();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        tvContent = findViewById(R.id.tv_content);
        tvPageIndicator = findViewById(R.id.tv_page_indicator);
        scrollContent = findViewById(R.id.scroll_content);

        btnPrevious = findViewById(R.id.btn_previous);
        btnNext = findViewById(R.id.btn_next);
        btnCopy = findViewById(R.id.btn_copy);
        btnShare = findViewById(R.id.btn_share);
        btnTranslate = findViewById(R.id.btn_translate);

        // TTS Views
        layoutTtsControls = findViewById(R.id.layout_tts_controls);
        btnTtsPrevious = findViewById(R.id.btn_tts_previous);
        btnTtsPlayPause = findViewById(R.id.btn_tts_play_pause);
        btnTtsStop = findViewById(R.id.btn_tts_stop);
        btnTtsNext = findViewById(R.id.btn_tts_next);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnPrevious.setOnClickListener(v -> {
            if (currentPageIndex > 0) {
                currentPageIndex--;
                displayCurrentPage();
                updatePageIndicator();
                updateNavigationButtons();
                updateTtsButtons();
                scrollToTop();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPageIndex < pagesContent.size() - 1) {
                currentPageIndex++;
                displayCurrentPage();
                updatePageIndicator();
                updateNavigationButtons();
                updateTtsButtons();
                scrollToTop();
            }
        });

        btnCopy.setOnClickListener(v -> copyCurrentPageText());
        btnShare.setOnClickListener(v -> shareCurrentPageText());
        btnTranslate.setOnClickListener(v -> translateCurrentPageText());

        // TTS Button Listeners
        btnTtsPrevious.setOnClickListener(v -> {
            if (isPlaying) {
                stopTts();
            }
            if (currentPageIndex > 0) {
                currentPageIndex--;
                displayCurrentPage();
                updatePageIndicator();
                updateNavigationButtons();
                updateTtsButtons();
                scrollToTop();
                // Optionally start playing the new page automatically
                // playCurrentPage();
            }
        });

        btnTtsPlayPause.setOnClickListener(v -> {
            if (isPlaying) {
                pauseTts();
            } else {
                playCurrentPage();
            }
        });

        btnTtsStop.setOnClickListener(v -> stopTts());

        btnTtsNext.setOnClickListener(v -> {
            if (isPlaying) {
                stopTts();
            }
            if (currentPageIndex < pagesContent.size() - 1) {
                currentPageIndex++;
                displayCurrentPage();
                updatePageIndicator();
                updateNavigationButtons();
                updateTtsButtons();
                scrollToTop();
                // Optionally start playing the new page automatically
                // playCurrentPage();
            }
        });
    }

    private void splitContentIntoPages(String fullContent) {
        pagesContent.clear();

        // Simple splitting logic - you can make this more sophisticated
        // For this example, we'll split by "--- Page X ---" markers if they exist
        // Otherwise, we'll split into chunks of roughly 1000 characters
        if (fullContent.contains("--- Page ")) {
            // Split by page markers
            Log.d(TAG, "Splitting content by page markers");
            String[] pageSections = fullContent.split("--- Page \\d+ ---");
            for (String section : pageSections) {
                String trimmedSection = section.trim();
                if (!trimmedSection.isEmpty()) {
                    pagesContent.add(trimmedSection);
                }
            }
        } else {
            // Simple character-based splitting
            Log.d(TAG, "Splitting content by character chunks");
            int chunkSize = 1500; // Roughly 250-300 words
            int length = fullContent.length();
            for (int i = 0; i < length; i += chunkSize) {
                int end = Math.min(length, i + chunkSize);
                pagesContent.add(fullContent.substring(i, end));
            }
        }

        // Ensure we have at least one page
        if (pagesContent.isEmpty()) {
            Log.d(TAG, "No pages created, adding default page");
            pagesContent.add("Content could not be processed into pages.");
        }

        Log.d(TAG, "Total pages created: " + pagesContent.size());
    }

    private void displayCurrentPage() {
        if (currentPageIndex >= 0 && currentPageIndex < pagesContent.size()) {
            String pageContent = pagesContent.get(currentPageIndex);
            Log.d(TAG, "Displaying page " + (currentPageIndex + 1) + ", content length: " +
                    (pageContent != null ? pageContent.length() : 0));
            tvContent.setText(pageContent);
            tvTitle.setText(bookTitle);
        }
    }

    private void updatePageIndicator() {
        int currentPageDisplay = currentPageIndex + 1; // 1-based for display
        int totalPages = pagesContent.size();
        String indicatorText = String.format("Page %d of %d", currentPageDisplay, totalPages);
        Log.d(TAG, "Updating page indicator: " + indicatorText);
        tvPageIndicator.setText(indicatorText);
    }

    private void updateNavigationButtons() {
        boolean hasPrevious = currentPageIndex > 0;
        boolean hasNext = currentPageIndex < pagesContent.size() - 1;

        btnPrevious.setEnabled(hasPrevious);
        btnNext.setEnabled(hasNext);

        // Optional: Change button appearance based on state
        btnPrevious.setAlpha(hasPrevious ? 1.0f : 0.5f);
        btnNext.setAlpha(hasNext ? 1.0f : 0.5f);

        Log.d(TAG, "Navigation buttons updated - Previous: " + hasPrevious + ", Next: " + hasNext);
    }

    private void updateTtsButtons() {
        boolean hasPrevious = currentPageIndex > 0;
        boolean hasNext = currentPageIndex < pagesContent.size() - 1;

        btnTtsPrevious.setEnabled(hasPrevious);
        btnTtsNext.setEnabled(hasNext);

        // Update play/pause button icon
        if (isPlaying) {
            btnTtsPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            btnTtsPlayPause.setImageResource(R.drawable.ic_play_arrow);
        }

        // Optional: Change button appearance based on state
        btnTtsPrevious.setAlpha(hasPrevious ? 1.0f : 0.5f);
        btnTtsNext.setAlpha(hasNext ? 1.0f : 0.5f);
    }

    private void scrollToTop() {
        scrollContent.scrollTo(0, 0);
    }

    private void copyCurrentPageText() {
        if (currentPageIndex >= 0 && currentPageIndex < pagesContent.size()) {
            String currentPageText = pagesContent.get(currentPageIndex);
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Page Content", currentPageText);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Page content copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCurrentPageText() {
        if (currentPageIndex >= 0 && currentPageIndex < pagesContent.size()) {
            String currentPageText = pagesContent.get(currentPageIndex);
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, currentPageText);
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, bookTitle + " - Page " + (currentPageIndex + 1));
            startActivity(android.content.Intent.createChooser(shareIntent, "Share via"));
        }
    }

    private void translateCurrentPageText() {
        Toast.makeText(this, "Translation functionality will be implemented in full version",
                Toast.LENGTH_SHORT).show();
        // In the future, this would trigger translation of the current page's text
    }

    // TTS Methods
    private void playCurrentPage() {
        if (!ttsInitialized) {
            Toast.makeText(this, "TTS engine is not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentPageIndex >= 0 && currentPageIndex < pagesContent.size()) {
            String textToRead = pagesContent.get(currentPageIndex);
            if (textToRead != null && !textToRead.trim().isEmpty()) {
                // Stop any ongoing speech
                textToSpeech.stop();

                // Speak the text
                textToSpeech.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "PAGE_READING");
                isPlaying = true;
                updateTtsButtons();
                Log.d(TAG, "Started TTS for page " + (currentPageIndex + 1));
            } else {
                Toast.makeText(this, "No text to read on this page", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void pauseTts() {
        if (ttsInitialized && isPlaying) {
            textToSpeech.stop(); // TTS doesn't have a true pause, so we stop
            isPlaying = false;
            updateTtsButtons();
            Log.d(TAG, "Paused TTS");
        }
    }

    private void stopTts() {
        if (ttsInitialized) {
            textToSpeech.stop();
            isPlaying = false;
            updateTtsButtons();
            Log.d(TAG, "Stopped TTS");
        }
    }

    // TextToSpeech.OnInitListener method
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language - you might want to make this configurable
            int result = textToSpeech.setLanguage(Locale.getDefault()); // Or Locale.US, Locale.UK, etc.

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS language is not supported");
                Toast.makeText(this, "TTS language is not supported on your device", Toast.LENGTH_LONG).show();
            } else {
                ttsInitialized = true;
                Log.d(TAG, "TTS engine initialized successfully");
                runOnUiThread(() -> updateTtsButtons());
            }
        } else {
            Log.e(TAG, "Failed to initialize TTS engine");
            Toast.makeText(this, "Failed to initialize Text-to-Speech engine", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        // Shutdown TTS engine
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            Log.d(TAG, "TTS engine shutdown");
        }
        super.onDestroy();
    }
}