package com.dumbopanda.ledger.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class DecodeJwtEmailTest {
    private fun fakeJwt(payloadJson: String): String {
        val header = urlEncode("""{"alg":"none"}""")
        val payload = urlEncode(payloadJson)
        return "$header.$payload.signature"
    }

    private fun urlEncode(text: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray())

    @Test
    fun `extracts the email claim from a real-shaped id token`() {
        val jwt = fakeJwt("""{"sub":"12345","email":"person@example.com","email_verified":true}""")
        assertEquals("person@example.com", decodeJwtEmail(jwt))
    }

    @Test
    fun `returns null when there is no email claim`() {
        val jwt = fakeJwt("""{"sub":"12345"}""")
        assertNull(decodeJwtEmail(jwt))
    }

    @Test
    fun `returns null for a token that isn't three dot-separated segments`() {
        assertNull(decodeJwtEmail("not-a-jwt"))
    }

    @Test
    fun `returns null for a payload segment that isn't valid base64url`() {
        assertNull(decodeJwtEmail("header.###.signature"))
    }

    @Test
    fun `returns null for a payload segment that decodes but isn't valid JSON`() {
        val jwt = "header.${urlEncode("not json")}.signature"
        assertNull(decodeJwtEmail(jwt))
    }

    @Test
    fun `handles a payload whose base64url length needs padding restored`() {
        // Deliberately pick a JSON body whose base64url encoding has no trailing '=' to prove
        // the padding-restoration logic isn't just accidentally unexercised by other cases.
        val jwt = fakeJwt("""{"email":"a@b.co"}""")
        assertEquals("a@b.co", decodeJwtEmail(jwt))
    }
}
