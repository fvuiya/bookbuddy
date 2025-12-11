
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

1.  **Our Guiding Principle: Empower the Journey of Knowledge.** We recognize that roles like Reader, Writer, Translator, Student, Teacher, and Researcher are not static professions but fluid states in the universal human journey of learning and creating. All architectural decisions must empower this journey, making knowledge accessible and its expression effortless through a robust, user-friendly system.
2.  **Modernize the UI:** Migrate the existing UI from XML layouts to Jetpack Compose for a more declarative and maintainable codebase.
3.  **Improve Performance:** Optimize the app's performance, particularly in the areas of PDF processing and OCR.
4.  **Enhance User Experience:** Add new features and refine existing ones to create a more intuitive and engaging user experience.
5.  **Increase Test Coverage:** Write comprehensive unit and integration tests to ensure the app's stability and reliability.
6.  **Integrate AI & Advanced Content Features:** Add capabilities like summarization, TTS, and multi-option translation.
7.  **Build an Online Community:** Create a platform for users to share and discover books.
8.  **Empower Creators:** Provide tools for authors to create, publish, and monetize their work within the BookBuddy ecosystem.
9.  **Break Language Barriers:** Enable users to read, translate, and publish books across any language, fostering a global literary community.

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
    *   [X] Implement a "draft" status for books in the library.
    *   [X] The editor will have a dirty-state check to manage when saving is needed.
    *   [X] Implement a robust auto-save mechanism that works in the background.
    *   [X] The "Save" button in the editor will manually trigger a save and clear the dirty state.
    *   [X] Design and implement a high-performance, intuitive editor focused on a fast and fluid writing experience, regardless of document size.
    *   [X] Implement a comprehensive set of rich text formatting tools (e.g., bold, italics, headings, lists).
    *   [X] Add "Create New Book" feature to write books from scratch with title, author, and language selection.
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

### Phase 3: Online Infrastructure & Backend

#### Note on Infrastructure Constraints
BookBuddy is built to work within the **Firebase Spark Plan (free tier)** and **GitHub Education resources** that don't require a credit card. We use **Firebase Realtime Database** instead of Firestore/Storage (which require Blaze Plan with credit card). We leverage free services, open-source alternatives, and efficient architecture to maximize functionality without paid upgrades.

*   **Firebase Setup & Authentication (Spark Plan Compatible):**
    *   [X] Set up Firebase project and integrate Firebase SDK into the app
    *   [X] Implement Firebase Authentication (Email/Password, Anonymous) - fully free on Spark Plan
    *   [X] Create user registration and login UI in Compose (LoginActivity, SignUpActivity)
    *   [X] Created FirebaseAuthManager for handling all auth operations
    *   [X] Made LoginActivity the launcher activity (app opens to login screen)
    *   [X] Integrated authentication flow: Login → SocialActivity (main hub) → All features
    *   [X] Added navigation drawer with logout functionality
    *   [X] Production-ready social media app architecture implemented
    *   [ ] Implement account recovery and password reset flows
    *   [ ] Set up Firebase Security Rules for user data protection
*   **Realtime Database & Data Storage (Spark Plan - NO CREDIT CARD):**
    *   [X] Designed Realtime Database schema for users, books, posts, comments, friends, notifications
    *   [X] Created RealtimeModels.kt with all data structures
    *   [X] Implemented FirebaseSocialService replacing MockSocialService
    *   [ ] Enable Realtime Database in Firebase Console
    *   [ ] Add Security Rules to Realtime Database
    *   [ ] Test real-time data synchronization
    *   [ ] Implement data compression for stored books (Base64 for small books)
    *   [ ] Design pagination and lazy-loading to optimize performance
    *   **Note:** Firebase Storage requires Blaze Plan (credit card) - using Base64 encoding in database or external hosting (GitHub Pages, Cloudinary) instead
*   **Social Features Backend (Realtime Database Optimized):**
    *   [X] Created database structure for user profiles, friend lists, and connections
    *   [X] Implemented friend request system with acceptance/rejection
    *   [X] Built post creation and retrieval system
    *   [X] Added like and comment functionality
    *   [X] Created notification triggers for friend requests
    *   [ ] Test feed system with real-time updates
    *   [ ] Implement activity feed archiving for old posts
