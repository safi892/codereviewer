package com.secure.codereviewer.util

import android.content.Context
import android.util.Log
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.eclipse.tm4e.core.registry.IThemeSource

/**
 * TextMate setup for the Sora editor.
 *
 * Loading the C++ grammar means reading + parsing a ~550 KB JSON file. That used to
 * happen on the main thread inside AndroidView's factory (and twice: once via
 * languages.json and once manually), which froze the UI when the editor opened.
 *
 * Now:
 *  - [preload] does the one-time asset/grammar/theme work on Dispatchers.IO.
 *    Call it early (MainActivity) so the editor is warm before the user opens it.
 *  - [createLanguage] builds the per-editor Language off the main thread.
 *  - [applyTo] only does the cheap per-editor view configuration on main.
 */
object EditorSetup {

    private const val TAG = "EditorSetup"
    private const val CPP_SCOPE = "source.cpp"
    private const val THEME_NAME = "light-default"

    private val initMutex = Mutex()

    @Volatile
    private var isInitialized = false

    /** Idempotent, thread-safe, main-safe. */
    suspend fun preload(context: Context) {
        if (isInitialized) return
        initMutex.withLock {
            if (isInitialized) return
            withContext(Dispatchers.IO) {
                val appContext = context.applicationContext
                FileProviderRegistry.getInstance()
                    .addFileProvider(AssetsFileResolver(appContext.assets))

                // languages.json already registers source.cpp -> cpp.tmLanguage.json,
                // so the grammar is parsed exactly once.
                runCatching {
                    GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
                }.onFailure { Log.e(TAG, "Failed to load grammars", it) }

                runCatching {
                    appContext.assets.open("textmate/themes/$THEME_NAME.json").use { stream ->
                        val source = IThemeSource.fromInputStream(stream, "$THEME_NAME.json", null)
                        ThemeRegistry.getInstance().apply {
                            loadTheme(ThemeModel(source, THEME_NAME))
                            setTheme(THEME_NAME)
                        }
                    }
                }.onFailure { Log.e(TAG, "Failed to load theme", it) }
            }
            isInitialized = true
        }
    }

    /** Builds the C++ language (grammar lookup + analyzer) on a background thread. */
    suspend fun createLanguage(context: Context): Language? {
        preload(context)
        return withContext(Dispatchers.Default) {
            runCatching { TextMateLanguage.create(CPP_SCOPE, true) }
                .onFailure { Log.e(TAG, "Failed to create C++ language", it) }
                .getOrNull()
        }
    }

    /** Cheap view-only settings. Safe to call in AndroidView.factory. */
    fun applyBaseSettings(editor: CodeEditor) {
        editor.isWordwrap = true
        editor.setPinLineNumber(true)
        editor.setTextSize(14f)
    }

    /** Simple direct configuration helper. */
    fun configure(editor: CodeEditor, context: Context) {
        applyBaseSettings(editor)
    }

    /** Must run on the main thread (touches the View). */
    fun applyTo(editor: CodeEditor, language: Language?) {
        if (editor.isReleased) return
        editor.colorScheme = runCatching { TextMateColorScheme.create(ThemeRegistry.getInstance()) }
            .getOrElse { EditorColorScheme() }
        if (language != null) editor.setEditorLanguage(language)
    }
}
