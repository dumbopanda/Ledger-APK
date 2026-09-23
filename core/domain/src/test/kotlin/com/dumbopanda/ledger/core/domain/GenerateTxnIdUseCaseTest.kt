package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.model.TxnId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GenerateTxnIdUseCaseTest {
    private val useCase = GenerateTxnIdUseCase()

    @Test
    fun `a generated N id starts with N and matches the format`() {
        val id = useCase.generate(isY = false, random = Random(1))
        assertTrue(TxnId.REGEX.matches(id.value))
        assertTrue(id.value.startsWith("N"))
    }

    @Test
    fun `a generated Y id starts with Y and matches the format`() {
        val id = useCase.generate(isY = true, random = Random(1))
        assertTrue(TxnId.REGEX.matches(id.value))
        assertTrue(id.value.startsWith("Y"))
    }

    @Test
    fun `isY discriminates correctly for both kinds`() {
        assertTrue(useCase.generate(isY = true, random = Random(2)).isY)
        assertEquals(false, useCase.generate(isY = false, random = Random(2)).isY)
    }

    @Test
    fun `the suffix is always exactly 6 lowercase-alphanumeric characters`() {
        val id = useCase.generate(isY = false, random = Random(3))
        assertEquals(7, id.value.length) // kind letter + 6-char suffix
        assertTrue(id.value.drop(1).all { it.isDigit() || it in 'a'..'z' })
    }

    @Test
    fun `a fixed seed is reproducible`() {
        val first = useCase.generate(isY = true, random = Random(42))
        val second = useCase.generate(isY = true, random = Random(42))
        assertEquals(first, second)
    }

    @Test
    fun `different seeds produce different ids`() {
        val first = useCase.generate(isY = true, random = Random(1))
        val second = useCase.generate(isY = true, random = Random(2))
        assertNotEquals(first, second)
    }

    @Test
    fun `regeneration keeps the same kind but changes the suffix`() {
        val a = useCase.generate(isY = false, random = Random(10))
        val b = useCase.generate(isY = false, random = Random(11))
        assertEquals(a.isY, b.isY)
        assertNotEquals(a, b)
    }
}
