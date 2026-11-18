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
        val repositoryRoot = sequenceOf(File("."), File(".."))
            .map(File::getAbsoluteFile)
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?: error("Unable to locate the repository root")
        val sourceRoots = sequenceOf("app", "core", "feature")
            .map { File(repositoryRoot, it) }
            .filter(File::isDirectory)
            .flatMap { it.walkTopDown().filter { dir -> dir.isDirectory && dir.path.endsWith("src/main/java") } }
            .toList()
        check(sourceRoots.any { it.path.contains("app") }) { "Unable to locate production Kotlin sources" }

        return sourceRoots.asSequence().flatMap { it.walkTopDown() }.filter { file -> file.extension == "kt" }
    }
}
