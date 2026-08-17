package com.multipos.app.ui.pos

import java.util.concurrent.atomic.AtomicBoolean

internal class SaleSubmissionGuard {
    private val submissionInProgress = AtomicBoolean(false)

    val isInProgress: Boolean
        get() = submissionInProgress.get()

    fun tryStart(): Boolean = submissionInProgress.compareAndSet(false, true)

    fun finish() {
        submissionInProgress.set(false)
    }
}
