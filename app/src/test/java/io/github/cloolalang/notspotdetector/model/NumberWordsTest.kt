package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberWordsTest {

    @Test
    fun toWords_singleDigits() {
        assertEquals("zero", NumberWords.toWords(0))
        assertEquals("seven", NumberWords.toWords(7))
    }

    @Test
    fun toWords_teensAndTens() {
        assertEquals("thirteen", NumberWords.toWords(13))
        assertEquals("twenty", NumberWords.toWords(20))
        assertEquals("forty two", NumberWords.toWords(42))
    }

    @Test
    fun toWords_hundreds() {
        assertEquals("eight hundred", NumberWords.toWords(800))
        assertEquals("eight hundred fifty", NumberWords.toWords(850))
        assertEquals("nine hundred", NumberWords.toWords(900))
    }

    @Test
    fun toWords_thousands() {
        assertEquals("two thousand six hundred", NumberWords.toWords(2_600))
        assertEquals("one thousand eight hundred", NumberWords.toWords(1_800))
        assertEquals("five thousand two hundred", NumberWords.toWords(5_200))
        assertEquals("two thousand", NumberWords.toWords(2_000))
    }

    @Test
    fun toHundredsWords_underOneThousand_matchesToWords() {
        assertEquals("eight hundred", NumberWords.toHundredsWords(800))
        assertEquals("four hundred fifty", NumberWords.toHundredsWords(450))
    }

    @Test
    fun toHundredsWords_roundThousands_speaksInformalHundreds() {
        assertEquals("twenty six hundred", NumberWords.toHundredsWords(2_600))
        assertEquals("eighteen hundred", NumberWords.toHundredsWords(1_800))
        assertEquals("fifty two hundred", NumberWords.toHundredsWords(5_200))
        assertEquals("nineteen hundred", NumberWords.toHundredsWords(1_900))
    }

    @Test
    fun toHundredsWords_withRemainder_appendsRemainderWords() {
        assertEquals("twenty six hundred fifty", NumberWords.toHundredsWords(2_650))
    }
}
