package com.multipos.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPolicyTest {

    @Test
    fun session_valid_when_within_limits() {
        val started = 1_000_000L
        val lastActivity = 1_000_000L
        val now = 1_000_000L + 60_000L // 1 minuto de uso
        assertFalse(SessionPolicy.isSessionExpired(started, lastActivity, now))
    }

    @Test
    fun session_expires_after_absolute_maximum() {
        val started = 1_000_000L
        val lastActivity = 1_000_000L + SessionPolicy.MAX_SESSION_DURATION_MS - 1_000L
        val now = started + SessionPolicy.MAX_SESSION_DURATION_MS // se cumple el tope absoluto
        assertTrue(SessionPolicy.isSessionExpired(started, lastActivity, now))
    }

    @Test
    fun session_expires_by_inactivity() {
        val started = 1_000_000L
        val lastActivity = 1_000_000L
        val now = lastActivity + SessionPolicy.MAX_INACTIVITY_MS // 30 min sin actividad
        assertTrue(SessionPolicy.isSessionExpired(started, lastActivity, now))
    }

    @Test
    fun session_not_expired_by_inactivity_yet() {
        val started = 1_000_000L
        val lastActivity = 1_000_000L
        val now = lastActivity + SessionPolicy.MAX_INACTIVITY_MS - 1_000L
        assertFalse(SessionPolicy.isSessionExpired(started, lastActivity, now))
    }

    @Test
    fun session_expires_when_no_timestamps() {
        assertTrue(SessionPolicy.isSessionExpired(0L, 0L, System.currentTimeMillis()))
    }

    @Test
    fun remaining_session_is_reported() {
        assertEquals(
            SessionPolicy.MAX_SESSION_DURATION_MS - 60_000L,
            SessionPolicy.remainingSessionMs(1_000_000L, 1_000_000L + 60_000L)
        )
    }

    @Test
    fun remaining_inactivity_is_reported() {
        assertEquals(
            SessionPolicy.MAX_INACTIVITY_MS - 60_000L,
            SessionPolicy.remainingInactivityMs(1_000_000L, 1_000_000L + 60_000L)
        )
    }
}