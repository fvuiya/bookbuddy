package com.vuiya.bookbuddy;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ReaderActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvTitle, tvContent;
    private String bookTitle = "Sample Book";
    private String bookContent = "This is a sample book content. In the full implementation, this would show the OCR'd text from your scanned book or PDF.\n\n" +
            "The text would be processed and translated according to your selected language preferences.\n\n" +
            "You would be able to switch between original and translated text, copy content, and use text-to-speech features.\n\n" +
            "For now, this is just a placeholder to demonstrate the app flow.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        initViews();
        setupClickListeners();
        displayContent();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        tvContent = findViewById(R.id.tv_content);
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

        // Share text button
        findViewById(R.id.btn_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareText();
            }
        });

        // Translate button
        findViewById(R.id.btn_translate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                translateText();
            }
        });
    }

    private void displayContent() {
        tvTitle.setText(bookTitle);
        tvContent.setText(bookContent);
    }

    private void copyText() {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Book Content", bookContent);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void shareText() {
        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, bookContent);
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, bookTitle);
        startActivity(android.content.Intent.createChooser(shareIntent, "Share via"));
    }

    private void translateText() {
        Toast.makeText(this, "Translation functionality will be implemented in full version",
                Toast.LENGTH_SHORT).show();
    }
}