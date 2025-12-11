# WebView Lifecycle: Visual Explanation

## The Problem (BEFORE)

```
┌─────────────────────────────────────────────────────────────┐
│ RichTextEditor Composable Recomposition                     │
└─────────────────────────────────────────────────────────────┘

┌─ IMMEDIATELY ──────────────────────────────────────────────┐
│ AndroidView.factory { }                                    │
│   └─ Create new WebView                                    │
│   └─ loadUrl("file:///android_asset/editor.html")          │
│                                                             │
│ AndroidView.update { }  ⚠️ RACE CONDITION!                 │
│   └─ evaluateJavascript("setContent(...)")  ❌ TOO EARLY! │
│   └─ Error: setContent is not defined!                     │
│   └─ Content NOT set                                       │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─ LATER (50-500ms later!) ──────────────────────────────────┐
│ WebViewClient.onPageFinished()                             │
│   └─ HTML page loaded                                      │
│   └─ Quill.js library loaded                               │
│   └─ setContent() function NOW exists                      │
│   └─ Content FINALLY set correctly                         │
│   └─ User sees blank editor → then text appears!           │
└─────────────────────────────────────────────────────────────┘


Result: ❌ Cursor resets, content loss, blank screens
```

---

## The Solution (AFTER)

```
┌─────────────────────────────────────────────────────────────┐
│ RichTextEditor Composable Recomposition                     │
└─────────────────────────────────────────────────────────────┘

┌─ IMMEDIATELY ──────────────────────────────────────────────┐
│ AndroidView.factory { }                                    │
│   ├─ Create WebView                                        │
│   ├─ webViewRef.value = this  (store reference)           │
│   ├─ addJavascriptInterface("Android")                     │
│   └─ loadUrl("file:///android_asset/editor.html")          │
│                                                             │
│ ❌ NO update block (removed!)                              │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─ LATER (50-500ms later!) ──────────────────────────────────┐
│ WebViewClient.onPageFinished()                             │
│   ├─ isPageLoaded = true  ✅ STATE UPDATED                 │
│   ├─ evaluateJavascript("setContent(...)")                 │
│   └─ SUCCESS: Quill.js ready, content set                  │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─ WHEN CONTENT CHANGES ─────────────────────────────────────┐
│ LaunchedEffect(content) {                                  │
│   if (isPageLoaded && lastLoadedContent != content) {      │
│       webViewRef.value?.evaluateJavascript(...)  ✅ SAFE! │
│   }                                                         │
│ }                                                           │
└─────────────────────────────────────────────────────────────┘


Result: ✅ Smooth typing, proper state, no artifacts
```

---

## State Machine Comparison

### Before: Broken State Flow
```
┌──────────────┐
│  LOADING     │ (AndroidView created, WebView loading)
└──────┬───────┘
       │
       ├─→ update { } fires immediately
       │        └─→ evaluateJavascript() ❌ FAILS
       │
       └─→ onPageFinished() fires
               └─→ Content set from onPageFinished()
               
Problem: Two competing operations, both trying to set content
         First one fails silently, second one works
         Cursor already reset, user loses position
```

### After: Proper State Flow
```
┌──────────────┐
│  LOADING     │ (AndroidView created, WebView loading)
│ isPageLoaded │ = false
└──────┬───────┘
       │
       ├─→ LaunchedEffect runs but:
       │        └─→ isPageLoaded == false, so:
       │        └─→ guard condition prevents update ✅
       │
       └─→ onPageFinished() fires
               ├─→ isPageLoaded = true ✅
               ├─→ evaluateJavascript() NOW succeeds ✅
               └─→ LaunchedEffect triggers again if content changed
                    └─→ NOW safe to update (isPageLoaded == true)

Result: Coordinated operations, no conflicts, reliable execution
```

---

## WebView Reference Management

### Before: Confused Reference
```
┌─ remember { } ────────────────────┐
│  val webView = WebView(context)   │  Created but...
│  webViewRef.value = null          │  Reference not used!
└───────────────────────────────────┘
                │
                │ ignored by factory
                ↓
┌─ AndroidView.factory { } ─────────┐
│  Creates NEW webView instance      │  Wait, which one?
│  Returns it to Compose             │  This is separate!
└───────────────────────────────────┘

Result: Confusion about which WebView is being used
        Potential memory leaks and state inconsistency
```

### After: Clear Reference
```
┌─ remember { } ────────────────────┐
│  val webViewRef = mutableStateOf() │  Storage for reference
│  WebViewRef.value = null           │  Initialized as empty
└───────────────────────────────────┘
                │
                │ used by factory
                ↓
┌─ AndroidView.factory { } ─────────┐
│  Creates WebView                   │
│  webViewRef.value = this           │  Store reference immediately
│  Return WebView to Compose         │  Compose displays it
└───────────────────────────────────┘
                │
                │ can use reference in LaunchedEffect
                ↓
┌─ LaunchedEffect { } ──────────────┐
│  webViewRef.value?.evaluateJS()    │  Access stored WebView
└───────────────────────────────────┘

Result: Clear ownership, single instance, reliable access
```

---

## Key Takeaways

| Aspect | Before | After |
|--------|--------|-------|
| **Timing** | Race condition (immediate vs. later) | Synchronized (waits for ready) |
| **State** | Lost on update | Preserved across operations |
| **Errors** | Silent (JavaScript errors ignored) | Prevented (guard conditions) |
| **Complexity** | Multiple competing updates | Single clear operation path |
| **Reliability** | Unpredictable | Deterministic |

---

## Why This Architecture Works

```
1️⃣ SINGLE SOURCE OF TRUTH
   └─ WebView created once in factory
   └─ Stored in webViewRef for entire lifetime

2️⃣ CLEAR STATE TRANSITIONS
   └─ isPageLoaded tracks readiness
   └─ LaunchedEffect waits for ready state

3️⃣ CONTENT CHANGE DETECTION
   └─ lastLoadedContent prevents loops
   └─ content parameter triggers updates only when different

4️⃣ SAFE JAVASCRIPT EXECUTION
   └─ Guard conditions (isPageLoaded && contentChanged)
   └─ Only executes when Quill.js is ready

5️⃣ NO CONFLICTING OPERATIONS
   └─ update block removed
   └─ Single LaunchedEffect handles all updates
```

This is the correct way to manage a WebView in Jetpack Compose!

