package com.friction.app

import com.friction.app.utils.RoastMessages
import org.junit.Assert.*
import org.junit.Test

class RoastMessagesTest {

    @Test
    fun `getRandom returns non-null non-blank string`() {
        val msg = RoastMessages.getRandom()
        assertNotNull(msg)
        assertTrue("Message should not be blank", msg.isNotBlank())
    }

    @Test
    fun `getRandom returns strings from the pool`() {
        // Call many times to exercise randomness; all should be non-blank
        val messages = (1..50).map { RoastMessages.getRandom() }
        assertTrue(messages.all { it.isNotBlank() })
        // Expect at least 2 distinct messages over 50 calls
        assertTrue("Should return varied messages", messages.toSet().size >= 2)
    }
}
