# WebView Editor Implementation Guide for Developers

## Overview

This guide explains the inner workings of BookBuddy's WebView-based rich text editor and how to extend or maintain it.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    EditorActivity (Kotlin)                  │
│  • Receives book_path and book_title intents                │
│  • Creates EditorViewModel                                  │
│  • Sets Compose content                                     │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│               EditorScreen Composable                        │
│  • Manages page navigation UI                               │
│  • Handles toolbar (Save, Back buttons)                     │
│  • Coordinates RichTextEditor component                     │
│  • Shows auto-save feedback (Toast)                         │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│           RichTextEditor Composable (Jetpack Compose)        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ State Management:                                    │   │
│  │ • isPageLoaded - tracks HTML/JS readiness           │   │
│  │ • lastLoadedContent - prevents infinite loops       │   │
│  │ • webViewRef - stores WebView instance reference    │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Effects:                                             │   │
│  │ • LaunchedEffect(content) - updates when safe       │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ AndroidView Factory:                                 │   │
│  │ • Creates WebView instance                           │   │
│  │ • Configures JavaScript/DOM storage                 │   │
│  │ • Sets up Android <-> JS bridge                     │   │
│  │ • Configures WebViewClient for load tracking        │   │
│  │ • Loads editor.html from assets                     │   │
│  └──────────────────────────────────────────────────────┘   │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│         WebView (Android Framework Component)                │
│  • Renders HTML                                             │
│  • Executes JavaScript                                      │
│  • Manages DOM                                              │
│  • Handles events                                           │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  editor.html (Web Assets) + Quill.js Library                │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ HTML Structure:                                      │   │
│  │ • <div id="editor"></div> - Quill container         │   │
│  │ • Quill.js library (CDN)                            │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ JavaScript Functions:                                │   │
│  │ • setContent(html) - Called from Android            │   │
│  │ • getContent() - Could be called from Android       │   │
│  │ • text-change event listener - Calls Android bridge │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Quill Instance:                                      │   │
│  │ • Handles rich text operations                      │   │
│  │ • Manages DOM                                        │   │
│  │ • Emits events                                       │   │
│  └──────────────────────────────────────────────────────┘   │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            │ Android.onContentChanged()
                            │ (JavaScript Bridge)
                            ▼
┌─────────────────────────────────────────────────────────────┐
│             EditorViewModel (MVVM Pattern)                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ State Properties:                                    │   │
│  │ • currentPageContent - Current page HTML            │   │
│  │ • currentPageIndex - Which page we're editing       │   │
│  │ • isDirty - Has unsaved changes                     │   │
│  │ • pageFiles - List of page files on disk            │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Methods:                                             │   │
│  │ • onContentChanged() - Updates state from editor    │   │
│  │ • saveDraft() - Auto-save to .draft.txt             │   │
│  │ • commitCurrentPage() - Save to main page file      │   │
│  │ • jumpToPage() - Navigate to different page         │   │
│  │ • startAutoSave() - 10-second auto-save loop        │   │
│  └──────────────────────────────────────────────────────┘   │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│         File System (Device Storage)                         │
│  /data/user/0/com.vuiya.bookbuddy/app_data/                │
│  └── books/                                                 │
│      └── bookTitle/                                         │
│          ├── page_0.txt            (Committed content)      │
│          ├── page_0.draft.txt      (Auto-save draft)        │
│          ├── page_1.txt                                     │
│          ├── page_1.draft.txt                               │
│          └── ...                                            │
└─────────────────────────────────────────────────────────────┘
```

---

## State Management Deep Dive

### RichTextEditor State Variables

```kotlin
var isPageLoaded by remember { mutableStateOf(false) }
```
**Purpose**: Tracks when the HTML/JavaScript has finished loading  
**Set By**: `webViewClient.onPageFinished()`  
**Used By**: Guard condition in `LaunchedEffect`  
**Why**: Prevents content update before Quill.js is ready

```kotlin
var lastLoadedContent by remember { mutableStateOf(content) }
```
**Purpose**: Prevents infinite update loops  
**Set By**: When we update content, or when JS bridge calls onContentChanged  
**Used By**: Comparison check in `LaunchedEffect`  
**Why**: Only update if content actually differs from what's already in editor

```kotlin
val webViewRef = remember { mutableStateOf<WebView?>(null) }
```
**Purpose**: Stores reference to the single WebView instance  
**Set By**: In `AndroidView.factory` when WebView is created  
**Used By**: `LaunchedEffect` to call JavaScript methods  
**Why**: Enables safe access to WebView across recompositions

### EditorViewModel State Variables

```kotlin
val currentPageContent = mutableStateOf("")
```
**Purpose**: Holds the HTML content of the current page  
**Set By**: `loadPage()` from disk, or `onContentChanged()` from editor  
**Used By**: `RichTextEditor` to display content  
**Flow**: Disk → ViewModel → RichTextEditor → WebView

```kotlin
val isDirty = mutableStateOf(false)
```
**Purpose**: Tracks if page has unsaved changes  
**Set By**: `onContentChanged()`, `saveDraft()`, `commitCurrentPage()`  
**Used By**: Auto-save trigger, Save button enable/disable  
**Why**: Minimize disk writes, only save when necessary

```kotlin
val currentPageIndex = mutableStateOf(0)
```
**Purpose**: Tracks which page user is viewing  
**Set By**: `loadPage()`, `jumpToPage()`  
**Used By**: Navigation controls, file selection  
**Why**: Support multi-page editing

---

## Data Flow Diagrams

### User Starts Typing (Happy Path)

```
User types in editor
       │
       ▼
