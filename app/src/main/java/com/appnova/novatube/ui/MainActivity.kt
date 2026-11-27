package com.appnova.novatube.ui

import android.Manifest
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.appnova.novatube.ui.screens.YouTubeCloneScreen
import com.appnova.novatube.ui.theme.NovaTubeTheme

class MainActivity : ComponentActivity() {

    private val novaViewModel: NovaViewModel by viewModels()
    private var inPipMode by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            val config by novaViewModel.config.collectAsState()
            NovaTubeTheme(themeStyle = config.ui.themeStyle) {
                YouTubeCloneScreen(
                    config = config,
                    isInPipMode = inPipMode,
                    onFetchSponsorSegments = novaViewModel::fetchSponsorSegmentsForClone,
                    onCheckSponsor = novaViewModel::checkSponsorInList,
                    onMarkSponsorSkipped = novaViewModel::markSponsorSkipped,
                    onVideoPlayingStateChanged = novaViewModel::setWebVideoPlaying,
                    onUpdatePlayback = novaViewModel::updatePlayback,
                    onUpdateSponsorBlock = novaViewModel::updateSponsorBlock,
                    onToggleSponsorCategory = novaViewModel::toggleSponsorCategory,
                    onSetSkipMode = novaViewModel::setSkipMode,
                    onUpdateUi = novaViewModel::updateUi,
                    onToggleTabVisibility = novaViewModel::toggleTabVisibility,
                    onMoveTab = novaViewModel::moveTab,
                    onResetDefaults = novaViewModel::resetDefaults
                )
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val cfg = novaViewModel.config.value
        val isPlaying = novaViewModel.isWebVideoPlaying.value || novaViewModel.player.value?.isPlaying == true
        if (cfg.playback.pipUnlockEnabled && isPlaying) {
            enterNovaPip(this)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPipMode = isInPictureInPictureMode
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    companion object {
        fun enterNovaPip(activity: Activity): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                return runCatching { activity.enterPictureInPictureMode(params) }.getOrDefault(false)
            }
            return false
        }
    }
}
