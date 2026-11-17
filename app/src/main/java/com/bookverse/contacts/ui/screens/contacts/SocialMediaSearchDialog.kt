package com.bookverse.contacts.ui.screens.contacts

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.SocialMediaType
import com.bookverse.contacts.domain.contacts.SocialLinkPolicy
import com.bookverse.contacts.ui.components.SocialMediaButton
import com.bookverse.contacts.utils.ExternalUriLauncher
import java.io.ByteArrayInputStream
import kotlinx.coroutines.delay

private const val DEFAULT_WEBSITE_URL = "https://google.com"
private const val INPUT_DEBOUNCE_MS = 350L
private enum class PreviewStatus { LOADING, CONTENT, ERROR }

@Composable
fun SocialMediaSearchDialog(
    onDismiss: () -> Unit,
    onSocialNetworkSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var network by remember { mutableStateOf(SocialMediaType.FACEBOOK) }
    var query by remember { mutableStateOf("") }
    var websiteMode by remember { mutableStateOf(false) }
    var website by remember { mutableStateOf(DEFAULT_WEBSITE_URL) }
    val selectedUrl = if (websiteMode) SocialLinkPolicy.website(website) else buildSocialMediaProfile(network, query)
    val candidate = selectedUrl ?: if (!websiteMode && query.isBlank()) SocialLinkPolicy.home(network.domain) else null
    var previewUrl by remember { mutableStateOf<String?>(SocialLinkPolicy.home(network.domain)) }
    var retry by remember { mutableIntStateOf(0) }
    var externalError by remember { mutableStateOf(false) }
    var confirmed by remember { mutableStateOf(false) }

    LaunchedEffect(candidate) {
        delay(INPUT_DEBOUNCE_MS)
        previewUrl = candidate
        externalError = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.add_website),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(
                        enabled = selectedUrl != null && !confirmed,
                        onClick = {
                            selectedUrl?.let {
                                confirmed = true
                                onSocialNetworkSelected(it)
                                onDismiss()
                            }
                        },
                    ) {
                        Icon(Icons.Default.Check, stringResource(R.string.confirm))
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, stringResource(R.string.close)) }
                }
                BoxWithConstraints(Modifier.weight(1f)) {
                    val previewHeight = maxOf(240.dp, maxHeight - 240.dp)
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = if (websiteMode) website else query,
                            onValueChange = { if (websiteMode) website = it else query = it },
                            label = { Text(stringResource(if (websiteMode) R.string.search_website else R.string.search_username)) },
                            isError = candidate == null && (if (websiteMode) website else query).isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = if (candidate == null) ({ Text(stringResource(R.string.social_preview_invalid_url)) }) else null,
                        )
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        ) {
                            listOf(SocialMediaType.FACEBOOK, SocialMediaType.INSTAGRAM, SocialMediaType.LINKEDIN).forEach { type ->
                                SocialMediaButton(
                                    type = type,
                                    isSelected = !websiteMode && network == type,
                                    fullColor = !websiteMode && network == type,
                                    onClick = { network = type; websiteMode = false; query = "" },
                                    accentColor = colors.primary,
                                )
                            }
                            WebsiteButton(websiteMode, { websiteMode = true })
                        }
                        Column(Modifier.fillMaxWidth()) {
                            TextButton(
                                enabled = previewUrl != null,
                                onClick = {
                                    previewUrl?.let { externalError = ExternalUriLauncher.open(context, it) != ExternalUriLauncher.Result.OPENED }
                                },
                            ) { Text(stringResource(R.string.social_preview_external)) }
                            if (externalError) Text(stringResource(R.string.social_preview_unavailable), style = MaterialTheme.typography.bodySmall)
                        }
                        EmbeddedWebsitePreview(
                            url = previewUrl,
                            retry = retry,
                            onRetry = { retry++ },
                            modifier = Modifier.fillMaxWidth().height(previewHeight).clip(RoundedCornerShape(16.dp)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun EmbeddedWebsitePreview(url: String?, retry: Int, onRetry: () -> Unit, modifier: Modifier) {
    var status by remember { mutableStateOf(PreviewStatus.LOADING) }
    // Track the requested URL, not WebView.url: redirects must not restart loading on recomposition.
    var loadedRequest by remember { mutableStateOf<Pair<String?, Int>?>(null) }
    LaunchedEffect(url, retry) {
        status = PreviewStatus.LOADING
        delay(20_000)
        if (status == PreviewStatus.LOADING) status = PreviewStatus.ERROR
    }
    Box(modifier.background(MaterialTheme.colorScheme.surface)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.safeBrowsingEnabled = true
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                            if (SocialLinkPolicy.website(request.url.toString()) != null) return null
                            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                        }
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val blocked = SocialLinkPolicy.website(request.url.toString()) == null
                            if (blocked && request.isForMainFrame) status = PreviewStatus.ERROR
                            return blocked
                        }
                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            if (url != "about:blank") status = PreviewStatus.LOADING
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            if (url != "about:blank" && status != PreviewStatus.ERROR) status = PreviewStatus.CONTENT
                        }
                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            if (request.isForMainFrame) status = PreviewStatus.ERROR
                        }
                        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                            if (request.isForMainFrame && response.statusCode >= 400) status = PreviewStatus.ERROR
                        }
                        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                            handler.cancel()
                            status = PreviewStatus.ERROR
                        }
                    }
                }
            },
            update = { view ->
                if (loadedRequest != (url to retry)) {
                    loadedRequest = url to retry
                    view.stopLoading()
                    view.loadUrl(url ?: "about:blank")
                }
            },
            onRelease = { view ->
                view.stopLoading()
                view.webViewClient = WebViewClient()
                view.loadUrl("about:blank")
                view.removeAllViews()
                view.destroy()
            },
        )
        if (url == null || status == PreviewStatus.ERROR) {
            Column(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(if (url == null) R.string.social_preview_invalid_url else R.string.social_preview_unavailable))
                if (url != null) TextButton(onClick = onRetry) { Text(stringResource(R.string.social_preview_retry)) }
            }
        } else if (status == PreviewStatus.LOADING) {
            CircularProgressIndicator(Modifier.align(Alignment.Center).size(40.dp))
        }
    }
}

internal fun buildSocialMediaProfile(networkType: SocialMediaType, username: String): String? =
    SocialLinkPolicy.profile(networkType.domain, username)

@Composable
private fun WebsiteButton(selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {}.selectable(selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(50.dp).clip(CircleShape).background(colors.surfaceContainerHighest)
                .border(if (selected) 3.dp else 0.dp, if (selected) colors.primary else colors.surfaceContainerHighest, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Public, null, tint = colors.onSurface)
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.website), style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