Quill.js detects change (text-change event)
       │
       ▼
JavaScript calls: Android.onContentChanged("new html")
       │
       ▼
EditorViewModel.onContentChanged() is called
       │
       ├─ currentPageContent.value = newContent
       │
       └─ isDirty.value = true
            │
            ▼
        startAutoSave() scheduled (10s delay)
            │
            ▼
        [After 10s]
            │
            ├─ isDirty still true?
            │  YES ─────┐
            │           │
            └─ NO: Skip save
                       │
                       ▼
                  saveDraft() writes to .draft.txt
                       │
                       ▼
                  Auto-save complete Toast shown
```

### User Navigates to Different Page (Before Saving)

```
User clicks "Next Page"
       │
       ▼
EditorScreen.saveAndNavigate(next=true)
       │
       ├─ Cancel autoSaveJob
       │
       └─ isDirty == true?
           │
           └─ YES
              │
              ▼
           saveDraft() to .draft.txt
              │
              ▼
              └─ Then: loadPage(currentIndex + 1)
                      │
                      ├─ Load from page_1.txt (if exists)
                      │  OR
                      └─ Load from page_1.draft.txt (if in progress)
                          │
                          ▼
                      Update currentPageContent
                          │
                          ▼
                      RichTextEditor re-renders with new content
                          │
                          ▼
                      WebView receives update via LaunchedEffect
```

### User Clicks Save Button (Commit Changes)

```
User clicks "Save" button
       │
       ▼
EditorScreen: Button(onClick={ viewModel.commitCurrentPage() })
       │
       ▼
EditorViewModel.commitCurrentPage()
       │
       ├─ Cancel autoSaveJob
       │
       ├─ pageFile.writeText(currentPageContent.value)
       │  [Overwrites page_X.txt with current content]
       │
       ├─ draftFile.delete()
       │  [Removes page_X.draft.txt]
       │
       ├─ isDirty.value = false
       │
       ├─ saveComplete.emit(Unit)
       │  [Triggers Toast "Auto-saved draft!"]
       │
       └─ startAutoSave()
          [Restart auto-save monitoring]
