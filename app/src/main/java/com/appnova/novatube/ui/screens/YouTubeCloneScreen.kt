package com.appnova.novatube.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.appnova.novatube.BuildConfig
import com.appnova.novatube.R
import com.appnova.novatube.adblock.BraveShieldsEngine
import com.appnova.novatube.anichin.AnichinLibraryRepository
import com.appnova.novatube.anichin.AnichinUpdateChecker
import com.appnova.novatube.config.NovaChannel
import com.appnova.novatube.config.NovaConfig
import com.appnova.novatube.config.NovaThemeStyle
import com.appnova.novatube.config.PlaybackConfig
import com.appnova.novatube.config.SkipMode
import com.appnova.novatube.config.SponsorBlockConfig
import com.appnova.novatube.config.SponsorCategory
import com.appnova.novatube.config.UiConfig
import com.appnova.novatube.player.NovaPlaybackService
import com.appnova.novatube.sponsorblock.SponsorSegment
import com.appnova.novatube.ui.theme.NovaColors
import com.appnova.novatube.updater.GitHubUpdater
import com.appnova.novatube.updater.UpdateInfo
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Custom WebView that:
 * 1. Keeps visibility as VISIBLE when Background Playback is enabled so Chromium never pauses
 *    audio/video when the screen turns off or the user switches apps.
 * 2. Notifies the UI whenever the user touches the screen so the floating NovaTube pill
 *    can temporarily appear and then auto-hide after 2.5 seconds.
 */
class NovaYouTubeWebView(context: Context) : WebView(context) {
    var allowBackgroundPlay: Boolean = true
    var onUserTouch: (() -> Unit)? = null

    override fun onWindowVisibilityChanged(visibility: Int) {
        if (allowBackgroundPlay && visibility != View.VISIBLE) {
            super.onWindowVisibilityChanged(View.VISIBLE)
        } else {
            super.onWindowVisibilityChanged(visibility)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.actionMasked == MotionEvent.ACTION_DOWN) {
            onUserTouch?.invoke()
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && (w != oldw || h != oldh)) {
            if (scrollX != 0) scrollTo(0, scrollY)
            post { fixViewportAlignment(this) }
        }
    }
}

private fun isGoogleOrYouTubeAuthOrContentHost(host: String): Boolean {
    val h = host.lowercase()
    return h.contains("youtube.") ||
        h.contains("youtu.be") ||
        h.contains("google.") ||
        h.contains("gstatic.com") ||
        h.contains("googleapis.com") ||
        h.contains("googleusercontent.com") ||
        h.contains("ggpht.com") ||
        h.contains("ytimg.com")
}

private fun isYouTubeHost(url: String?): Boolean {
    val lower = url?.lowercase().orEmpty()
    return lower.contains("youtube.com") || lower.contains("youtu.be")
}

private fun isAnichinDomain(host: String): Boolean {
    val h = host.lowercase()
    return h == "anichin.moe" || h.endsWith(".anichin.moe")
}

