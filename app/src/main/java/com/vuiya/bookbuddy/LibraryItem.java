package com.vuiya.bookbuddy;

public class LibraryItem {
    private String title;
    private String author;
    private String language;
    private String type; // Book, PDF, etc.
    private String filePath; // Path to the file
    private long dateAdded; // Timestamp when added

    public LibraryItem(String title, String author, String language, String type) {
        this.title = title;
        this.author = author;
        this.language = language;
        this.type = type;
        this.dateAdded = System.currentTimeMillis();
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }
}