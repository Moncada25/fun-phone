package com.bookverse.contacts.security.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.widget.RemoteViews
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.security.CryptoManager
import com.bookverse.contacts.settings.LocaleController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class VaultAutofillService : AutofillService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }
        val job = scope.launch {
            val response = try {
                buildFillResponse(structure)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
            callback.onSuccess(response)
        }
        cancellationSignal.setOnCancelListener { job.cancel() }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val form = request.fillContexts.lastOrNull()?.structure?.let(AutofillStructureParser::parse)
        val password = form?.passwordText
        val failure = localized().getString(R.string.autofill_save_failed)
        if (form == null || password.isNullOrEmpty() || form.target.packageName == packageName) {
            callback.onFailure(failure)
            return
        }
        scope.launch {
            try {
                if (!VaultGate.isConfigured(this@VaultAutofillService)) {
                    callback.onFailure(localized().getString(R.string.autofill_save_unavailable))
                    return@launch
                }
                val sender = withContext(Dispatchers.IO) {
                    val intent = VaultAutofillActivity.saveIntent(
                        context = this@VaultAutofillService,
                        target = form.target,
                        usernameEnc = CryptoManager.encrypt(
                            this@VaultAutofillService,
                            form.usernameText.orEmpty().toByteArray(),
                        ),
                        passwordEnc = CryptoManager.encrypt(
                            this@VaultAutofillService,
                            password.toByteArray(),
                        ),
                    )
                    PendingIntent.getActivity(
                        this@VaultAutofillService,
                        nextRequestCode(),
                        intent,
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ).intentSender
                }
                callback.onSuccess(sender)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                callback.onFailure(failure)
            }
        }
    }

    private suspend fun buildFillResponse(structure: AssistStructure): FillResponse? {
        val form = AutofillStructureParser.parse(structure) ?: return null
        if (form.target.packageName == packageName) return null
        if (!VaultGate.isConfigured(this)) return null
        val fillIds = listOfNotNull(form.usernameId, form.passwordId)
        if (fillIds.isEmpty()) return null
        val response = FillResponse.Builder()
            .addDataset(lockedDataset(fillIds, authenticationSender()))
        form.passwordId?.let { passwordId ->
            val saveInfo = SaveInfo.Builder(
                SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                arrayOf(passwordId),
            ).setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
            form.usernameId?.let { saveInfo.setOptionalIds(arrayOf(it)) }
            response.setSaveInfo(saveInfo.build())
        }
        return response.build()
    }

    @Suppress("DEPRECATION")
    private fun lockedDataset(ids: List<AutofillId>, sender: IntentSender): Dataset {
        val presentation = presentation(
            packageName,
            localized().getString(R.string.autofill_unlock_prompt),
        )
        val dataset = Dataset.Builder(presentation)
        ids.forEach { dataset.setValue(it, null, presentation) }
        return dataset.setAuthentication(sender).build()
    }

    private fun authenticationSender(): IntentSender {
        val mutability = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        return PendingIntent.getActivity(
            this,
            nextRequestCode(),
            VaultAutofillActivity.fillIntent(this),
            PendingIntent.FLAG_CANCEL_CURRENT or mutability,
        ).intentSender
    }

    private fun localized(): Context = LocaleController.localizedContext(this)

    companion object {
        private val requestCodes = AtomicInteger(1)

        private fun nextRequestCode(): Int = requestCodes.getAndIncrement()

        internal fun presentation(packageName: String, text: String): RemoteViews =
            RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                setTextViewText(android.R.id.text1, text)
            }
    }
}
