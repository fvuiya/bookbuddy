
# BookBuddy Master Plan

This document outlines the development roadmap for BookBuddy, a mobile application for managing and reading books.

## App Description

BookBuddy allows users to:

*   Organize their personal library of books.
*   Import books from PDF files.
*   Extract text from images using OCR technology.
*   Read books in a dedicated reader interface.
*   Generate AI-powered summaries of their books.
*   Create PDF files from pictures.
*   Listen to books using text-to-speech (TTS).
*   Translate books with various integrated services.
*   Share their own books and download books created by other users.
*   Connect with friends and get updates on their new book publications.

## High-Level Goals

The primary goals for BookBuddy's future development are:

1.  **Modernize the UI:** Migrate the existing UI from XML layouts to Jetpack Compose for a more declarative and maintainable codebase.
2.  **Improve Performance:** Optimize the app's performance, particularly in the areas of PDF processing and OCR.
3.  **Enhance User Experience:** Add new features and refine existing ones to create a more intuitive and engaging user experience.
4.  **Increase Test Coverage:** Write comprehensive unit and integration tests to ensure the app's stability and reliability.
5.  **Integrate AI & Advanced Content Features:** Add capabilities like summarization, TTS, and multi-option translation.
6.  **Build an Online Community:** Create a platform for users to share and discover books.

## Detailed Roadmap

### Phase 1: Foundational Improvements

*   **Update Dependencies:**
    *   [X] Upgrade all libraries to their latest stable versions.
    *   [X] Replace deprecated libraries with modern alternatives.
*   **Refactor to Kotlin:**
    *   [ ] Convert all existing Java code to Kotlin to take advantage of its modern features and improved syntax.
*   **Introduce Jetpack Compose:**
    *   [ ] Migrate the `MainActivity` to Jetpack Compose.
    *   [ ] Gradually migrate other screens to Compose, starting with the simpler ones like `SettingsActivity`.

### Phase 2: UI/UX Enhancements

*   **Redesign the Library:**
    *   [ ] Implement a more visually appealing and user-friendly design for the `LibraryActivity`.
    *   [ ] Add options for sorting and filtering the user's book collection.
*   **Improve the Reader:**
    *   [ ] Enhance the `ReaderActivity` with features like adjustable font sizes, themes, and bookmarks.
    *   [ ] Add support for more file formats, such as EPUB.

### Phase 3: New Features

*   **Online Community Platform:**
    *   [ ] Design and implement a backend service for user authentication and book storage.
    *   [ ] Create a UI for users to upload their books to the online database.
    *   [ ] Develop a browsable and searchable public library of user-created books.
    *   [ ] Implement a system for users to download books from the public library to their personal library.
    *   [ ] Implement a friend request system allowing users to connect with each other.
    *   [ ] Create a notification feed or system to inform users about new books published by their friends.
*   **Advanced OCR:**
    *   [ ] Explore more advanced OCR techniques to improve the accuracy of text extraction.
*   **AI-Powered Summarization:**
    *   [ ] Implement a feature to generate summaries of books using an AI model.
*   **Image-to-PDF Creation:**
    *   [ ] Develop a feature to capture or select multiple images and compile them into a single PDF file.
*   **Audiobook & Text-to-Speech:**
    *   [ ] Integrate a TTS engine to read books aloud.
    *   [ ] Investigate and implement options for creating and saving audiobook files from text, a potentially using AI for more natural voices.
*   **Multi-Language Support & Translation:**
    *   [ ] Implement multi-language support for the app's user interface.
    *   [ ] Add a book translation feature with multiple provider options (offline, Google Translate, and AI for tone-maintained translation).

### Phase 4: Testing and Release

*   **Write Unit Tests:**
    *   [ ] Write comprehensive unit tests for all major components of the app.
*   **Conduct User Testing:**
    *   [ ] Gather feedback from a group of beta testers to identify and address any usability issues.
*   **Prepare for Release:**
    *   [ ] Finalize the app's branding and marketing materials.
    *   [ ] Publish the app to the Google Play Store.

## Build Environment

*   **Gradle Version:** 8.13

## Changelog

*   Upgraded `androidx.appcompat:appcompat` to version `1.7.1`.
*   Upgraded `com.google.android.material:material` to version `1.13.0`.
*   Fixed compilation errors in `ReaderActivity` and `PdfSelectorActivity`.
*   Upgraded `androidx.constraintlayout:constraintlayout` to version `2.2.1`.
*   Upgraded all remaining project dependencies to their latest stable versions.
*   Refactored `PdfSelectorActivity` to use the modern `ActivityResultLauncher` API.
