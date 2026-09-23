package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.model.TxnId
import javax.inject.Inject
import kotlin.random.Random

/** §7.5.5: `N` or `Y` followed by 6 random lowercase-alphanumeric characters, generated fresh
 * on every kind change and every regenerate tap. [random] defaults to the real generator in
 * production and takes a seeded one in tests, so the format is verifiable without flakiness. */
class GenerateTxnIdUseCase @Inject constructor() {
    fun generate(isY: Boolean, random: Random = Random.Default): TxnId {
        val suffix = buildString {
            repeat(SUFFIX_LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
        }
        return TxnId((if (isY) "Y" else "N") + suffix)
    }

    private companion object {
        const val SUFFIX_LENGTH = 6
        const val ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
    }
}
