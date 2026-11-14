package com.appnova.novatube.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.appnova.novatube.config.NovaConfig
import com.appnova.novatube.config.SkipMode
import com.appnova.novatube.sponsorblock.SponsorSegment
import com.appnova.novatube.ui.components.GlassCard
import com.appnova.novatube.ui.components.VideoCard
import com.appnova.novatube.ui.theme.NovaColors
import com.appnova.novatube.youtube.QualityOption
import com.appnova.novatube.youtube.StreamDetails
import com.appnova.novatube.youtube.VideoSummary
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WatchPlayerScreen(
    stream: StreamDetails,
    player: Player?,
    config: NovaConfig,
    segments: List<SponsorSegment>,
    bookmarks: List<VideoSummary>,
    selectedQuality: QualityOption?,
    onBack: () -> Unit,
    onSelectRelated: (VideoSummary) -> Unit,
    onSwitchQuality: (QualityOption) -> Unit,
    onToggleBookmark: (VideoSummary) -> Unit,
    onCheckSponsor: (Long) -> SponsorSegment?,
    onMarkSponsorSkipped: (String, SponsorSegment) -> Boolean
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    var controlsVisible by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showDescription by remember { mutableStateOf(false) }

    var hudText by remember { mutableStateOf<String?>(null) }
    var activeManualSegment by remember { mutableStateOf<SponsorSegment?>(null) }
    var isPlaying by remember { mutableStateOf(player?.isPlaying == true) }
    var isBuffering by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember(stream.videoId) { mutableLongStateOf(stream.durationMs) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPreviewMs by remember { mutableLongStateOf(0L) }

    var currentSpeed by remember(stream.videoId) { mutableFloatStateOf(config.playback.defaultSpeed) }
    var isLooping by remember(stream.videoId) { mutableStateOf(config.playback.loopByDefault) }

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onBack()
        }
    }

    // Handle orientation & system bars for Fullscreen mode
    DisposableEffect(isFullscreen) {
        if (activity != null) {
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            if (isFullscreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (activity != null && isFullscreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Auto-hide controls after 3.5s while playing
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3500L)
            controlsVisible = false
        }
    }

    // Poll playback state & SponsorBlock segments
    LaunchedEffect(player, stream.videoId, config.sponsorBlock) {
        while (true) {
            val p = player
            if (p != null) {
                isPlaying = p.isPlaying
                isBuffering = p.playbackState == Player.STATE_BUFFERING
                val dur = p.duration
                if (dur > 0) totalDurationMs = dur
                if (!isSeeking) {
                    currentPosMs = p.currentPosition.coerceAtLeast(0L)
                }
                if (p.isPlaying) {
                    val seg = onCheckSponsor(currentPosMs)
                    if (seg != null) {
                        when (config.sponsorBlock.skipMode) {
                            SkipMode.AUTO_SKIP -> {
                                if (onMarkSponsorSkipped(stream.videoId, seg)) {
                                    p.seekTo(seg.endMs)
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
            }
            delay(300L)
        }
    }

    LaunchedEffect(hudText) {
        if (hudText != null) {
            delay(1300L)
            hudText = null
        }
    }

    val isBookmarked = bookmarks.any { it.id == stream.videoId }
    val videoAsSummary = remember(stream) {
        VideoSummary(
            id = stream.videoId,
            title = stream.title,
            channelName = stream.channelName,
            thumbnailUrl = stream.thumbnailUrl,
            durationSeconds = stream.durationMs / 1000L,
            viewCount = 0L
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (config.ui.oledModeEnabled) NovaColors.OledBlack else Color(0xFF0E1015))
            .then(if (!isFullscreen) Modifier.statusBarsPadding() else Modifier)
    ) {
        // Video Player + Custom YouTube-style Interactive GUI
        Box(
            modifier = if (isFullscreen) {
                Modifier.fillMaxSize().background(Color.Black)
            } else {
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)
            }
        ) {
            if (player != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                        }
                    },
                    update = { it.player = player },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Gesture Layer (Tap to toggle GUI, Double-tap to Seek, Vertical swipe for Brightness/Volume)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(config.playback, player) {
                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                            },
                            onDoubleTap = { offset ->
                                if (player == null || !config.playback.swipeSeekEnabled) return@detectTapGestures
                                val isRight = offset.x > size.width * 0.5f
                                val stepMs = config.playback.seekStepSeconds * 1000L
                                val target = if (isRight) {
                                    (player.currentPosition + stepMs).coerceAtMost(totalDurationMs.coerceAtLeast(0L))
                                } else {
                                    (player.currentPosition - stepMs).coerceAtLeast(0L)
                                }
                                player.seekTo(target)
                                currentPosMs = target
                                hudText = if (isRight) "+${config.playback.seekStepSeconds}s" else "-${config.playback.seekStepSeconds}s"
                            }
                        )
                    }
                    .pointerInput(config.playback) {
                        if (!config.playback.gesturesEnabled) return@pointerInput
                        detectVerticalDragGestures { change, dragAmount ->
                            val isLeft = change.position.x < size.width * 0.45f
                            val isRight = change.position.x > size.width * 0.55f
                            if (isLeft && config.playback.brightnessGestureEnabled && activity != null) {
                                val lp = activity.window.attributes
                                val cur = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                                val next = (cur - dragAmount / 600f).coerceIn(0.05f, 1f)
                                lp.screenBrightness = next
                                activity.window.attributes = lp
                                hudText = "Brightness ${(next * 100).toInt()}%"
                            } else if (isRight && config.playback.volumeGestureEnabled && audioManager != null) {
                                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val delta = if (dragAmount < -12f) 1 else if (dragAmount > 12f) -1 else 0
                                if (delta != 0) {
                                    val next = (cur + delta).coerceIn(0, max)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
                                    hudText = "Volume ${((next.toFloat() / max) * 100).toInt()}%"
                                }
                            }
                        }
                    }
            )

            // YouTube-style Interactive Controls Overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    // Top Controls Row (Minimize, Title, Quality, PiP)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (isFullscreen) isFullscreen = false else onBack()
                        }) {
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Minimize",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        if (isFullscreen) {
                            Text(
                                text = stream.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        if (stream.qualityOptions.isNotEmpty()) {
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .clickable { showQualityMenu = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = selectedQuality?.label ?: "Auto",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                DropdownMenu(
                                    expanded = showQualityMenu,
                                    onDismissRequest = { showQualityMenu = false }
                                ) {
                                    stream.qualityOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (opt.label == selectedQuality?.label) "${opt.label} ✓" else opt.label,
                                                    fontWeight = if (opt.label == selectedQuality?.label) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                showQualityMenu = false
                                                hudText = "Quality: ${opt.label}"
                                                onSwitchQuality(opt)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (config.playback.pipUnlockEnabled && activity != null) {
                            IconButton(onClick = { runCatching { activity.enterPictureInPictureMode() } }) {
                                Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = "PiP", tint = Color.White)
                            }
                        }
                    }

                    // Center Play / Pause / Seek Buttons
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                player?.let {
                                    val target = (it.currentPosition - 10_000L).coerceAtLeast(0L)
                                    it.seekTo(target)
                                    currentPosMs = target
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Icon(Icons.Rounded.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        IconButton(
                            onClick = {
                                player?.let {
                                    if (it.isPlaying) it.pause() else it.play()
                                    isPlaying = !isPlaying
                                }
                            },
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                player?.let {
                                    val target = (it.currentPosition + 10_000L).coerceAtMost(totalDurationMs.coerceAtLeast(0L))
                                    it.seekTo(target)
                                    currentPosMs = target
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Icon(Icons.Rounded.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }

                    // Bottom Bar: Time, Interactive Seekbar, Fullscreen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${formatTime(if (isSeeking) seekPreviewMs else currentPosMs)} / ${formatTime(totalDurationMs)}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(
                                onClick = { isFullscreen = !isFullscreen },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }

                        val displayPos = (if (isSeeking) seekPreviewMs else currentPosMs).toFloat()
                        val safeDuration = totalDurationMs.coerceAtLeast(1000L).toFloat()
                        Slider(
                            value = displayPos.coerceIn(0f, safeDuration),
                            onValueChange = { v ->
                                isSeeking = true
                                seekPreviewMs = v.toLong()
                            },
                            onValueChangeFinished = {
                                player?.seekTo(seekPreviewMs)
                                currentPosMs = seekPreviewMs
                                isSeeking = false
                            },
                            valueRange = 0f..safeDuration,
                            colors = SliderDefaults.colors(
                                thumbColor = NovaColors.AccentRed,
                                activeTrackColor = NovaColors.AccentRed,
                                inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth().height(24.dp)
                        )
                    }
                }
            }

            // Liquid Glass Gesture / Skip Toast HUD
            hudText?.let { msg ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (config.ui.liquidGlassHudEnabled) Color(0xB016181E) else Color(0xEB0C0C0E)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(text = msg, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Manual SponsorBlock Skip Pill
            activeManualSegment?.let { seg ->
                Button(
                    onClick = {
                        player?.seekTo(seg.endMs)
                        onMarkSponsorSkipped(stream.videoId, seg)
                        activeManualSegment = null
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC16181E)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 52.dp)
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Skip ${seg.category.displayName}", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        if (isFullscreen) return

        // SponsorBlock Timeline Marker Bar
        if (config.sponsorBlock.enabled && config.sponsorBlock.showSeekbarMarkers && totalDurationMs > 0) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0xFF1F232E))
            ) {
                val total = totalDurationMs.toFloat().coerceAtLeast(1f)
                val progressW = (currentPosMs / total).coerceIn(0f, 1f) * size.width
                drawRect(color = NovaColors.AccentRed, size = Size(progressW, size.height))
                segments.forEach { seg ->
                    val startX = (seg.startMs / total).coerceIn(0f, 1f) * size.width
                    val segW = ((seg.endMs - seg.startMs) / total).coerceIn(0f, 1f) * size.width
                    drawRect(
                        color = Color(seg.category.defaultColorHex),
                        topLeft = Offset(startX, 0f),
                        size = Size(segW.coerceAtLeast(4f), size.height)
                    )
                }
            }
        }

        // Video Info, YouTube Action Chips, Description & Related Videos
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard {
                    Text(
                        text = stream.title,
                        color = NovaColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NovaColors.AccentRed.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stream.channelName.firstOrNull()?.uppercase() ?: "N",
                                    color = NovaColors.AccentRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stream.channelName,
                                color = NovaColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row {
                            IconButton(onClick = { onToggleBookmark(videoAsSummary) }) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                    contentDescription = "Save",
                                    tint = if (isBookmarked) NovaColors.AccentRed else NovaColors.TextPrimary
                                )
                            }
                            IconButton(onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, "https://youtu.be/${stream.videoId}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share video"))
                            }) {
                                Icon(Icons.Rounded.Share, contentDescription = "Share", tint = NovaColors.TextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val speeds = if (config.playback.customSpeedOptionsEnabled) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
                        } else {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        }
                        speeds.forEach { speed ->
                            FilterChip(
                                selected = kotlin.math.abs(currentSpeed - speed) < 0.02f,
                                onClick = {
                                    currentSpeed = speed
                                    player?.setPlaybackSpeed(speed)
                                },
                                label = { Text("${speed}x", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NovaColors.AccentRed.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        FilterChip(
                            selected = isLooping,
                            onClick = {
                                isLooping = !isLooping
                                player?.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                            },
                            leadingIcon = { Icon(Icons.Rounded.Repeat, contentDescription = null) },
                            label = { Text("Loop", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NovaColors.AccentCyan.copy(alpha = 0.25f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    if (stream.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (showDescription) stream.description else "Tap to show description",
                            color = NovaColors.TextSecondary,
                            fontSize = 12.sp,
                            maxLines = if (showDescription) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDescription = !showDescription }
                        )
                    }
                }
            }

            if (stream.relatedVideos.isNotEmpty()) {
                item {
                    Text(
                        text = "Up Next",
                        color = NovaColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(stream.relatedVideos, key = { it.id }) { related ->
                    VideoCard(
                        video = related,
                        isBookmarked = bookmarks.any { it.id == related.id },
                        onClick = { onSelectRelated(related) },
                        onToggleBookmark = { onToggleBookmark(related) }
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}
