package com.vuiya.bookbuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LibraryAdapter(
    private val libraryItems: List<LibraryItem>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<LibraryAdapter.LibraryViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(item: LibraryItem)
        fun onDeleteClick(item: LibraryItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_library, parent, false)
        return LibraryViewHolder(view)
    }

    override fun onBindViewHolder(holder: LibraryViewHolder, position: Int) {
        val item = libraryItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = libraryItems.size

    inner class LibraryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tv_author)
        private val tvLanguage: TextView = itemView.findViewById(R.id.tv_language)
        private val tvType: TextView = itemView.findViewById(R.id.tv_type)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(item: LibraryItem) {
            tvTitle.text = item.title
            tvAuthor.text = item.author
            tvLanguage.text = item.language
            tvType.text = item.category

            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(libraryItems[position])
                }
            }

            btnDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(libraryItems[position])
                }
            }
        }
    }
}
