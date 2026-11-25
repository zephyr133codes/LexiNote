package com.lexinote.app

import com.lexinote.app.ui.editor.formatDictionaryPartOfSpeech
import com.lexinote.app.ui.editor.formatDictionaryPhonetic
import org.junit.Assert.assertEquals
import org.junit.Test

class DictionaryDisplayFormatTest {
    @Test fun normalizesPhoneticDelimitersWithoutChangingIpa() {
        assertEquals("/ˈfæktəri/", formatDictionaryPhonetic(" ˈfæktəri "))
        assertEquals("/bʊk/", formatDictionaryPhonetic("/bʊk/"))
        assertEquals("/bʊk/", formatDictionaryPhonetic(" [bʊk] "))
        assertEquals("/bʊk/", formatDictionaryPhonetic("/[bʊk]/"))
        assertEquals("音标暂无", formatDictionaryPhonetic(null))
        assertEquals("音标暂无", formatDictionaryPhonetic("   "))
        assertEquals("音标暂无", formatDictionaryPhonetic("/ /"))
    }

    @Test fun normalizesKnownPartOfSpeechVariantsAndKeepsUnknownReadable() {
        listOf("N" to "n.", "n" to "n.", "n." to "n.", "V" to "v.", "VT" to "vt.", "VI" to "vi.",
            "ADJ" to "adj.", "adj." to "adj.", "ADV" to "adv.", "PRON" to "pron.", "PREP" to "prep.",
            "CONJ" to "conj.", "DET" to "det.", "NUM" to "num.", "INT" to "int.", "ART" to "art.", "AUX" to "aux.",
            "abbr" to "abbr.", "interj." to "interj.").forEach { (input, expected) ->
            assertEquals(input, expected, formatDictionaryPartOfSpeech(input))
        }
        assertEquals("unknown", formatDictionaryPartOfSpeech("UNKNOWN"))
        assertEquals("Xyz.", formatDictionaryPartOfSpeech("Xyz"))
    }
}
