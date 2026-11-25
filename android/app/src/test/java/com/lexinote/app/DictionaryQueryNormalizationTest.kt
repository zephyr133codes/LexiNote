package com.lexinote.app

import com.lexinote.app.dictionary.normalizeDictionaryQuery
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DictionaryQueryNormalizationTest {
    @Test fun trimsAndLowercasesQueriesWithLocaleIndependentEnglishRules() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("introduction", normalizeDictionaryQuery("  Introduction  "))
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
