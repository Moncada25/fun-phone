package com.bookverse.contacts.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    private val packageName = targetPackageName()

    @Before
    fun prepare() {
        prepareSyntheticContacts(packageName)
    }

    @Test
    fun generateStartupProfile() {
        baselineProfileRule.collect(
            packageName = packageName,
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            awaitContactsList()
        }
    }

    @Test
    fun generateCoreJourneysProfile() {
        baselineProfileRule.collect(packageName = packageName) {
            pressHome()
            startActivityAndWait()
            exerciseCoreJourneys()
        }
    }
}
