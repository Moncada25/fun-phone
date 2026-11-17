package com.bookverse.contacts.ui.screens.contacts

import android.Manifest
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.provider.ContactsContract
import androidx.core.net.toUri
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.App
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.testing.ComposeTestContent
import com.bookverse.contacts.testing.ComposeTestHostActivity
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactSaveResult
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import contacts.core.entities.PhoneEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.Assume.assumeTrue
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class ContactCrudFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestHostActivity>()

    @Test
    fun createAndEditThroughComposeThenDeleteFromProvider() {
        assumeTrue("Synthetic provider CRUD runs only on an emulator", isEmulator())
        grantContactPermissions()
        val prefix = "FunPhoneTest"
        val suffix = System.currentTimeMillis().toString().takeLast(8)
        val originalName = "$prefix $suffix"
        val updatedName = "$originalName Updated"
        val phoneNumber = "55501${suffix.takeLast(5)}"
        cleanupContacts(prefix)

        val app = composeRule.activity.application as App
        val contactsViewModel = ViewModelProvider(
            composeRule.activity,
            app.container.contactsViewModelFactory,
        )[ContactsViewModel::class.java]
        val settingsViewModel = ViewModelProvider(
            composeRule.activity,
            app.container.appSettingsViewModelFactory,
        )[AppSettingsViewModel::class.java]
        settingsViewModel.setDefaultContactAccount(
            composeRule.activity,
            ContactAccount.LOCAL_ACCOUNT,
        )

        var contactId: Long? = null
        try {
            val createdContact = AtomicReference<Contact>()
            showCreateForm(
                phoneNumber = phoneNumber,
                settingsViewModel = settingsViewModel,
                contactsViewModel = contactsViewModel,
                onSaved = createdContact::set,
            )
            firstEditableField().performTextInput(originalName)
            firstEditableField().assertTextContains(originalName)
            val saveButton = composeRule
                .onNodeWithText(composeRule.activity.getString(R.string.save))
            saveButton
                .assertIsEnabled()
                .assertIsDisplayed()
                .performClick()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                createdContact.get() != null
            }
            assertTrue(createdContact.get().id.isNotBlank())

            composeRule.waitUntil(timeoutMillis = 15_000) {
                findContactIdByName(originalName) != null
            }
            val createdId = requireNotNull(findContactIdByName(originalName))
            contactId = createdId
            assertEquals(originalName, findDisplayName(createdId))
            assertTrue(findPhoneNumbers(createdId).any { it.onlyDigits() == phoneNumber })

            contactsViewModel.loadContacts()
            composeRule.waitUntil(timeoutMillis = 15_000) {
                contactsViewModel.findContactById(createdId.toString()) != null
            }
            val persisted = contactsViewModel.findContactById(createdId.toString())
            assertNotNull(persisted)

            val updatedContact = AtomicReference<Contact>()
            showUpdateForm(
                contact = persisted!!,
                settingsViewModel = settingsViewModel,
                contactsViewModel = contactsViewModel,
                onSaved = updatedContact::set,
            )
            firstEditableField().performTextClearance()
            firstEditableField().performTextInput(updatedName)
            composeRule
                .onNodeWithText(composeRule.activity.getString(R.string.save))
                .performClick()

            composeRule.waitUntil(timeoutMillis = 15_000) {
                findDisplayName(createdId) == updatedName
            }
            assertEquals(updatedName, findDisplayName(createdId))
            assertEquals(updatedName, updatedContact.get()?.name)

            contactsViewModel.loadContacts()
            composeRule.waitUntil(timeoutMillis = 15_000) {
                contactsViewModel.findContactById(createdId.toString())?.name == updatedName
            }
            val calledNumber = AtomicReference<String>()
            showDialer(
                query = phoneNumber.takeLast(5),
                settingsViewModel = settingsViewModel,
                contactsViewModel = contactsViewModel,
                onCall = calledNumber::set,
            )
            composeRule.onNodeWithText(updatedName).assertIsDisplayed()
            composeRule
                .onAllNodes(
                    hasContentDescription(composeRule.activity.getString(R.string.call))
                )[0]
                .performClick()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                calledNumber.get() != null
            }
            assertEquals(phoneNumber, calledNumber.get().onlyDigits())

            val deleted = AtomicBoolean(false)
            contactsViewModel.deleteContact(createdId.toString(), onResult = deleted::set)
            composeRule.waitUntil(timeoutMillis = 15_000) {
                deleted.get() && findDisplayName(createdId) == null
            }
            assertTrue(deleted.get())
        } finally {
            contactId?.let(::deleteContact)
            cleanupContacts(prefix)
        }
    }

    @Test
    fun highResolutionPhotoSurvivesCreateReplaceReloadAndRemoval() {
        assumeTrue("Synthetic provider CRUD runs only on an emulator", isEmulator())
        grantContactPermissions()
        val prefix = "FunPhonePhotoTest"
        val suffix = System.currentTimeMillis().toString().takeLast(8)
        val name = "$prefix $suffix"
        val phoneNumber = "55502${suffix.takeLast(5)}"
        val firstSource = createDetailedPhoto("first", Color.RED, Color.YELLOW)
        val secondSource = createDetailedPhoto(
            label = "second",
            firstColor = Color.BLUE,
            secondColor = Color.CYAN,
            fileNamePrefix = "crop_",
        )
        cleanupContacts(prefix)

        val app = composeRule.activity.application as App
        val contactsViewModel = ViewModelProvider(
            composeRule.activity,
            app.container.contactsViewModelFactory,
        )[ContactsViewModel::class.java]
        var contactId: Long? = null
        try {
            val createResult = AtomicReference<ContactSaveResult>()
            contactsViewModel.createContact(
                contact = Contact(
                    id = "",
                    name = name,
                    phones = listOf(
                        ContactPhone(
                            number = phoneNumber,
                            type = PhoneEntity.Type.MOBILE,
                        ),
                    ),
                    photoUri = firstSource.toUri(),
                ),
                account = ContactAccount.LOCAL_ACCOUNT,
                onSaveResult = createResult::set,
            )
            composeRule.waitUntil(timeoutMillis = 20_000) { createResult.get() != null }
            assertTrue(createResult.get() is ContactSaveResult.Success)

            composeRule.waitUntil(timeoutMillis = 15_000) {
                findContactIdByName(name) != null
            }
            val createdId = requireNotNull(findContactIdByName(name))
            contactId = createdId
            contactsViewModel.loadContacts()
            composeRule.waitUntil(timeoutMillis = 15_000) {
                contactsViewModel.findContactById(createdId.toString())?.photoUri?.scheme == "file"
            }
            val created = requireNotNull(contactsViewModel.findContactById(createdId.toString()))
            val firstLocalUri = requireNotNull(created.photoUri)
            assertLocalPhotoDimensions(firstLocalUri.path)
            assertProviderPhotoDimensions(createdId)

            val updateResult = AtomicReference<ContactSaveResult>()
            contactsViewModel.updateContact(
                contact = created.copy(photoUri = secondSource.toUri()),
                successMessage = "",
                onResult = updateResult::set,
            )
            composeRule.waitUntil(timeoutMillis = 20_000) { updateResult.get() != null }
            val callbackContact = (updateResult.get() as? ContactSaveResult.Success)?.contact
            assertNotNull(callbackContact)
            assertNotEquals(secondSource.toUri(), callbackContact?.photoUri)
            assertEquals("file", callbackContact?.photoUri?.scheme)
            assertLocalPhotoDimensions(callbackContact?.photoUri?.path)
            composeRule.waitUntil(timeoutMillis = 20_000) {
                contactsViewModel.findContactById(createdId.toString())?.photoUri?.let { uri ->
                    uri.scheme == "file" && uri != firstLocalUri
                } == true
            }
            val updated = requireNotNull(contactsViewModel.findContactById(createdId.toString()))
            assertLocalPhotoDimensions(updated.photoUri?.path)

            val removeResult = AtomicReference<ContactSaveResult>()
            contactsViewModel.updateContact(
                contact = updated.copy(photoUri = null),
                successMessage = "",
                onResult = removeResult::set,
            )
            composeRule.waitUntil(timeoutMillis = 20_000) { removeResult.get() != null }
            composeRule.waitUntil(timeoutMillis = 20_000) {
                contactsViewModel.findContactById(createdId.toString())?.photoUri == null
            }
            assertTrue(openProviderPhoto(createdId) == null)
        } finally {
            contactId?.let(::deleteContact)
            cleanupContacts(prefix)
            firstSource.delete()
            secondSource.delete()
        }
    }

    private fun showCreateForm(
        phoneNumber: String,
        settingsViewModel: AppSettingsViewModel,
        contactsViewModel: ContactsViewModel,
        onSaved: (Contact) -> Unit,
    ) {
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                MaterialTheme {
                    CreateContactScreen(
                        onContactSaved = onSaved,
                        onBackClick = {},
                        phoneNumber = phoneNumber,
                        country = null,
                        appSettingsViewModel = settingsViewModel,
                        contactsViewModel = contactsViewModel,
                    )
                }
            }
        }
    }

    private fun showUpdateForm(
        contact: Contact,
        settingsViewModel: AppSettingsViewModel,
        contactsViewModel: ContactsViewModel,
        onSaved: (Contact) -> Unit,
    ) {
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                MaterialTheme {
                    UpdateContactScreen(
                        onContactUpdated = onSaved,
                        onBackClick = {},
                        contactToEdit = contact,
                        appSettingsViewModel = settingsViewModel,
                        contactsViewModel = contactsViewModel,
                    )
                }
            }
        }
    }

    private fun showDialer(
        query: String,
        settingsViewModel: AppSettingsViewModel,
        contactsViewModel: ContactsViewModel,
        onCall: (String) -> Unit,
    ) {
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                MaterialTheme {
                    DialerScreen(
                        onBack = {},
                        appSettingsViewModel = settingsViewModel,
                        contactsViewModel = contactsViewModel,
                        phoneNumber = query,
                        onPhoneNumberChange = {},
                        onCallPhoneNumber = { _, number -> onCall(number) },
                    )
                }
            }
        }
    }

    private fun firstEditableField() = composeRule.onAllNodes(hasSetTextAction())[0]

    private fun grantContactPermissions() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val packageName = composeRule.activity.packageName
        automation.grantRuntimePermission(packageName, Manifest.permission.READ_CONTACTS)
        automation.grantRuntimePermission(packageName, Manifest.permission.WRITE_CONTACTS)
        automation.grantRuntimePermission(packageName, Manifest.permission.GET_ACCOUNTS)
    }

    private fun findContactIdByName(name: String): Long? {
        val resolver = composeRule.activity.contentResolver
        return resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID),
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} = ?",
            arrayOf(name),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }

    private fun assertLocalPhotoDimensions(path: String?) {
        val bitmap = requireNotNull(path?.let(BitmapFactory::decodeFile))
        try {
            assertEquals(2_048, maxOf(bitmap.width, bitmap.height))
        } finally {
            bitmap.recycle()
        }
    }

    private fun assertProviderPhotoDimensions(contactId: Long) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            openProviderPhoto(contactId)?.let { bitmap ->
                bitmap.recycle()
                true
            } ?: false
        }
        val bitmap = requireNotNull(openProviderPhoto(contactId))
        try {
            val providerMaximum = queryProviderMaximumDimension()
            assertTrue(maxOf(bitmap.width, bitmap.height) <= providerMaximum)
        } finally {
            bitmap.recycle()
        }
    }

    private fun openProviderPhoto(contactId: Long): Bitmap? {
        val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        return ContactsContract.Contacts.openContactPhotoInputStream(
            composeRule.activity.contentResolver,
            uri,
            true,
        )?.use(BitmapFactory::decodeStream)
    }

    private fun queryProviderMaximumDimension(): Int {
        return composeRule.activity.contentResolver.query(
            ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI,
            arrayOf(ContactsContract.DisplayPhoto.DISPLAY_MAX_DIM),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use 2_048
            cursor.getInt(0)
        } ?: 2_048
    }

    private fun createDetailedPhoto(
        label: String,
        firstColor: Int,
        secondColor: Int,
        fileNamePrefix: String? = null,
    ): File {
        val file = File.createTempFile(
            fileNamePrefix ?: "contact-$label-",
            ".png",
            composeRule.activity.cacheDir,
        )
        val bitmap = Bitmap.createBitmap(3_000, 2_000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        repeat(30) { index ->
            paint.color = if (index % 2 == 0) firstColor else secondColor
            canvas.drawRect(
                index * 100f,
                0f,
                (index + 1) * 100f,
                bitmap.height.toFloat(),
                paint,
            )
        }
        try {
            FileOutputStream(file).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            bitmap.recycle()
        }
        return file
    }

    private fun findPhoneNumbers(contactId: Long): List<String> {
        val resolver = composeRule.activity.contentResolver
        return resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null,
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }.orEmpty()
    }

    private fun findDisplayName(contactId: Long): String? {
        val resolver = composeRule.activity.contentResolver
        return resolver.query(
            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId),
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun deleteContact(contactId: Long) {
        composeRule.activity.contentResolver.delete(
            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId),
            null,
            null,
        )
    }

    private fun cleanupContacts(prefix: String) {
        val resolver = composeRule.activity.contentResolver
        val ids = resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID),
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?",
            arrayOf("$prefix%"),
            null,
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getLong(0))
            }
        }.orEmpty()
        ids.forEach(::deleteContact)
    }

    private fun String.onlyDigits(): String = filter(Char::isDigit)

    private fun isEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator") ||
            Build.MODEL.contains("sdk_gphone")
}
