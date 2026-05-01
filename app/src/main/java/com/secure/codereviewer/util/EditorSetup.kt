package com.secure.codereviewer.util

import android.content.Context
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.GrammarDefinition
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.eclipse.tm4e.core.registry.IGrammarSource
import org.eclipse.tm4e.core.registry.IThemeSource

object EditorSetup {

    private var isInitialized = false

    fun configure(editor: CodeEditor, context: Context) {
        try {
            val fileProviderRegistry = FileProviderRegistry.getInstance()
            val grammarRegistry = GrammarRegistry.getInstance()
            val themeRegistry = ThemeRegistry.getInstance()

            if (!isInitialized) {
                // Register assets resolver
                fileProviderRegistry.addFileProvider(AssetsFileResolver(context.assets))
                
                // Load languages registry file as well
                try {
                    grammarRegistry.loadGrammars("textmate/languages.json")
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 1. Manually Register C++ Grammar to be 100% sure it's loaded
                try {
                    val grammarInputStream = context.assets.open("textmate/cpp/cpp.tmLanguage.json")
                    val grammarSource = IGrammarSource.fromInputStream(
                        grammarInputStream,
                        "cpp.tmLanguage.json",
                        null
                    )
                    
                    val cppDef = object : GrammarDefinition {
                        override fun getName(): String = "source.cpp"
                        override fun getScopeName(): String = "source.cpp"
                        override fun getGrammar(): IGrammarSource = grammarSource
                        override fun getLanguageConfiguration(): String? = null
                    }
                    
                    grammarRegistry.loadGrammar(cppDef)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 2. Load the Light Mode theme
                try {
                    val themeStream = context.assets.open("textmate/themes/light-default.json")
                    val themeSource = IThemeSource.fromInputStream(
                        themeStream,
                        "light-default.json",
                        null
                    )
                    val themeModel = ThemeModel(themeSource, "light-default")
                    themeRegistry.loadTheme(themeModel)
                    themeRegistry.setTheme("light-default")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                isInitialized = true
            }

            // 3. Set the editor color scheme (must be set for each editor)
            try {
                editor.colorScheme = TextMateColorScheme.create(themeRegistry)
            } catch (e: Exception) {
                e.printStackTrace()
                editor.colorScheme = EditorColorScheme()
            }

            // 4. Apply C++ Language (must be set for each editor)
            try {
                val cppLanguage = TextMateLanguage.create("source.cpp", true)
                editor.setEditorLanguage(cppLanguage)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 4. Basic editor settings
            editor.isWordwrap = true
            editor.setPinLineNumber(true)
            editor.setTextSize(14f)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
