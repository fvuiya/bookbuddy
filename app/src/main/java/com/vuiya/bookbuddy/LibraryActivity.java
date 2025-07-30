package com.vuiya.bookbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LibraryActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView recyclerLibrary;
    private TextView tvEmptyLibrary;
    private LibraryAdapter libraryAdapter;
    private List<LibraryItem> libraryItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        initViews();
        setupClickListeners();
        setupRecyclerView();
        loadLibraryItems();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        recyclerLibrary = findViewById(R.id.recycler_library);
        tvEmptyLibrary = findViewById(R.id.tv_empty_library);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setupRecyclerView() {
        libraryItems = new ArrayList<>();
        libraryAdapter = new LibraryAdapter(libraryItems, new LibraryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(LibraryItem item) {
                // Navigate to Reader Activity
                Intent intent = new Intent(LibraryActivity.this, ReaderActivity.class);
                intent.putExtra("book_title", item.getTitle());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(LibraryItem item) {
                // TODO: Handle item deletion
                removeLibraryItem(item);
            }
        });

        recyclerLibrary.setLayoutManager(new LinearLayoutManager(this));
        recyclerLibrary.setAdapter(libraryAdapter);
    }

    private void loadLibraryItems() {
        // TODO: Load actual library items from storage/database
        // For now, we'll add some sample items
        libraryItems.add(new LibraryItem("The Great Gatsby", "F. Scott Fitzgerald", "English", "Book"));
        libraryItems.add(new LibraryItem("To Kill a Mockingbird", "Harper Lee", "English", "Book"));
        libraryItems.add(new LibraryItem("1984", "George Orwell", "English", "Book"));
        libraryItems.add(new LibraryItem("Pride and Prejudice", "Jane Austen", "English", "Book"));
        libraryItems.add(new LibraryItem("The Catcher in the Rye", "J.D. Salinger", "English", "Book"));

        // Update UI
        updateLibraryUI();
    }

    private void updateLibraryUI() {
        if (libraryItems.isEmpty()) {
            recyclerLibrary.setVisibility(View.GONE);
            tvEmptyLibrary.setVisibility(View.VISIBLE);
        } else {
            recyclerLibrary.setVisibility(View.VISIBLE);
            tvEmptyLibrary.setVisibility(View.GONE);
            libraryAdapter.notifyDataSetChanged();
        }
    }

    private void removeLibraryItem(LibraryItem item) {
        libraryItems.remove(item);
        libraryAdapter.notifyDataSetChanged();
        updateLibraryUI();
        Toast.makeText(this, "Removed: " + item.getTitle(), Toast.LENGTH_SHORT).show();
    }
}