*   **Book Publishing & Discovery (Realtime Database):**
    *   [X] Designed database schema for books with metadata
    *   [X] Implemented book publishing workflow (draft → published state)
    *   [X] Built public book library browsing
    *   [ ] Test book download tracking
    *   [ ] Implement book reviews and ratings system
    *   [ ] Add book search functionality
    *   [ ] Add book download tracking with batched analytics (reduce write operations)
    *   [ ] Implement book reviews and ratings system with aggregated scores (reduce reads)
*   **Cloud Functions & Backend Logic (Free Tier Alternative):**
    *   [ ] **Note:** Firebase Cloud Functions require a credit card to enable, so we use alternative approaches:
    *   [ ] Implement client-side triggers using Android WorkManager for background tasks
    *   [ ] Use Firestore triggers via client-side listeners instead of Cloud Functions
    *   [ ] Implement notification system using Firebase Cloud Messaging (FCM) with client-side scheduling
    *   [ ] Create book indexing logic on-device or during user actions instead of server-side
    *   [ ] Use Firestore Security Rules for enforcing data consistency instead of functions
*   **GitHub Education Resources (No Credit Card Required):**
    *   [ ] Leverage GitHub Copilot for accelerated development (free with Student Developer Pack)
    *   [ ] Use GitHub Actions for basic CI/CD pipeline (free for public and private repos)
    *   [ ] Implement GitHub Projects for project management and issue tracking (free)
    *   [ ] Set up GitHub repository with proper documentation and contributing guidelines
    *   [ ] Use GitHub Discussions for community support and feature requests (free)
    *   [ ] Use GitHub Pages for hosting documentation and project website (free)
    *   [ ] Leverage free GitHub-hosted runners for automated testing and builds
*   **Alternative Services (No Credit Card, Community-Friendly):**
    *   [ ] Consider Supabase (PostgreSQL backend) as Firebase alternative - offers free tier with no credit card required
    *   [ ] Evaluate Appwrite (self-hosted or managed) as open-source Firebase alternative
    *   [ ] Use Cloudinary for free image storage and optimization (free tier: 25GB/month)
    *   [ ] Implement email via SendGrid free tier (100 emails/day) for notifications
    *   [ ] Use Sentry for error tracking and monitoring (free tier available)
*   **Security & Compliance (Spark Plan Friendly):**
    *   [ ] Set up comprehensive Firebase Security Rules (main security layer on Spark Plan)
    *   [ ] Implement data encryption on-device before uploading to Firebase
    *   [ ] Add rate limiting in Security Rules to prevent abuse (reduce operational costs)
    *   [ ] Set up GDPR-compliant data handling and deletion (important for free tier)
    *   [ ] Create user data privacy controls and export features (Firestore backup exports)
    *   [ ] Implement strict field-level permissions in Security Rules
*   **Analytics & Monitoring (Spark Plan Compatible):**
    *   [ ] Integrate Firebase Analytics for user behavior tracking (fully free on Spark Plan)
    *   [ ] Set up Firebase Crashlytics for crash reporting (fully free on Spark Plan)
    *   [ ] Create custom analytics events for key user actions
    *   [ ] Use local logging and export analytics to Firestore for dashboard creation
    *   [ ] Implement lightweight error logging using Firestore (batched writes)
    *   [ ] Use Google Analytics to track website/documentation traffic (free)
*   **Cost Optimization Strategy:**
    *   [ ] Implement aggressive caching in Kotlin to reduce Firestore reads
    *   [ ] Use Firestore's offline persistence to reduce real-time sync costs
    *   [ ] Archive old/inactive user data to keep storage manageable
    *   [ ] Implement monthly quota monitoring and alerts
    *   [ ] Design workflows that batch database operations
    *   [ ] Monitor monthly usage to stay within Spark Plan limits (50k reads, 20k writes, 20k deletes/day)

### Phase 4: Testing and Release

*   **Write Unit Tests:**
    *   [ ] Write comprehensive unit tests for all major components of the app.
*   **Conduct User Testing:**
    *   [ ] Gather feedback from a group of beta testers to identify and address any usability issues.
*   **Prepare for Release:**
    *   [ ] Finalize the app's branding and marketing materials.
    *   [ ] Publish the app to the Google Play Store.

## Changelog

### Completed Tasks

