
# BookBuddy Master Plan

This document outlines the development roadmap for BookBuddy, a mobile application for managing and reading books.

## App Description

BookBuddy allows users to:

*   Organize their personal library of books.
*   Import books from PDF and EPUB files.
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
7.  **Empower Creators:** Provide tools for authors to create, publish, and monetize their work within the BookBuddy ecosystem.
8.  **Break Language Barriers:** Enable users to read, translate, and publish books across any language, fostering a global literary community.

## Detailed Roadmap

### Phase 1: Foundational Improvements

*   **Update Dependencies:**
    *   [X] Upgrade all libraries to their latest stable versions.
    *   [X] Replace deprecated libraries with modern alternatives.
*   **Refactor to Kotlin:**
    *   [X] Convert all existing Java code to Kotlin to take advantage of its modern features and improved syntax.
*   **Introduce Jetpack Compose:**
    *   [X] Migrate the `MainActivity` to Jetpack Compose.
    *   [X] Gradually migrate other screens to Compose, starting with the simpler ones like `SettingsActivity`.

### Phase 2: UI/UX Enhancements

*   **Redesign the Library:**
    *   [X] Implement a more visually appealing and user-friendly design for the `LibraryActivity`.
    *   [X] Add options for sorting and filtering the user's book collection.
*   **Improve the Reader:**
    *   [X] Enhance the `ReaderActivity` with features like adjustable font sizes, themes, and bookmarks.
    *   [X] Add support for more file formats, such as EPUB.
*   **Architect for Performance and Scale:**
    *   [X] Re-architect file processing to save books as a directory of individual page files.
    *   [X] Update the `ReaderActivity` to load only the current page's text into memory on demand.

### Phase 3: New Features

*   **Online Community Platform:**
    *   [ ] Design and implement a backend service for user authentication and book storage.
    *   [ ] Create a UI for users to upload their books to the online database.
    *   [ ] Develop a browsable and searchable public library of user-created books.
    *   [ ] Implement a system for users to download books from the public library to their personal library.
    *   [ ] Implement a friend request system allowing users to connect with each other.
    *   [ ] Create a notification feed or system to inform users about new books published by their friends.
    *   [ ] Add support for publishing user-created translations of books, with clear attribution to the original author and the translator.
*   **Authoring & Publishing Tools:**
    *   [ ] Design and implement a high-performance, intuitive editor focused on a fast and fluid writing experience, regardless of document size.
    *   [ ] Implement a comprehensive set of rich text formatting tools (e.g., bold, italics, headings, lists).
    *   [ ] Create a dedicated "Translation Mode" within the editor for side-by-side translation work.
    *   [ ] Add a "Save to Library" feature to permanently store both new creations and edited translations.
*   **Book Marketplace & Monetization:**
    *   [ ] Implement a system for authors to set a price for their books or offer them for free.
    *   [ ] Integrate a secure payment gateway for book sales.
    *   [ ] Develop a "My Earnings" dashboard for authors to track sales.
*   **Language & Translation Hub:**
    *   [X] Design and implement a centralized Language Management screen.
    *   [ ] Create sub-sections within the hub for managing different language resources:
        *   **Text-to-Speech (TTS):**
            *   [X] Allow users to select their preferred system TTS Engine (Google, Samsung, etc.).
            *   [X] Display all available languages for the selected engine and their installation status.
            *   [X] Guide users to the system settings to download missing voice packs.
        *   **Text Recognition (OCR):**
            *   [ ] Display all available ML Kit OCR language packs (e.g., Devanagari, Latin, Chinese).
            *   [ ] Show their download status.
            *   [ ] Allow users to trigger downloads for new OCR models directly from within the app.
        *   **Translation:**
            *   [ ] Display all available offline translation models.
            *   [ ] Show their download status.
            *   [ ] Allow users to trigger downloads for new translation models.
*   **Advanced OCR:**
    *   [X] Explore more advanced OCR techniques to improve the accuracy of text extraction.
    *   [ ] Implement multi-language detection, including mixed languages on a single page.
*   **AI-Powered Summarization:**
    *   [ ] Implement a feature to generate summaries of books using an AI model.
*   **Image-to-PDF Creation:**
    *   [ ] Develop a feature to capture or select multiple images and compile them into a single PDF file.
*   **Audiobook & Text-to-Speech:**
    *   [ ] Implement on-the-fly language detection for on-screen text to automatically select the correct TTS voice.
    *   [ ] Redesign the reader's audio controls to offer two distinct options: "Hear Original Text" and "Hear Translated Text".
    *   [X] Integrate a TTS engine that can be configured by user preferences (e.g., Google vs. Samsung).

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
*   Converted `PdfSelectorActivity.java` to `PdfSelectorActivity.kt`.
*   Converted `LibraryItem.java` to `LibraryItem.kt` and fixed resulting compilation errors in `LibraryActivity`.
*   Converted `LibraryActivity.java` to `LibraryActivity.kt`.
*   Converted `LibraryAdapter.java` to `LibraryAdapter.kt`.
*   Converted `OcrProcessor.java`, `OcrResultActivity.java`, and `CameraActivity.java` to Kotlin.
*   Improved PDF OCR accuracy by rendering pages at 300 DPI.
*   Converted `MainActivity.java` and `SettingsActivity.java` to Kotlin.
*   Converted the final main application file, `ReaderActivity.java`, to Kotlin, completing the full conversion of the app's source code.
*   Fixed a crash in `ReaderActivity` caused by a missing view reference.
*   Migrated `MainActivity` to Jetpack Compose, marking the first step in our UI modernization effort.
*   Migrated `SettingsActivity` to Jetpack Compose.
*   Refactored `MainActivity` to a 2x2 grid for a more consistent UI.
*   Migrated `LibraryActivity` to Jetpack Compose, replacing the `RecyclerView` with a `LazyColumn`.
*   Refactored the `LibraryScreen` to improve testability and code quality.
*   Added sorting and filtering options to the Library screen.
*   Enhanced the reader screen with an adjustable font size feature.
*   Enhanced the reader screen with a theme selection feature.
*   Added support for the EPUB file format, including a new `FileSelectorActivity`.
*   Added Bangla OCR support using the Devanagari ML Kit model.
*   Implemented the initial version of the Language Management screen to display TTS language status.
*   Enhanced the Language Management screen to support TTS engine selection and display all available languages.
*   Fixed a crash when loading large books by passing content via a temporary file.
*   Added a TTS autoplay feature to the reader screen.
*   Re-architected file processing to use a paged-file system for improved performance and to fix crashes with large books.
*   Fixed a page sorting bug in the reader.