```

---

## JavaScript Bridge Communication

### Android → JavaScript (Android Calls JavaScript)

```kotlin
// In RichTextEditor.kt
webViewRef.value?.evaluateJavascript("javascript:setContent($escapedContent);", null)
```

The HTML in `editor.html` must define:
```javascript
function setContent(htmlContent) {
    quill.root.innerHTML = htmlContent;
}
```

**Important**: 
- `evaluateJavascript` is asynchronous
- No return value handling (third parameter is null)
- Should only be called when `isPageLoaded == true`
- Content must be properly escaped with `JSONObject.quote()`

### JavaScript → Android (JavaScript Calls Android)

```javascript
// In editor.html
quill.on('text-change', function(delta, oldDelta, source) {
    if (source === 'user') {
        if (typeof Android !== 'undefined') {
            Android.onContentChanged(quill.root.innerHTML);
        }
    }
});
```

The Android interface is injected by:
```kotlin
addJavascriptInterface(object {
    @JavascriptInterface
    fun onContentChanged(newContent: String) {
        lastLoadedContent = newContent
        onContentChanged(newContent)
    }
}, "Android")
```

**Important**:
- `@JavascriptInterface` annotation is required for security
- The object name "Android" is the namespace in JavaScript
- Only public methods can be called from JavaScript
- This runs on the main thread

---

## Performance Considerations

### Memory Usage

```
Single WebView Instance:
  ├─ Base WebView overhead: ~10-15 MB
  ├─ Page content (HTML): < 5 MB typically
  ├─ JavaScript (Quill.js + our code): ~2 MB
  └─ Total: ~15-25 MB per editor

Why NOT multiple WebViews:
  ✗ 2 pages × 20 MB = 40 MB
  ✗ 5 pages × 20 MB = 100 MB  <- Would freeze app
```

### CPU Usage

```
Idle State:
  ├─ No JavaScript running
  ├─ WebView doing nothing
  ├─ Auto-save timer waiting
  └─ CPU usage: < 1%

Typing:
  ├─ Text input to display: < 50ms
  ├─ Quill.js processing: < 20ms
  ├─ JavaScript bridge call: < 10ms
  └─ Total input latency: ~80ms (smooth to user)

Auto-save (Every 10s):
  ├─ Check isDirty: < 1ms
  ├─ If dirty, write to disk: ~50-100ms
  ├─ Emit saveComplete signal: < 1ms
  └─ Schedule next check: < 1ms
```

### Optimization Tips

1. **Lazy Loading**
   ```kotlin
   // Only load a page when user navigates to it
   if (index in pageFiles.indices) {
       // Don't load all pages upfront
   }
   ```

2. **Draft Cleanup**
   ```kotlin
   // Delete draft files after commit
   draftFile.delete()
   ```

3. **Content Throttling**
   ```kotlin
   // Quill only reports user changes, not programmatic ones
   if (source === 'user') {  // Skip internal changes
       Android.onContentChanged(...)
   }
   ```

4. **Auto-save Interval**
   ```kotlin
   delay(10000)  // 10 seconds - balance between data loss and disk I/O
   ```

---

## Error Handling

### Current Error Handling

```kotlin
webViewRef.value?.evaluateJavascript(..., null)
// Errors are swallowed (null callback)
// This is acceptable because:
// 1. No return value needed
// 2. If JS fails, user sees blank editor (obvious)
// 3. Retry on next update when content changes
```

### Potential Improvements

```kotlin
// More robust error handling:
webViewRef.value?.evaluateJavascript(javascript) { result ->
    if (result == null) {
        Log.e("Editor", "JavaScript execution failed: $javascript")
        // Could retry, or show error toast
    }
}
```

### Common Errors & Fixes

| Error | Cause | Fix |
|-------|-------|-----|
| `setContent is not defined` | Called before page loaded | Check `isPageLoaded` guard |
| `quill is not defined` | Quill.js didn't load | Check CDN URL in HTML |
| `SyntaxError in JSON` | Improper escaping of content | Use `JSONObject.quote()` |
| `blank editor on load` | Content overwritten by default | Load content in `onPageFinished` |
| `content won't update` | `lastLoadedContent` matches new content | Check comparison logic |

