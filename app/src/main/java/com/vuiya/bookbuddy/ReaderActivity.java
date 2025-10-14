package com.vuiya.bookbuddy;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReaderActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "ReaderActivity";
    private static final String UTTERANCE_ID = "PAGE_UTTERANCE";

    // Standard Views
    private Toolbar toolbar;
    private TextView tvTitle, tvContent, tvPageIndicator;
    private ScrollView scrollContent;
    private View btnPrevious, btnNext, btnCopy, btnShare, btnTranslate;
    private LinearLayout layoutStandardControls;

    // TTS Views
    private LinearLayout layoutTtsMode;
    private ImageButton btnTtsPreviousPage, btnTtsPlayPause, btnTtsStop, btnTtsNextPage;
    private TextView tvTtsStatus;
    private Button btnExitTtsMode;

    private String bookTitle = "Sample Book";
    private List<String> pagesContent = new ArrayList<>();
    private int currentPageIndex = 0;

    // TTS Engine
    private TextToSpeech textToSpeech;
    private boolean ttsInitialized = false;
    private boolean isPlaying = false;
    private boolean autoPlayEnabled = true; // Default to auto-play

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        initViews();
        setupToolbar();
        setupClickListeners();

        // Initialize TTS Engine
        textToSpeech = new TextToSpeech(this, this);
        // Set up utterance listener for auto-play
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                runOnUiThread(() -> {
                    if (UTTERANCE_ID.equals(utteranceId)) {
                        isPlaying = true;
                        updateTtsUi();
                        Log.d(TAG, "TTS started for page " + (currentPageIndex + 1));
                    }
                });
            }

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    if (UTTERANCE_ID.equals(utteranceId)) {
                        Log.d(TAG, "TTS finished for page " + (currentPageIndex + 1));
                        isPlaying = false;
                        updateTtsUi();

                        // Handle auto-play to next page
                        if (autoPlayEnabled && currentPageIndex < pagesContent.size() - 1) {
                            Log.d(TAG, "Auto-play enabled, moving to next page");
                            moveToNextPageAndPlay();
                        } else if (autoPlayEnabled && currentPageIndex >= pagesContent.size() - 1) {
                            Log.d(TAG, "Reached last page, stopping TTS mode");
                            // Optionally, you could just stop, or show a message
                            // For now, we'll just update the status
                            tvTtsStatus.setText("Finished reading");
                        }
                    }
                });
            }

            @Override
            public void onError(String utteranceId) {
                runOnUiThread(() -> {
                    if (UTTERANCE_ID.equals(utteranceId)) {
                        Log.e(TAG, "TTS error for page " + (currentPageIndex + 1));
                        isPlaying = false;
                        updateTtsUi();
                        Toast.makeText(ReaderActivity.this, "Error in text-to-speech", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Get content from Intent
        String fullContent = getIntent().getStringExtra("book_content");
        bookTitle = getIntent().getStringExtra("book_title");
        if (bookTitle == null || bookTitle.isEmpty()) {
            bookTitle = "Untitled Document";
        }

        if (fullContent != null && !fullContent.isEmpty()) {
            splitContentIntoPages(fullContent);
        } else {
            pagesContent.add("No content available. This is a placeholder page.");
        }

        displayCurrentPage();
        updatePageIndicator();
        updateNavigationButtons();
        // Initially, TTS UI is hidden
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvTitle = findViewById(R.id.tv_title); // Although Toolbar handles title, keep reference if needed elsewhere
        tvContent = findViewById(R.id.tv_content);
        tvPageIndicator = findViewById(R.id.tv_page_indicator);
        scrollContent = findViewById(R.id.scroll_content);

        btnPrevious = findViewById(R.id.btn_previous);
        btnNext = findViewById(R.id.btn_next);
        btnCopy = findViewById(R.id.btn_copy);
        btnShare = findViewById(R.id.btn_share);
        btnTranslate = findViewById(R.id.btn_translate);
        layoutStandardControls = findViewById(R.id.layout_standard_controls);

        // TTS Views
        layoutTtsMode = findViewById(R.id.layout_tts_mode);
        btnTtsPreviousPage = findViewById(R.id.btn_tts_previous_page);
        btnTtsPlayPause = findViewById(R.id.btn_tts_play_pause);
        btnTtsStop = findViewById(R.id.btn_tts_stop);
        btnTtsNextPage = findViewById(R.id.btn_tts_next_page);
        tvTtsStatus = findViewById(R.id.tv_tts_status);
        btnExitTtsMode = findViewById(R.id.btn_exit_tts_mode);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(bookTitle);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable back button
        }
    }

    private void setupClickListeners() {
        // Standard Navigation
        btnPrevious.setOnClickListener(v -> {
            if (currentPageIndex > 0) {
                stopTtsIfPlaying(); // Stop TTS if navigating manually
                currentPageIndex--;
                displayCurrentPage();
                updatePageIndicator();
                updateNavigationButtons();
                scrollToTop();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPageIndex < pagesContent.size() - 1) {
                stopTtsIfPlaying(); // Stop TTS if navigating manually
                currentPageIndex++;
                displayCurrentPage();
                updatePageIndicator();
                updateNavigationButtons();
                scrollToTop();
            }
        });

        // Standard Actions
        btnCopy.setOnClickListener(v -> copyCurrentPageText());
        btnShare.setOnClickListener(v -> shareCurrentPageText());
        btnTranslate.setOnClickListener(v -> translateCurrentPageText());

        // TTS Mode Button Listeners
        btnTtsPreviousPage.setOnClickListener(v -> {
            if (currentPageIndex > 0) {
                stopTtsIfPlaying();
                currentPageIndex--;
                displayCurrentPage();
                updatePageIndicator();
                updateNavigationButtons();
                scrollToTop();
                // Optionally start playing the new page automatically
                playCurrentPage();
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

        btnTtsNextPage.setOnClickListener(v -> {
            if (currentPageIndex < pagesContent.size() - 1) {
                stopTtsIfPlaying();
                currentPageIndex++;
                displayCurrentPage();
                updatePageIndicator();
                updateNavigationButtons();
                scrollToTop();
                // Optionally start playing the new page automatically
                playCurrentPage();
            } else if (currentPageIndex >= pagesContent.size() - 1) {
                // If on last page, stop
                stopTts();
                tvTtsStatus.setText("Finished reading");
            }
        });

        btnExitTtsMode.setOnClickListener(v -> exitTtsMode());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reader_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // Handle back button press
            finish();
            return true;
        } else if (id == R.id.action_listen_audio) {
            // Handle "Listen Audio" menu item
            enterTtsMode();
            return true;
        }
        // Handle other menu items if you add them
        return super.onOptionsItemSelected(item);
    }

    private void enterTtsMode() {
        if (!ttsInitialized) {
            Toast.makeText(this, "TTS engine is initializing, please try again shortly", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hide standard controls and show TTS mode controls
        layoutStandardControls.setVisibility(View.GONE);
        layoutTtsMode.setVisibility(View.VISIBLE);

        // Update TTS UI state
        updateTtsUi();
        tvTtsStatus.setText("TTS Mode: Ready");

        // Optionally start playing immediately
        // playCurrentPage();
        Toast.makeText(this, "Entered Audio Mode", Toast.LENGTH_SHORT).show();
    }

    private void exitTtsMode() {
        // Stop any ongoing TTS
        stopTtsIfPlaying();

        // Show standard controls and hide TTS mode controls
        layoutTtsMode.setVisibility(View.GONE);
        layoutStandardControls.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Exited Audio Mode", Toast.LENGTH_SHORT).show();
    }

    private void moveToNextPageAndPlay() {
        if (currentPageIndex < pagesContent.size() - 1) {
            currentPageIndex++;
            displayCurrentPage();
            updatePageIndicator();
            updateNavigationButtons();
            scrollToTop();

            // Play the next page after a short delay to allow UI update
            tvTtsStatus.setText("Moving to next page...");
            scrollContent.postDelayed(() -> {
                playCurrentPage();
            }, 500); // 500ms delay
        }
    }

    private void splitContentIntoPages(String fullContent) {
        pagesContent.clear();

        if (fullContent.contains("--- Page ")) {
            String[] pageSections = fullContent.split("--- Page \\d+ ---");
            for (String section : pageSections) {
                String trimmedSection = section.trim();
                if (!trimmedSection.isEmpty()) {
                    pagesContent.add(trimmedSection);
                }
            }
        } else {
            int chunkSize = 1500;
            int length = fullContent.length();
            for (int i = 0; i < length; i += chunkSize) {
                int end = Math.min(length, i + chunkSize);
                pagesContent.add(fullContent.substring(i, end));
            }
        }

        if (pagesContent.isEmpty()) {
            pagesContent.add("Content could not be processed into pages.");
        }
    }

    private void displayCurrentPage() {
        if (currentPageIndex >= 0 && currentPageIndex < pagesContent.size()) {
            String pageContent = pagesContent.get(currentPageIndex);
            tvContent.setText(pageContent);
            // Toolbar title is handled by setSupportActionBar, but if you need dynamic update:
            // if (getSupportActionBar() != null) getSupportActionBar().setTitle(bookTitle);
        }
    }

    private void updatePageIndicator() {
        int currentPageDisplay = currentPageIndex + 1;
        int totalPages = pagesContent.size();
        String indicatorText = String.format("Page %d of %d", currentPageDisplay, totalPages);
        tvPageIndicator.setText(indicatorText);
    }

    private void updateNavigationButtons() {
        boolean hasPrevious = currentPageIndex > 0;
        boolean hasNext = currentPageIndex < pagesContent.size() - 1;

        // These are only relevant in standard mode, but good to keep updated
        if (btnPrevious != null) btnPrevious.setEnabled(hasPrevious);
        if (btnNext != null) btnNext.setEnabled(hasNext);

        if (btnPrevious != null) btnPrevious.setAlpha(hasPrevious ? 1.0f : 0.5f);
        if (btnNext != null) btnNext.setAlpha(hasNext ? 1.0f : 0.5f);
    }

    private void updateTtsUi() {
        if (layoutTtsMode.getVisibility() == View.VISIBLE) {
            boolean hasPrevious = currentPageIndex > 0;
            boolean hasNext = currentPageIndex < pagesContent.size() - 1;

            btnTtsPreviousPage.setEnabled(hasPrevious);
            btnTtsNextPage.setEnabled(hasNext);

            btnTtsPreviousPage.setAlpha(hasPrevious ? 1.0f : 0.5f);
            btnTtsNextPage.setAlpha(hasNext ? 1.0f : 0.5f);

            if (isPlaying) {
                btnTtsPlayPause.setImageResource(R.drawable.ic_pause);
                tvTtsStatus.setText("TTS Mode: Playing (Page " + (currentPageIndex + 1) + ")");
            } else {
                btnTtsPlayPause.setImageResource(R.drawable.ic_play_arrow);
                if (textToSpeech != null && textToSpeech.isSpeaking()) {
                    tvTtsStatus.setText("TTS Mode: Speaking...");
                } else {
                    tvTtsStatus.setText("TTS Mode: Stopped (Page " + (currentPageIndex + 1) + ")");
                }
            }
        }
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

                // Speak the text with an utterance ID for progress tracking
                textToSpeech.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
                // isPlaying flag will be set by UtteranceProgressListener.onStart
                Log.d(TAG, "Queued TTS for page " + (currentPageIndex + 1));
            } else {
                Toast.makeText(this, "No text to read on this page", Toast.LENGTH_SHORT).show();
                tvTtsStatus.setText("No text on this page");
            }
        }
    }

    private void pauseTts() {
        // TTS doesn't have a true pause, stop and restart from position if needed
        // For simplicity, we'll just stop
        if (ttsInitialized && (isPlaying || textToSpeech.isSpeaking())) {
            textToSpeech.stop();
            isPlaying = false;
            updateTtsUi();
            Log.d(TAG, "Paused/Stopped TTS");
        }
    }

    private void stopTts() {
        if (ttsInitialized) {
            textToSpeech.stop();
            isPlaying = false;
            updateTtsUi();
            Log.d(TAG, "Stopped TTS");
        }
    }

    private void stopTtsIfPlaying() {
        if (isPlaying || (textToSpeech != null && textToSpeech.isSpeaking())) {
            stopTts();
        }
    }

    // TextToSpeech.OnInitListener method
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS language is not supported");
                Toast.makeText(this, "TTS language is not supported on your device", Toast.LENGTH_LONG).show();
            } else {
                ttsInitialized = true;
                Log.d(TAG, "TTS engine initialized successfully");
                // Update UI if needed, but TTS mode might not be active yet
            }
        } else {
            Log.e(TAG, "Failed to initialize TTS engine");
            Toast.makeText(this, "Failed to initialize Text-to-Speech engine", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            Log.d(TAG, "TTS engine shutdown");
        }
        super.onDestroy();
    }
}