#### Core Architecture & Infrastructure
*   [X] Migrate entire app to Kotlin and Jetpack Compose; re-architected file processing to paged-file system for performance with large books

#### OCR & Document Processing
*   [X] Implemented ML Kit Text Recognition with multi-language support (Bangla/Devanagari); created ReaderActivity for scanned books with improved PDF accuracy (300 DPI)

#### Reading Experience
*   [X] BookReaderActivity for published books with adjustable font sizes (12-32pt), theme selection, EPUB support, and seamless edit flow

#### Text-to-Speech & Language Features
*   [X] TTS engine selection (Google, Samsung, etc.) with voice installation status; translation services integration; language detection

#### Professional Authoring System
*   [X] EditorActivity with Quill.js WebView; rich text formatting; paged file system with flexible numbering (1, 1.1, 1.2, 2, 2.1); multi-page navigation (Previous/Next/Jump); page management ("Before", "After", "At End"); auto-save (10-sec); export options (Plain Text, Markdown, Clean HTML); manual Publish feature; dirty-state checking

#### Image Processing
*   [X] CameraActivity for image capture; Image-to-PDF conversion

#### Social Networking & Community
*   [X] SocialActivity with tabbed interface (Feed, Friends, Books, Notifications); user profiles, friend connections, feed system, post interactions (Like, Comment, Share), notifications

#### UI/UX Enhancements
*   [X] Redesigned Library with grid layout, book covers, sorting/filtering, "Draft" badge; migrated MainActivity, SettingsActivity, LibraryActivity to Compose; fixed library state management with onResume() refresh

#### Authentication & App Flow
*   [X] Implemented complete authentication system with LoginActivity and SignUpActivity (Email/Password, Anonymous login)
*   [X] Made LoginActivity the launcher activity; app opens to login screen (like Instagram/Twitter/TikTok)
*   [X] SocialActivity is now the main hub after login with navigation drawer (Camera, Library, Settings, Logout)
*   [X] Production-ready social media app architecture: Login → Feed → All Features

### Dependencies & Architecture
*   Upgraded all project dependencies to latest stable versions (AppCompat 1.7.1, Material 1.13.0, ConstraintLayout 2.2.1, etc.)
*   Converted entire codebase from Java to Kotlin
*   Migrated UI from XML layouts to Jetpack Compose (MainActivity, SettingsActivity, LibraryActivity)
*   Re-architected file processing to paged-file system for performance and crash prevention with large books

### Features & Enhancements
*   **Library**: Added sorting/filtering options, grid layout with book covers, "Draft" badge for in-progress books
*   **Reader**: Font size adjustment, theme selection (light/dark), EPUB support, TTS with voice selection
*   **OCR**: Improved PDF accuracy (300 DPI rendering), added Bangla support via Devanagari ML Kit
*   **Language Management**: TTS engine selection screen showing all available voices and installation status
*   **Create New Book**: FloatingActionButton in library to write from scratch with metadata input

### Professional Authoring System
*   **Editor**: High-performance Quill.js WebView with rich text formatting toolbar (bold, italic, headings, lists, colors, alignment, etc.)
*   **Multi-page System**: Flexible page numbering (1, 1.1, 1.2, 2, 2.1) allowing page insertion anywhere; navigation with Previous/Next/Jump controls
*   **Page Management**: 3 buttons - "Before" (insert before current), "After" (insert after current), "At End" (append new page)
*   **Auto-save**: Silent 10-second auto-save to `.draft.txt` files; manual Publish commits to library
*   **Export System**: 3 formats - Plain Text (.txt), Markdown (.md), Clean HTML (.html) - removes Quill markup for publishers/printing
*   **Clean UI**: 3-dot menu for Publish/Export; editor focused on writing with minimal distractions
*   **Library Refresh**: Fixed library not loading on app start - now properly uses Compose state management with refresh trigger on onResume()
*   **Professional Reader**: New BookReaderActivity for published books - read-only Quill.js display with rich formatting, adjustable font size (12-32pt), minimal UI with 3-dot menu, page navigation identical to editor. Separate from OCR-based ReaderActivity which handles scanned books.
*   **Seamless Edit Flow**: BookReaderActivity includes "Edit" button in 3-dot menu that opens the book in EditorActivity at the exact same page you were reading - smooth transition from reading to editing.

### Build Environment
*   Gradle Version: 8.13
