# Performance Review: Main-Thread Work and Coroutines

Goal: steady 60 fps. Android has about 16 ms per frame, and any disk read, JSON parse, or full-text copy on the main thread uses up that budget and causes visible jank.

Rule used throughout:

| Work type | Dispatcher |
|---|---|
| Disk, SharedPreferences first read, ContentResolver, network | `Dispatchers.IO` |
| CPU work (parsing, regex, mapping lists) | `Dispatchers.Default` |
| Touching Views / Compose state | `Dispatchers.Main` (the default in `viewModelScope` and `LaunchedEffect`) |

---

## 1. The C++ grammar was parsed twice on the main thread when the editor opened

File: `util/EditorSetup.kt`, `ui/screens/editor/EditorScreen.kt`

Issue
- `EditorSetup.configure()` ran inside `AndroidView(factory = …)`, which runs on the main thread.
- It read and parsed `cpp.tmLanguage.json` (about 550 KB). This happened twice: once through `languages.json` and once through manual registration. It also parsed the theme.
- Result: a long freeze on the first frame of the Editor screen.

Fix
- `EditorSetup.preload(context)` is a `suspend` function that does all the asset, grammar, and theme loading on `Dispatchers.IO`. A `Mutex` and `@Volatile` flag make it run only once and thread-safe.
- `MainActivity` calls it at startup (`lifecycleScope.launch { EditorSetup.preload(...) }`), so the grammar is already loaded before the user opens the editor.
- The grammar is parsed manually only if `languages.json` didn't register `source.cpp`.
- `TextMateLanguage.create()` runs on `Dispatchers.Default`. The result is applied to the view on Main in a `LaunchedEffect(editorRef)`.
- The `factory` now only does cheap view settings (`applyBaseSettings`).

## 2. Every keystroke copied the whole document and recomposed the whole screen

File: `EditorScreen.kt`, `EditorViewModel.kt`

Issue
On each key press:
1. `ContentChangeEvent` called `editor.text.toString()`, which copies the full document.
2. The result was written into `currentContent`, a Compose state that the whole `EditorScreen` reads, so the whole screen recomposed.
3. `LaunchedEffect(currentContent)` then called `editor.text.toString()` again to compare.

That's two full copies plus a full recomposition per character, which gets worse as the file grows.

Fix
- The event listener only emits a signal: `MutableSharedFlow<Unit>(extraBufferCapacity = 1, DROP_OLDEST)`.
- The flow is `debounce(250ms)`, so the text is copied once after the user pauses.
- VM → editor sync now uses `externalContentVersion`, a counter that is bumped only when the ViewModel replaces the text (open file, auto-fix, new file). Typing no longer bounces text back into the view.
- Before Analyze, Optimize, or Save, `syncFromEditor(editor.text)` flushes the latest text so nothing is lost to the debounce.
- `AndroidView(onRelease = { it.release() })` stops Sora's analyzer thread when the screen leaves. Before this, the editor view and its thread leaked.

## 3. Syntax validation used a manual Job/delay

File: `EditorViewModel.kt`

Issue
`validationJob?.cancel(); launch { delay(500) … }` works, but it is error-prone. A cancelled request could still write its state, and `CancellationException` was swallowed by `catch (Exception)`.

Fix: a reactive pipeline.
```kotlin
validationInput            // MutableStateFlow<String>
    .debounce(500)         // wait for the user to pause
    .distinctUntilChanged()// skip identical text
    .collectLatest { validate(it) } // auto-cancels the in-flight request on new text
```
`CancellationException` is rethrown everywhere so cancellation works correctly.

## 4. Analyze and Optimize ran one after another

File: `EditorViewModel.analyzeCurrentCode()`

Issue
`/optimize` only started after `/analyze` finished. Both are slow model calls and they don't depend on each other.

Fix: structured concurrency.
```kotlin
coroutineScope {
    val opt = async { repository.optimizeCode(code) }   // starts immediately
    val analysis = repository.analyzeCode(code)
    analysis to opt.await()
}
```
Wait time drops from analyze + optimize to the slower of the two. If analysis fails, `coroutineScope` cancels the optimize request automatically. The code is also snapshotted at the start, so later typing can't change what gets sent.

## 5. File open/save ran on the main thread

Files: `EditorViewModel.loadFile/saveToUri`, `CodeInputScreen.kt`

Issue
`contentResolver.openInputStream(...).readText()` and `openOutputStream(...).write()` ran on Main. Document providers can be cloud-backed (Drive), so this can take seconds and cause an ANR.