private fun isAnichinHost(url: String?): Boolean {
    val lower = url?.lowercase().orEmpty()
    return lower.contains("anichin.moe")
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeCloneScreen(
    config: NovaConfig,
    isInPipMode: Boolean,
    onFetchSponsorSegments: (String, (List<SponsorSegment>) -> Unit) -> Unit,
    onCheckSponsor: (List<SponsorSegment>, Long) -> SponsorSegment?,
    onMarkSponsorSkipped: (String, SponsorSegment) -> Boolean,
    onVideoPlayingStateChanged: (Boolean) -> Unit,
    onUpdatePlayback: ((PlaybackConfig) -> PlaybackConfig) -> Unit,
    onUpdateSponsorBlock: ((SponsorBlockConfig) -> SponsorBlockConfig) -> Unit,
    onToggleSponsorCategory: (SponsorCategory, Boolean) -> Unit,
    onSetSkipMode: (SkipMode) -> Unit,
    onUpdateUi: ((UiConfig) -> UiConfig) -> Unit,
    onToggleTabVisibility: (String) -> Unit,
    onMoveTab: (String, Int) -> Unit,
    onResetDefaults: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var webViewRef by remember { mutableStateOf<NovaYouTubeWebView?>(null) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var currentVideoId by remember { mutableStateOf("") }
    var isWatchPage by remember { mutableStateOf(false) }
    var isVideoPlaying by remember { mutableStateOf(false) }
    var sponsorSegments by remember { mutableStateOf<List<SponsorSegment>>(emptyList()) }
    var activeManualSegment by remember { mutableStateOf<SponsorSegment?>(null) }
    var hudText by remember { mutableStateOf<String?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Anichin Favorites, History & Donghua Update Notifier
    val anichinRepo = remember { AnichinLibraryRepository(context.applicationContext) }
    val anichinLibrary by anichinRepo.data.collectAsState()
    var showAnichinLibrarySheet by remember { mutableStateOf(false) }
    var isCheckingAnichinUpdates by remember { mutableStateOf(false) }
    var currentAnichinUrl by remember { mutableStateOf("") }
    var currentAnichinTitle by remember { mutableStateOf("") }
    var currentAnichinSeriesUrl by remember { mutableStateOf("") }
    var currentAnichinEpisode by remember { mutableStateOf("") }

    // GitHub Releases In-App Self-Updater state
    val updateScope = rememberCoroutineScope()
    var pendingUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var updateDownloadProgress by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        pendingUpdate = GitHubUpdater.checkForUpdate(BuildConfig.VERSION_NAME)
        val favs = anichinLibrary.favorites
        if (favs.isNotEmpty()) {
            val (updatedAll, newlyUpdated) = AnichinUpdateChecker.checkUpdates(favs)
            if (newlyUpdated.isNotEmpty()) {
                anichinRepo.applyDetectedUpdates(updatedAll)
                AnichinUpdateChecker.showUpdateNotification(context, newlyUpdated)
                val first = newlyUpdated.first()
                hudText = "Update Donghua: ${first.title} (${first.lastKnownEpisode})"
            }
        }
    }

    // Track last user touch so the floating NovaTube pill auto-hides after 2.5 seconds
    var lastInteractionUptimeMs by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var pillVisible by remember { mutableStateOf(true) }

    var activeSpeed by remember { mutableFloatStateOf(config.playback.defaultSpeed) }
    var activeLoop by remember { mutableStateOf(config.playback.loopByDefault) }

    // Fullscreen custom view state from WebChromeClient
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    BackHandler {
        if (customFullscreenView != null) {
            (customFullscreenView?.parent as? ViewGroup)?.removeView(customFullscreenView)
            customViewCallback?.onCustomViewHidden()
            customFullscreenView = null
            customViewCallback = null
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            webViewRef?.let { fixViewportAlignment(it) }
        } else if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
            webViewRef?.let { fixViewportAlignment(it) }
        } else {
            activity?.moveTaskToBack(true)
        }
    }

    // Hide system bars automatically when rotated to landscape or in custom fullscreen view
    DisposableEffect(isLandscape, customFullscreenView) {
        if (activity != null) {
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            if (isLandscape || customFullscreenView != null) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        webViewRef?.let { fixViewportAlignment(it) }
        onDispose { }
    }

    // Auto-hide the NovaTube floating pill after 2.5s of inactivity
    LaunchedEffect(lastInteractionUptimeMs, isVideoPlaying, isWatchPage) {
        pillVisible = true
        val timeoutMs = if (isWatchPage || isVideoPlaying) 2500L else 3500L
        delay(timeoutMs)
        pillVisible = false
    }

    LaunchedEffect(hudText) {
        if (hudText != null) {
            delay(1400L)
            hudText = null
        }
    }

    // Re-apply CSS / playback settings whenever config changes (only on supported channel pages)
    LaunchedEffect(config, webViewRef) {
        val wv = webViewRef ?: return@LaunchedEffect
        wv.allowBackgroundPlay = config.playback.backgroundPlayEnabled
        activeSpeed = config.playback.defaultSpeed
        activeLoop = config.playback.loopByDefault
        if (isYouTubeHost(wv.url)) {
            injectNovaEnhancements(wv, config, activeSpeed, activeLoop)
        } else if (isAnichinHost(wv.url)) {
            injectAnichinEnhancements(wv, config)
        }
    }

    val activeChannel = config.ui.activeChannel
    fun switchChannel(target: NovaChannel) {
        lastInteractionUptimeMs = SystemClock.uptimeMillis()
        if (activeChannel == target && webViewRef?.url?.contains(
                if (target == NovaChannel.YOUTUBE) "youtube.com" else "anichin.moe"
            ) == true
        ) {
            return
        }
        onUpdateUi { it.copy(activeChannel = target) }
        currentVideoId = ""
        isWatchPage = false
        isVideoPlaying = false
        sponsorSegments = emptyList()
        activeManualSegment = null
        onVideoPlayingStateChanged(false)
        webViewRef?.loadUrl(target.homeUrl)
        hudText = "Channel: ${target.displayName}"
    }

    // Keep foreground media service alive when background playback is enabled
    DisposableEffect(Unit) {
        val serviceIntent = Intent(context, NovaPlaybackService::class.java)
        runCatching { context.startService(serviceIntent) }
        onDispose { }
    }

    val isFullScreenOrLandscape = isInPipMode || isLandscape || customFullscreenView != null
    val themeStyle = config.ui.themeStyle
    val themeSurfaceColor = Color(themeStyle.surfaceHex)
    val themeCardColor = Color(themeStyle.cardHex)
    val themeAccentColor = Color(themeStyle.accentHex)
    val themeBorderColor = Color(themeStyle.borderHex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeSurfaceColor)
    ) {
        AndroidView(
            factory = { ctx ->
                NovaYouTubeWebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(AndroidColor.BLACK)
                    allowBackgroundPlay = config.playback.backgroundPlayEnabled
                    onUserTouch = {
                        lastInteractionUptimeMs = SystemClock.uptimeMillis()
                    }

                    val wv = this
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(wv, true)
                        // Enable YouTube native dark mode by default
                        setCookie("https://m.youtube.com", "PREF=f6=400; Domain=.youtube.com; Path=/")
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        cacheMode = WebSettings.LOAD_DEFAULT
                        // Let m.youtube.com use its native mobile viewport without landscape zoom/offset glitches
                        useWideViewPort = false
                        loadWithOverviewMode = false
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        setSupportMultipleWindows(false)
                        javaScriptCanOpenWindowsAutomatically = false
                        userAgentString =
                            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.6613.88 Mobile Safari/537.36"
                    }

                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onUrlChanged(url: String) {
                                val watch = url.contains("/watch") || url.contains("/shorts/")
                                isWatchPage = watch
                                if (!watch) {
                                    isVideoPlaying = false
                                    onVideoPlayingStateChanged(false)
                                }
                                post { fixViewportAlignment(wv) }
                                val vid = extractIdFromUrl(url)
                                if (vid != null && vid != currentVideoId) {
                                    currentVideoId = vid
                                    sponsorSegments = emptyList()
                                    activeManualSegment = null
                                    if (config.sponsorBlock.enabled) {
                                        onFetchSponsorSegments(vid) { segs ->
                                            sponsorSegments = segs
                                        }
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun onVideoProgress(positionSec: Double, durationSec: Double, playing: Boolean) {
                                isVideoPlaying = playing
                                onVideoPlayingStateChanged(playing)
                                if (!playing || !config.sponsorBlock.enabled || currentVideoId.isEmpty()) return
                                val posMs = (positionSec * 1000.0).toLong()
                                val seg = onCheckSponsor(sponsorSegments, posMs)
                                if (seg != null) {
                                    when (config.sponsorBlock.skipMode) {
                                        SkipMode.AUTO_SKIP -> {
                                            if (onMarkSponsorSkipped(currentVideoId, seg)) {
                                                val targetSec = seg.endMs / 1000.0
                                                post {
                                                    evaluateJavascript(
                                                        "(() => { const v = document.querySelector('video'); if (v) v.currentTime = $targetSec; })();",
                                                        null
                                                    )
                                                }
                                                if (config.sponsorBlock.showSkipToast) {
                                                    hudText = "Skipped ${seg.category.displayName}"
                                                }
                                            }
                                            activeManualSegment = null
                                        }
                                        SkipMode.MANUAL_BUTTON -> activeManualSegment = seg
                                        SkipMode.SHOW_ONLY -> activeManualSegment = null
                                    }
                                } else {
                                    activeManualSegment = null
                                }
                            }

                            @JavascriptInterface
                            fun onAnichinVisited(url: String, title: String, seriesUrl: String, episodeLabel: String) {
                                post {
                                    currentAnichinUrl = url
                                    currentAnichinTitle = title
                                    currentAnichinSeriesUrl = seriesUrl.ifBlank { url }
                                    currentAnichinEpisode = episodeLabel
                                    val path = runCatching { java.net.URI(url).path.orEmpty() }.getOrDefault("")
                                    val isContentPage = path.length > 1 &&
                                        !path.startsWith("/page/") &&
                                        !path.startsWith("/genre/") &&
                                        !path.startsWith("/tag/")
                                    if (isContentPage) {
                                        anichinRepo.recordHistory(url, title, episodeLabel)
                                    }
                                }
                            }
                        },
                        "NovaBridge"
                    )

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            if (config.ui.adBlockEnabled && BraveShieldsEngine.shouldBlockRequest(request)) {
                                return BraveShieldsEngine.emptyResponse()
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            // Never hijack iframe / sub-frame loads (fixes Google login & embedded players)
                            if (!request.isForMainFrame) return false
                            val scheme = request.url.scheme?.lowercase().orEmpty()
                            if (scheme != "http" && scheme != "https") return true

                            val host = request.url.host?.lowercase().orEmpty()
                            if (config.ui.adBlockEnabled && BraveShieldsEngine.isBlockedHost(host)) {
                                return true
                            }

                            // When on Anichin, bypass & block any external popunder/new-tab redirects
                            if (activeChannel == NovaChannel.ANICHIN || isAnichinHost(view.url)) {
                                return !isAnichinDomain(host)
                            }

                            return if (isGoogleOrYouTubeAuthOrContentHost(host) || isAnichinDomain(host)) {
                                false
                            } else {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                                }
                                true
                            }
                        }

                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            if (isYouTubeHost(url)) {
                                injectNovaEnhancements(view, config, activeSpeed, activeLoop)
                            } else if (isAnichinHost(url)) {
                                injectAnichinEnhancements(view, config)
                            }
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            super.onPageFinished(view, url)
                            if (isYouTubeHost(url)) {
                                injectNovaEnhancements(view, config, activeSpeed, activeLoop)
                                fixViewportAlignment(view)
                            } else if (isAnichinHost(url)) {
                                injectAnichinEnhancements(view, config)
                                fixViewportAlignment(view)
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            loadProgress = newProgress
                        }

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            customFullscreenView = view
                            customViewCallback = callback
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }

                        override fun onHideCustomView() {
                            (customFullscreenView?.parent as? ViewGroup)?.removeView(customFullscreenView)
                            customFullscreenView = null
                            customViewCallback?.onCustomViewHidden()
                            customViewCallback = null
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            fixViewportAlignment(wv)
                        }
                    }

                    loadUrl(config.ui.activeChannel.homeUrl)
                    webViewRef = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        )

        // Fullscreen Video Container when YouTube's fullscreen button is tapped
        customFullscreenView?.let { fsView ->
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        setBackgroundColor(AndroidColor.BLACK)
                        (fsView.parent as? ViewGroup)?.removeView(fsView)
                        addView(
                            fsView,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!isInPipMode) {
            if (loadProgress in 1..95 && !isFullScreenOrLandscape) {
                LinearProgressIndicator(
                    progress = { loadProgress / 100f },
                    color = NovaColors.AccentRed,
                    trackColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter)
                )
            }

            // iPhone AssistiveTouch-style Draggable Round Floating Button + Popup Menu
            if (!isFullScreenOrLandscape) {
                var isFloatingMenuOpen by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                val density = LocalDensity.current

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val btnSizeDp = 50.dp
                    val btnSizePx = with(density) { btnSizeDp.toPx() }
                    val edgeMarginPx = with(density) { 12.dp.toPx() }
                    val maxWidthPx = with(density) { maxWidth.toPx() }
                    val maxHeightPx = with(density) { maxHeight.toPx() }

                    val offsetX = remember(maxWidthPx) {
                        Animatable((maxWidthPx - btnSizePx - edgeMarginPx).coerceAtLeast(0f))
                    }
                    var offsetY by remember(maxHeightPx) {
                        mutableFloatStateOf((maxHeightPx * 0.72f).coerceIn(edgeMarginPx, (maxHeightPx - btnSizePx - edgeMarginPx).coerceAtLeast(edgeMarginPx)))
                    }

                    // Dimmed scrim when AssistiveTouch menu is expanded
                    if (isFloatingMenuOpen) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { isFloatingMenuOpen = false }
                        )
                    }

                    // Expanded AssistiveTouch Floating Menu Card
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isFloatingMenuOpen,
                        enter = fadeIn() + scaleIn(initialScale = 0.85f),
                        exit = fadeOut() + scaleOut(targetScale = 0.85f),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Column(
                            modifier = Modifier
                                .width(276.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(themeCardColor)
                                .border(1.dp, themeBorderColor, RoundedCornerShape(28.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.novatube_logo),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                )
                                Text(
                                    text = "Nova Menu",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Row 1: Channel Switcher (YouTube / Anichin) + Settings
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                AssistiveMenuButton(
                                    icon = Icons.Rounded.LiveTv,
                                    label = "YouTube",
                                    tint = if (activeChannel == NovaChannel.YOUTUBE) NovaColors.AccentRed else Color.White.copy(alpha = 0.7f),
                                    selected = activeChannel == NovaChannel.YOUTUBE
                                ) {
                                    isFloatingMenuOpen = false
                                    switchChannel(NovaChannel.YOUTUBE)
                                }
                                AssistiveMenuButton(
                                    icon = Icons.Rounded.LiveTv,
                                    label = "Anichin",
                                    tint = if (activeChannel == NovaChannel.ANICHIN) NovaColors.AccentCyan else Color.White.copy(alpha = 0.7f),
                                    selected = activeChannel == NovaChannel.ANICHIN
                                ) {
                                    isFloatingMenuOpen = false
                                    switchChannel(NovaChannel.ANICHIN)
                                }
                                AssistiveMenuButton(
                                    icon = Icons.Rounded.Settings,
                                    label = "Settings",
                                    tint = Color.White,
                                    selected = false
                                ) {
                                    isFloatingMenuOpen = false
                                    showSettingsSheet = true
                                }
                            }

                            // Row 2: Library (Favorit & History) + Update Check
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                if (activeChannel == NovaChannel.ANICHIN) {
                                    val targetSeries = currentAnichinSeriesUrl.ifBlank { webViewRef?.url.orEmpty() }
                                    val isFav = targetSeries.isNotBlank() && anichinRepo.isFavorite(targetSeries)
                                    AssistiveMenuButton(
                                        icon = Icons.Rounded.Star,
                                        label = if (isFav) "Favorited" else "Favorit",
                                        tint = if (isFav) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.75f),
                                        selected = isFav
                                    ) {
                                        isFloatingMenuOpen = false
                                        val pageUrl = currentAnichinUrl.ifBlank { webViewRef?.url.orEmpty() }
                                        val sUrl = currentAnichinSeriesUrl.ifBlank { pageUrl }
                                        val title = currentAnichinTitle.ifBlank { webViewRef?.title ?: "Donghua" }
                                        if (sUrl.isNotBlank()) {
                                            val added = anichinRepo.toggleFavorite(sUrl, title, currentAnichinEpisode, pageUrl)
                                            hudText = if (added) "Ditambahkan ke Favorit" else "Dihapus dari Favorit"
                                        }
                                    }
                                }
                                val hasDonghuaUpdate = anichinLibrary.favorites.any { it.hasUpdate }
                                AssistiveMenuButton(
                                    icon = Icons.Rounded.Bookmarks,
                                    label = if (hasDonghuaUpdate) "Library •" else "Library",
                                    tint = if (hasDonghuaUpdate) Color(0xFFFBBF24) else NovaColors.AccentCyan,
                                    selected = hasDonghuaUpdate
                                ) {
                                    isFloatingMenuOpen = false
                                    showAnichinLibrarySheet = true
                                }
                                AssistiveMenuButton(
                                    icon = Icons.Rounded.SystemUpdate,
                                    label = "Update",
                                    tint = themeAccentColor,
                                    selected = pendingUpdate != null
                                ) {
                                    isFloatingMenuOpen = false
                                    hudText = "Cek update..."
                                    updateScope.launch {
                                        val info = GitHubUpdater.checkForUpdate(BuildConfig.VERSION_NAME)
                                        if (info != null) {
                                            pendingUpdate = info
                                        } else {
                                            hudText = "Versi terbaru (v${BuildConfig.VERSION_NAME})"
                                        }
                                    }
                                }
                            }

                            // Row 3: Video Controls (when on Watch page)
                            if (isWatchPage) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    AssistiveMenuButton(
                                        icon = Icons.Rounded.Speed,
                                        label = "${activeSpeed}x",
                                        tint = NovaColors.AccentCyan,
                                        selected = activeSpeed != 1.0f
                                    ) {
                                        val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 0.75f)
                                        val nextIdx =
                                            (speeds.indexOfFirst { kotlin.math.abs(it - activeSpeed) < 0.05f } + 1) % speeds.size
                                        activeSpeed = speeds[nextIdx]
                                        webViewRef?.evaluateJavascript(
                                            "(() => { const v = document.querySelector('video'); if (v) v.playbackRate = $activeSpeed; })();",
                                            null
                                        )
                                        hudText = "Speed ${activeSpeed}x"
                                    }
                                    AssistiveMenuButton(
                                        icon = Icons.Rounded.Repeat,
                                        label = if (activeLoop) "Loop On" else "Loop",
                                        tint = if (activeLoop) NovaColors.AccentCyan else Color.White.copy(alpha = 0.75f),
                                        selected = activeLoop
                                    ) {
                                        activeLoop = !activeLoop
                                        webViewRef?.evaluateJavascript(
                                            "(() => { const v = document.querySelector('video'); if (v) v.loop = $activeLoop; })();",
                                            null
                                        )
                                        hudText = if (activeLoop) "Loop Enabled" else "Loop Off"
                                    }
                                    if (config.playback.pipUnlockEnabled && activity != null) {
                                        AssistiveMenuButton(
                                            icon = Icons.Rounded.PictureInPictureAlt,
                                            label = "PiP",
                                            tint = Color.White,
                                            selected = false
                                        ) {
                                            isFloatingMenuOpen = false
                                            runCatching { activity.enterPictureInPictureMode() }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Draggable Round AssistiveTouch Ball
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.roundToInt()) }
                            .size(btnSizeDp)
                            .alpha(if (pillVisible || isFloatingMenuOpen) 1f else 0.50f)
                            .clip(CircleShape)
                            .background(themeCardColor)
                            .border(1.5.dp, themeBorderColor, CircleShape)
                            .pointerInput(maxWidthPx, maxHeightPx) {
                                detectDragGestures(
                                    onDragStart = {
                                        lastInteractionUptimeMs = SystemClock.uptimeMillis()
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        lastInteractionUptimeMs = SystemClock.uptimeMillis()
                                        val nextX = (offsetX.value + dragAmount.x)
                                            .coerceIn(0f, (maxWidthPx - btnSizePx).coerceAtLeast(0f))
                                        offsetY = (offsetY + dragAmount.y)
                                            .coerceIn(edgeMarginPx, (maxHeightPx - btnSizePx - edgeMarginPx).coerceAtLeast(edgeMarginPx))
                                        scope.launch { offsetX.snapTo(nextX) }
                                    },
                                    onDragEnd = {
                                        val snapLeft = edgeMarginPx
                                        val snapRight = (maxWidthPx - btnSizePx - edgeMarginPx).coerceAtLeast(edgeMarginPx)
                                        val targetX = if (offsetX.value + btnSizePx / 2f < maxWidthPx / 2f) snapLeft else snapRight
                                        scope.launch { offsetX.animateTo(targetX) }
                                    }
                                )
                            }
                            .clickable {
                                lastInteractionUptimeMs = SystemClock.uptimeMillis()
                                isFloatingMenuOpen = !isFloatingMenuOpen
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                                .background(Color(0xFF000324)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.novatube_logo),
                                contentDescription = "Nova Floating Menu",
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
            }

            // Manual SponsorBlock Skip Pill
            activeManualSegment?.let { seg ->
                Button(
                    onClick = {
                        val targetSec = seg.endMs / 1000.0
                        webViewRef?.evaluateJavascript(
                            "(() => { const v = document.querySelector('video'); if (v) v.currentTime = $targetSec; })();",
                            null
                        )
                        onMarkSponsorSkipped(currentVideoId, seg)
                        activeManualSegment = null
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC16181E)),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp, bottom = 66.dp)
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Skip ${seg.category.displayName}", color = Color.White, fontSize = 12.sp)
                }
            }

            // Liquid Glass Toast HUD
            androidx.compose.animation.AnimatedVisibility(
                visible = hudText != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(themeCardColor)
                        .border(1.dp, themeBorderColor, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = hudText.orEmpty(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = themeSurfaceColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.novatube_logo),
                            contentDescription = "NovaTube Logo",
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = "NovaTube Settings",
                            color = NovaColors.TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { showSettingsSheet = false }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = NovaColors.TextSecondary)
                    }
                }
                // Channel Switcher Bar inside Nova Menu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Channel:",
                        color = NovaColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    NovaChannel.entries.forEach { channel ->
                        val selected = activeChannel == channel
                        FilterChip(
                            selected = selected,
                            onClick = {
                                switchChannel(channel)
                                showSettingsSheet = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.LiveTv,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = channel.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NovaColors.AccentCyan.copy(alpha = 0.22f),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = NovaColors.AccentCyan
                            )
                        )
                    }
                }
                SettingsTab(
                    config = config,
                    onUpdatePlayback = onUpdatePlayback,
                    onUpdateSponsorBlock = onUpdateSponsorBlock,
                    onToggleSponsorCategory = onToggleSponsorCategory,
                    onSetSkipMode = onSetSkipMode,
                    onUpdateUi = onUpdateUi,
                    onToggleTabVisibility = onToggleTabVisibility,
                    onMoveTab = onMoveTab,
                    onResetDefaults = onResetDefaults
                )
            }
        }
    }

    pendingUpdate?.let { update ->
        AlertDialog(
            onDismissRequest = { if (!isDownloadingUpdate) pendingUpdate = null },
            containerColor = themeCardColor,
            titleContentColor = Color.White,
            textContentColor = NovaColors.TextSecondary,
            title = {
                Text(
                    text = "Update Tersedia (${update.versionName})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = update.changelog, fontSize = 13.sp)
                    if (isDownloadingUpdate) {
                        LinearProgressIndicator(
                            progress = { updateDownloadProgress / 100f },
                            color = themeAccentColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Mengunduh update... $updateDownloadProgress%",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDownloadingUpdate,
                    onClick = {
                        isDownloadingUpdate = true
                        updateDownloadProgress = 0
                        updateScope.launch {
                            val apkFile = GitHubUpdater.downloadApk(context, update.downloadUrl) { pct ->
                                updateDownloadProgress = pct
                            }
                            isDownloadingUpdate = false
                            if (apkFile != null) {
                                pendingUpdate = null
                                GitHubUpdater.installApk(context, apkFile)
                            } else {
                                hudText = "Gagal mengunduh update"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeAccentColor,
                        contentColor = Color.Black
                    )
                ) {
                    Text(if (isDownloadingUpdate) "Downloading..." else "Download & Update", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!isDownloadingUpdate) {
                    TextButton(onClick = { pendingUpdate = null }) {
                        Text("Nanti", color = NovaColors.TextSecondary)
                    }
                }
            }
        )
    }

    if (showAnichinLibrarySheet) {
        ModalBottomSheet(
            onDismissRequest = { showAnichinLibrarySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = themeSurfaceColor
        ) {
            AnichinLibrarySheet(
                library = anichinLibrary,
                isCheckingUpdates = isCheckingAnichinUpdates,
                onCheckUpdates = {
                    isCheckingAnichinUpdates = true
                    updateScope.launch {
                        val (updatedAll, newlyUpdated) = AnichinUpdateChecker.checkUpdates(anichinLibrary.favorites)
                        isCheckingAnichinUpdates = false
                        if (newlyUpdated.isNotEmpty()) {
                            anichinRepo.applyDetectedUpdates(updatedAll)
                            AnichinUpdateChecker.showUpdateNotification(context, newlyUpdated)
                            hudText = "${newlyUpdated.size} donghua ada update!"
                        } else {
                            hudText = "Semua donghua favorit sudah terbaru"
                        }
                    }
                },
                onOpenUrl = { targetUrl ->
                    showAnichinLibrarySheet = false
                    if (activeChannel != NovaChannel.ANICHIN) {
                        onUpdateUi { it.copy(activeChannel = NovaChannel.ANICHIN) }
                    }
                    webViewRef?.loadUrl(targetUrl)
                },
                onMarkFavoriteSeen = anichinRepo::markUpdateSeen,
                onRemoveFavorite = anichinRepo::removeFavorite,
                onRemoveHistory = anichinRepo::removeHistory,
                onClearHistory = anichinRepo::clearHistory,
                onDismiss = { showAnichinLibrarySheet = false }
            )
        }
    }
}

@Composable
private fun AssistiveMenuButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

private fun extractIdFromUrl(url: String): String? {
    val vIndex = url.indexOf("v=")
    if (vIndex >= 0 && url.length >= vIndex + 13) {
        return url.substring(vIndex + 2, vIndex + 13)
    }
    val shortsIndex = url.indexOf("/shorts/")
    if (shortsIndex >= 0 && url.length >= shortsIndex + 19) {
        return url.substring(shortsIndex + 8, shortsIndex + 19)
    }
    return null
}

/**
 * Forces the WebView & YouTube DOM to re-align to the portrait/current viewport width
 * so minimizing a video, exiting fullscreen, or navigating back never shifts the layout.
 */
private fun fixViewportAlignment(webView: WebView) {
    if (webView.scrollX != 0) {
        webView.scrollTo(0, webView.scrollY)
    }
    webView.requestLayout()
    val resetJs = """
        (function() {
            if (!location.hostname.includes('youtube.com') && !location.hostname.includes('anichin.moe')) return;
            const applyFix = () => {
                let vp = document.querySelector('meta[name="viewport"]');
                if (!vp) {
                    vp = document.createElement('meta');
                    vp.name = 'viewport';
                    document.head && document.head.appendChild(vp);
                }
                vp.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';
                document.documentElement.style.overflowX = 'hidden';
                document.documentElement.style.maxWidth = '100vw';
                document.documentElement.scrollLeft = 0;
                if (document.body) {
                    document.body.style.overflowX = 'hidden';
                    document.body.style.maxWidth = '100vw';
                    document.body.style.margin = '0';
                    document.body.scrollLeft = 0;
                }
                if (window.scrollX !== 0) {
                    window.scrollTo(0, window.scrollY);
                }
                window.dispatchEvent(new Event('resize'));
            };
            applyFix();
            setTimeout(applyFix, 120);
            setTimeout(applyFix, 320);
        })();
    """.trimIndent()
    webView.evaluateJavascript(resetJs, null)
}

/**
 * Injects YTKACE / ReVanced-style enhancements + Brave Shields AdBlock into YouTube pages ONLY:
 * 1. Brave Shields JSON-prune scriptlet (strips adPlacements, playerAds, adSlots before player loads)
 * 2. Viewport lock CSS so minimizing/exiting a video never shifts ytm-app horizontally
 * 3. Background Playback unlock (overrides document.hidden / visibilitychange)
 * 4. Brave Cosmetic AdBlock CSS + fallback video ad fast-skipper
 * 5. OLED Pure Black CSS & UI Cleanup (hide Shorts shelf)
 * 6. SponsorBlock & Playback rate / loop bridge
 */
private fun injectNovaEnhancements(
    webView: WebView,
    config: NovaConfig,
    speed: Float,
    loop: Boolean
) {
    if (config.ui.adBlockEnabled) {
        webView.evaluateJavascript(BraveShieldsEngine.BRAVE_YOUTUBE_SCRIPTLET_JS, null)
    }

    val viewportLockCss = """
        html, body, ytm-app {
            overflow-x: hidden !important;
            max-width: 100vw !important;
            width: 100% !important;
            margin-left: 0 !important;
            margin-right: 0 !important;
            left: 0 !important;
        }
    """.trimIndent()

    val themeBg = config.ui.themeStyle.webBgHex
    val glassHeaderCss = if (config.ui.themeStyle == NovaThemeStyle.LIQUID_GLASS) {
        """
        .mobile-topbar-header, ytm-pivot-bar-renderer {
            background-color: rgba(12, 18, 36, 0.78) !important;
            backdrop-filter: blur(16px) !important;
            -webkit-backdrop-filter: blur(16px) !important;
            border-color: rgba(103, 232, 249, 0.25) !important;
        }
        """.trimIndent()
    } else ""

    val oledCss = """
        html, body, ytm-app, ytm-watch, ytm-SingleColumnWatchNextResultsRenderer,
        ytm-RichGridRenderer, .mobile-topbar-header, ytm-pivot-bar-renderer {
            background-color: $themeBg !important;
        }
        $glassHeaderCss
    """.trimIndent()

    val shortsCss = if (config.ui.hideShortsShelf) {
        """
        ytm-reel-shelf-renderer, ytm-shorts-lockup-view-model,
        ytm-pivot-bar-item-renderer:has(.pivot-shorts) {
            display: none !important;
        }
        """.trimIndent()
    } else ""

    val adBlockCss = if (config.ui.adBlockEnabled) {
        BraveShieldsEngine.BRAVE_COSMETIC_CSS.trimIndent()
    } else ""

    val combinedCss = (viewportLockCss + "\n" + oledCss + "\n" + shortsCss + "\n" + adBlockCss)
        .replace("\n", " ")
        .replace("\"", "\\\"")

    val js = """
        (function() {
            if (!location.hostname.includes('youtube.com')) return;

            // 1. Inject / Update NovaTube CSS
            let style = document.getElementById('novatube-style');
            if (!style) {
                style = document.createElement('style');
                style.id = 'novatube-style';
                document.head && document.head.appendChild(style);
            }
            if (style) {
                style.textContent = "$combinedCss";
            }

            // 2. Background Playback Unlock
            if (${config.playback.backgroundPlayEnabled} && !window.__novaBgHooked) {
                window.__novaBgHooked = true;
                try {
                    Object.defineProperty(document, 'hidden', { get: () => false, configurable: true });
                    Object.defineProperty(document, 'visibilityState', { get: () => 'visible', configurable: true });
                    window.addEventListener('visibilitychange', e => e.stopImmediatePropagation(), true);
                    window.addEventListener('webkitvisibilitychange', e => e.stopImmediatePropagation(), true);
                } catch (e) {}
            }

            // 3. Apply Speed & Loop ONLY on /watch or /shorts pages
            const isWatch = location.pathname.startsWith('/watch') || location.pathname.startsWith('/shorts');
            if (isWatch) {
                const v = document.querySelector('video');
                if (v && !document.querySelector('.ad-showing')) {
                    if (Math.abs(v.playbackRate - $speed) > 0.01) {
                        v.playbackRate = $speed;
                    }
                    v.loop = $loop;
                }
            }

            // 4. Periodic monitor for AdBlock, URL changes, and SponsorBlock progress
            if (!window.__novaLoopStarted) {
                window.__novaLoopStarted = true;
                let lastUrl = '';
                setInterval(() => {
                    if (!location.hostname.includes('youtube.com')) return;

                    if (location.href !== lastUrl) {
                        lastUrl = location.href;
                        if (window.NovaBridge) window.NovaBridge.onUrlChanged(lastUrl);
                    }

                    const onWatchPage = location.pathname.startsWith('/watch') || location.pathname.startsWith('/shorts');

                    // AdBlock: Auto-skip video ads on watch page
                    if (${config.ui.adBlockEnabled} && onWatchPage) {
                        const skipBtn = document.querySelector(
                            '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .ytp-ad-overlay-close-button'
                        );
                        if (skipBtn) skipBtn.click();

                        const adShowing = document.querySelector('.ad-showing, .ad-interrupting');
                        const adVideo = adShowing ? adShowing.querySelector('video') : null;
                        if (adVideo) {
                            adVideo.muted = true;
                            if (isFinite(adVideo.duration) && adVideo.duration > 0) {
                                adVideo.currentTime = adVideo.duration;
                            } else {
                                adVideo.playbackRate = 16.0;
                            }
                        }
                    }

                    if (onWatchPage) {
                        const video = document.querySelector('video');
                        if (video && window.NovaBridge && !document.querySelector('.ad-showing')) {
                            window.NovaBridge.onVideoProgress(
                                video.currentTime || 0,
                                video.duration || 0,
                                !video.paused
                            );
                        }
                    }
                }, 250);
            }
        })();
    """.trimIndent()

    webView.evaluateJavascript(js, null)
}

/**
 * Injects AdBlock, popup blocking, background playback unlock, and viewport lock
 * when the user switches to the Anichin channel (https://anichin.moe/).
 */
private fun injectAnichinEnhancements(
    webView: WebView,
    config: NovaConfig
) {
    val adCss = if (config.ui.adBlockEnabled) {
        """
        .ads, .ad-banner, .banner-ads, .iklan, .float-ads, .floating-ads,
        [class*="iklan"], [id*="iklan"], [class*="ad-slot"], [id*="ad-slot"],
        iframe[src*="doubleclick"], iframe[src*="googlesyndication"],
        iframe[src*="popads"], iframe[src*="adsterra"] {
            display: none !important;
        }
        """.trimIndent()
    } else ""

    val themeBg = config.ui.themeStyle.webBgHex
    val viewportCss = """
        html, body {
            overflow-x: hidden !important;
            max-width: 100vw !important;
            background-color: $themeBg !important;
        }
    """.trimIndent()

    val combinedCss = (viewportCss + "\n" + adCss)
        .replace("\n", " ")
        .replace("\"", "\\\"")

    val js = """
        (function() {
            if (!location.hostname.includes('anichin.moe')) return;

            let style = document.getElementById('novatube-anichin-style');
            if (!style) {
                style = document.createElement('style');
                style.id = 'novatube-anichin-style';
                document.head && document.head.appendChild(style);
            }
            if (style) {
                style.textContent = "$combinedCss";
            }

            if (${config.ui.adBlockEnabled} && !window.__novaPopupBlocked) {
                window.__novaPopupBlocked = true;
                window.open = function() { return null; };

                const origAnchorClick = HTMLAnchorElement.prototype.click;
                HTMLAnchorElement.prototype.click = function() {
                    try {
                        if (this.hostname && !this.hostname.includes('anichin.moe')) {
                            return;
                        }
                    } catch (e) {}
                    return origAnchorClick.apply(this, arguments);
                };

                document.addEventListener('click', function(e) {
                    const a = e.target && e.target.closest ? e.target.closest('a') : null;
                    if (a && a.hostname && !a.hostname.includes('anichin.moe')) {
                        e.preventDefault();
                        e.stopImmediatePropagation();
                        return false;
                    }
                    if (a && a.target === '_blank') {
                        a.removeAttribute('target');
                    }
                }, true);
            }

            if (${config.playback.backgroundPlayEnabled} && !window.__novaBgHooked) {
                window.__novaBgHooked = true;
                try {
                    Object.defineProperty(document, 'hidden', { get: () => false, configurable: true });
                    Object.defineProperty(document, 'visibilityState', { get: () => 'visible', configurable: true });
                    window.addEventListener('visibilitychange', e => e.stopImmediatePropagation(), true);
                } catch (e) {}
            }

            // Record current Anichin page for History & Favorite Series tracking
            if (window.NovaBridge && window.NovaBridge.onAnichinVisited) {
                const h1 = document.querySelector('h1.entry-title, h1');
                const rawTitle = (h1 && h1.textContent ? h1.textContent : document.title) || 'Anichin';
                const cleanTitle = rawTitle.replace(/\s*-\s*Anichin.*$/i, '').trim();
                const seriesLink = document.querySelector('.breadcrumb a[href*="/seri/"], .allc a[href*="/seri/"], a[href*="/seri/"]');
                const seriesUrl = seriesLink ? seriesLink.href : location.href;
                const epMatch = cleanTitle.match(/Episode\s*\d+/i);
                const epLabel = epMatch ? epMatch[0] : '';
                window.NovaBridge.onAnichinVisited(location.href, cleanTitle, seriesUrl, epLabel);
            }
        })();
    """.trimIndent()

    webView.evaluateJavascript(js, null)
}
