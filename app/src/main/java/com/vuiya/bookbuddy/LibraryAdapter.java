package com.vuiya.bookbuddy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.LibraryViewHolder> {

    private List<LibraryItem> libraryItems;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(LibraryItem item);
        void onDeleteClick(LibraryItem item);
    }

    public LibraryAdapter(List<LibraryItem> libraryItems, OnItemClickListener listener) {
        this.libraryItems = libraryItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LibraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_library, parent, false);
        return new LibraryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LibraryViewHolder holder, int position) {
        LibraryItem item = libraryItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return libraryItems.size();
    }

    class LibraryViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle, tvAuthor, tvLanguage, tvType;
        private ImageButton btnDelete;

        public LibraryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvLanguage = itemView.findViewById(R.id.tv_language);
            tvType = itemView.findViewById(R.id.tv_type);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(LibraryItem item) {
            tvTitle.setText(item.getTitle());
            tvAuthor.setText(item.getAuthor());
            tvLanguage.setText(item.getLanguage());
            tvType.setText(item.getType());

            // Set click listeners
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(libraryItems.get(position));
                        }
                    }
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onDeleteClick(libraryItems.get(position));
                        }
                    }
                }
            });
        }
    }
}