---

## Testing Strategies

### Unit Tests (EditorViewModel)

```kotlin
@Test
fun testOnContentChanged_UpdatesState() {
    viewModel.onContentChanged("new content")
    assertEquals(viewModel.currentPageContent.value, "new content")
    assertTrue(viewModel.isDirty.value)
}

@Test
fun testAutoSaveCreates_DraftFile() {
    // Wait 10+ seconds
    // Assert .draft.txt file exists
}

@Test
fun testCommitCurrentPage_MovesContent() {
    // Write to page_0.txt
    // Delete page_0.draft.txt
    // Assert main file has content
}
```

### UI Tests (EditorScreen with Compose)

```kotlin
@get:Rule
val composeTestRule = createAndroidComposeRule<EditorActivity>()

@Test
fun testEditorDisplaysContent() {
    composeTestRule.setContent {
        RichTextEditor("Test content") { }
    }
    // Assert content visible in WebView
}

@Test
fun testSaveButtonDisabledWhenClean() {
    // Assert Save button is disabled when isDirty == false
}
```

### Integration Tests

```kotlin
@Test
fun testFullEditCycle() {
    // 1. Load page
    // 2. Modify content
    // 3. Navigate away
    // 4. Return to page
    // 5. Assert content preserved
}
```

---

## Debugging Tips

### Logcat Patterns

```bash
# Show only editor-related logs
adb logcat | grep -E "(Editor|WebView|JavaScript)"

# Show JavaScript console output
adb logcat *:V | grep "JavaScript"
```

### Inspect WebView State

```kotlin
// Add to EditorViewModel or RichTextEditor
Log.d("Editor", "isPageLoaded: $isPageLoaded")
Log.d("Editor", "currentPageContent.length: ${currentPageContent.value.length}")
Log.d("Editor", "isDirty: ${isDirty.value}")
Log.d("Editor", "lastLoadedContent.length: $lastLoadedContent.length")
```

### JavaScript Console

Add to `editor.html` for debugging:
```javascript
// Redirect console logs to Android
window.onerror = function(msg, url, lineNo, columnNo, error) {
    if (typeof Android !== 'undefined') {
        Android.onError(msg);
    }
    return false;
};
```

### Monitor File System

```bash
# Watch for draft files being created
adb shell "watch -n 1 ls -la /data/user/0/com.vuiya.bookbuddy/app_data/books/"
```

---

## Future Extensibility

### Adding Rich Text Features

```kotlin
// In EditorControls:
Button(onClick = {
    webViewRef.value?.evaluateJavascript(
        "javascript:document.execCommand('bold');",
        null
    )
})
```

### Adding Content Search

```kotlin
// Create new composable
@Composable
fun SearchBar(onSearch: (String) -> Unit) {
    // Search within currentPageContent
}
```

### Adding Offline Sync

```kotlin
// Extend EditorViewModel
fun syncToCloud() {
    // Upload committed pages to server
    // Merge changes from cloud
    // Handle conflicts
}
```

### Adding Translation Features

```kotlin
@Composable
fun TranslationEditor(
    originalContent: String,
    translatedContent: String,
    onTranslate: (String) -> Unit
) {
    Row {
        RichTextEditor(originalContent) { }
        RichTextEditor(translatedContent) { onTranslate(it) }
    }
}
```

---

## Summary

The BookBuddy editor is built on these principles:

1. **Single Instance** - One WebView for entire book
2. **State Synchronization** - Careful tracking of readiness
3. **Async-First** - All operations assume async execution
4. **Safety Guards** - Check conditions before executing
5. **File-Based Persistence** - Draft and committed files
6. **User-Centric** - Auto-save + manual commit options

Understanding this architecture makes it easy to:
- Fix bugs
- Add features
- Optimize performance
- Handle edge cases
- Scale to larger documents

---

**For Questions**: Refer to the inline code comments in `EditorScreen.kt` and `EditorViewModel.kt`

**Last Updated**: December 10, 2025

