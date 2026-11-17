package com.bookverse.contacts.diagnostics

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticLoggingBoundaryTest {
    @Test
    fun shouldKeepAndroidLoggingInsideTheDiagnosticImplementation() {
        val violations = productionSources()
            .filter { source -> source.name != "DiagnosticLog.kt" }
            .filter { source -> "android.util.Log" in source.readText() }
            .map(File::getPath)
            .toList()

        assertEquals(emptyList<String>(), violations)
    }

    @Test
    fun shouldRejectProductionStackTracePrinting() {
        val violations = productionSources()
            .filter { source -> "printStackTrace" in source.readText() }
            .map(File::getPath)
            .toList()

        assertEquals(emptyList<String>(), violations)
    }

    @Test
    fun shouldExposeOnlyContentFreeEventIdentifiers() {
        assertTrue(
            DiagnosticEvent.entries.all { event ->
                event.name.matches(Regex("[A-Z][A-Z0-9_]*"))
            },
        )
    }

    private fun productionSources(): Sequence<File> {
        val sourceRoot = sequenceOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).firstOrNull(File::isDirectory)
            ?: error("Unable to locate production Kotlin sources")

        return sourceRoot.walkTopDown().filter { file -> file.extension == "kt" }
    }
}
