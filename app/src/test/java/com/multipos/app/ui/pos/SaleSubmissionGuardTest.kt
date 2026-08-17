package com.multipos.app.ui.pos

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SaleSubmissionGuardTest {
    @Test
    fun secondSubmissionIsRejectedWhileFirstIsInProgress() {
        val guard = SaleSubmissionGuard()

        assertTrue(guard.tryStart())
        assertFalse(guard.tryStart())
        assertTrue(guard.isInProgress)
    }

    @Test
    fun submissionCanStartAgainAfterFinishing() {
        val guard = SaleSubmissionGuard()

        assertTrue(guard.tryStart())
        guard.finish()

        assertFalse(guard.isInProgress)
        assertTrue(guard.tryStart())
    }
}
