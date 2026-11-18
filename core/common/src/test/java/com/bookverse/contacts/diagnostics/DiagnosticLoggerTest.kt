package com.bookverse.contacts.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DiagnosticLoggerTest {
    @Test
    fun shouldRecordOnlyTypedDiagnosticEvents() {
        val recorded = mutableListOf<Pair<DiagnosticLevel, DiagnosticEvent>>()
        val logger = DiagnosticLogger { level, event -> recorded += level to event }

        logger.debug(DiagnosticEvent.APP_DATA_LOAD_COMPLETED)
        logger.warning(DiagnosticEvent.CALL_NOTIFICATION_LATE_UPDATE_IGNORED)
        logger.error(DiagnosticEvent.CONTACT_PHOTO_DECODE_FAILED)

        assertEquals(
            listOf(
                DiagnosticLevel.DEBUG to DiagnosticEvent.APP_DATA_LOAD_COMPLETED,
                DiagnosticLevel.WARNING to DiagnosticEvent.CALL_NOTIFICATION_LATE_UPDATE_IGNORED,
                DiagnosticLevel.ERROR to DiagnosticEvent.CONTACT_PHOTO_DECODE_FAILED,
            ),
            recorded,
        )
    }

    @Test
    fun shouldUseNoOpSinkForReleaseBuilds() {
        assertSame(NoOpDiagnosticSink, defaultDiagnosticSink(isDebugBuild = false))
    }
}
