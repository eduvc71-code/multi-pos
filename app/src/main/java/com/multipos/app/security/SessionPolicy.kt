package com.multipos.app.security

/**
 * Política de expiración de sesión.
 *
 * Pura y comprobable: recibe los tiempos por parámetro y no depende de reloj real.
 *  - Duración absoluta máxima de sesión: 12 horas.
 *  - Inactividad máxima: 30 minutos.
 */
object SessionPolicy {
    const val MAX_SESSION_DURATION_MS = 12L * 60L * 60L * 1000L
    const val MAX_INACTIVITY_MS = 30L * 60L * 1000L

    fun remainingSessionMs(sessionStartedAt: Long, now: Long): Long =
        MAX_SESSION_DURATION_MS - (now - sessionStartedAt)

    fun remainingInactivityMs(lastActivityAt: Long, now: Long): Long =
        MAX_INACTIVITY_MS - (now - lastActivityAt)

    fun isSessionExpired(
        sessionStartedAt: Long,
        lastActivityAt: Long,
        now: Long
    ): Boolean {
        if (sessionStartedAt <= 0L || lastActivityAt <= 0L) return true
        if (now - sessionStartedAt >= MAX_SESSION_DURATION_MS) return true
        if (now - lastActivityAt >= MAX_INACTIVITY_MS) return true
        return false
    }

    fun remainingLockedMs(blockedUntil: Long, now: Long): Long = (blockedUntil - now).coerceAtLeast(0L)
}