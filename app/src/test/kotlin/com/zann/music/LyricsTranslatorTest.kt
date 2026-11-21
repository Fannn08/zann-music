package com.zann.music

import com.zann.music.data.lyrics.LyricsTranslator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsTranslatorTest {
    @Test
    fun joinsEverySegmentOfTheResponse() {
        val raw = """[[["Halo dunia\n","Hello world\n",null,null,1],["Apa kabar","How are you",null,null,1]],null,"en"]"""
        assertEquals("Halo dunia\nApa kabar", LyricsTranslator.parse(raw))
    }

    @Test
    fun garbageIsNull() {
        assertNull(LyricsTranslator.parse("<html>blocked</html>"))
    }
}