Fix
- All ContentResolver I/O runs in `withContext(Dispatchers.IO)`. Results come back on Main through state or a callback.
- The ViewModel now takes a `ContentResolver` instead of an `Activity`, so it no longer holds an Activity reference (which leaks on rotation).
- Save uses mode `"wt"` (truncate). Previously, saving shorter text over a longer file left leftover bytes at the end.

## 6. History cache was a large blob in the auth SharedPreferences file

File: `data/local/HistoryStorage.kt`

Issue
- 20 history items (code + commented code + explanation, often hundreds of KB) were stored as one JSON string in `auth_prefs`, the same file as the token.
- Any `AuthStorage` read on Main (splash, settings, every OkHttp request) had to wait for that whole file to load.
- Every `apply()` rewrote the whole file. Android waits for pending `apply()` writes on the main thread during `onPause`/`onStop`, which causes jank when switching screens or apps.
- The JSON was parsed on Main (`getLocalHistory()` was called inside `viewModelScope.launch`, which runs on Main).

Fix
- Moved the cache to `cacheDir/history_cache.json`, written atomically (temp file + rename).
- All functions are `suspend` and run on `Dispatchers.IO`. Gson replaces hand-written JSONObject code.
- Old keys are removed from `auth_prefs` once. The cache is cleared on logout (this used to happen implicitly because it shared the auth file).

## 7. History screen: work during composition

Files: `HistoryScreen.kt`, `HistoryViewModel.kt`

Issues
- `filteredItems` (a `contains` over every item's full code) re-ran on every recomposition.
- `viewModel.loadMore()` was called from inside the `itemsIndexed` item content, so a side effect ran during composition, repeatedly.
- No item `key`s, so the list couldn't reuse rows properly when it changed.
- Mapping items (Regex + `OffsetDateTime.parse`) ran on Main, and the Regex was recompiled for every item.

Fix
- `derivedStateOf` for filtering, so it only recomputes when the query or items change.
- Pagination uses `snapshotFlow { listState.layoutInfo }.distinctUntilChanged().filter { nearEnd }` inside `LaunchedEffect`.
- `key = { item.id }` and `contentType`. Pages are `distinctBy { id }` so duplicate keys can't crash the list.
- Mapping runs on `Dispatchers.Default`. The Regex is a single precompiled constant.
- Refresh cancels the previous fetch job, so stale responses can't overwrite new ones.

## 8. Smaller items

- Splash: the first `AuthStorage.isLoggedIn()` read (which waits for disk) now runs on `Dispatchers.IO`.
- `HttpLoggingInterceptor.Level.BODY` was on in all builds. It formatted every large AI response and logged login passwords and tokens to logcat. It is now `BODY` only in debug and `NONE` in release. This is also a security fix.

---

## 9. Editor screen layout broke on narrow phones (UI)

File: `EditorScreen.kt`

| Issue | Cause | Fix |
|---|---|---|
| "Unsaved" chip wrapped one letter per line | Filename + chip sat in a `Row` in the title slot, squeezed by 4 action icons | Status moved under the filename as a one-line subtitle (dot + text, `softWrap = false`); filename ellipsizes |
| Optimizer `Switch` pushed off the right edge | Label `Row` had no weight, so long text took all the width | Label gets `Modifier.weight(1f)` and wraps; Switch always keeps its size; clearer unchecked colors |
| "Optimize" wrapped to two lines | Default button padding (24dp per side) + icon + `weight(1f)` left almost no room | `contentPadding = 12.dp`, equal weights for both buttons, `maxLines = 1` + ellipsis |
| Top-bar icons had no explanation | Icons only | New `LabeledTopBarAction`: icon + small caption (New / Open / Save / Save As), 48dp touch target |

---

## Not changed (recommendations)

- Measure fps on a release build. Compose debug builds are much slower, so judging 60 fps on debug is misleading. Use `isMinifyEnabled = true` with keep rules for Retrofit and Gson models, and add a Baseline Profile (`androidx.profileinstaller` + Macrobenchmark).
- Verify with Android Studio Profiler → System Trace, or `adb shell dumpsys gfxinfo com.secure.codereviewer`, before and after.
- Add `StrictMode` in debug to catch any future disk or network calls on Main:
  ```kotlin
  if (BuildConfig.DEBUG) StrictMode.setThreadPolicy(
      StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
  ```
- Results sheets render very long `Text` inside `verticalScroll`. For very large outputs, a `LazyColumn` of lines would be smoother.
- The 1200 ms splash delay is artificial and can be removed.
- Repositories create `RetrofitClient` directly. Injecting them (constructor or Hilt) would allow unit-testing the ViewModels with `kotlinx-coroutines-test`.
