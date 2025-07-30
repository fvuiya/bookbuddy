package com.vuiya.bookbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {

    private CardView btnCamera, btnPdf;
    private ImageButton btnLibrary, btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        btnCamera = findViewById(R.id.btn_camera);
        btnPdf = findViewById(R.id.btn_pdf);
        btnLibrary = findViewById(R.id.btn_library);
        btnSettings = findViewById(R.id.btn_settings);
    }

    private void setupClickListeners() {
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Camera Activity
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        btnPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to PDF Selector Activity
                Toast.makeText(MainActivity.this, "PDF feature coming soon!", Toast.LENGTH_SHORT).show();
            }
        });

        btnLibrary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Library feature coming soon!", Toast.LENGTH_SHORT).show();
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Settings feature coming soon!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}