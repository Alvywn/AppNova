package com.appnova.novatube.ui

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.appnova.novatube.config.NovaConfig
import com.appnova.novatube.config.NovaConfigRepository
import com.appnova.novatube.config.PlaybackConfig
import com.appnova.novatube.config.SkipMode
import com.appnova.novatube.config.SponsorBlockConfig
import com.appnova.novatube.config.SponsorCategory
import com.appnova.novatube.config.UiConfig
import com.appnova.novatube.player.NovaPlaybackService
import com.appnova.novatube.player.NovaPlayerHolder
import com.appnova.novatube.sponsorblock.SponsorBlockClient
import com.appnova.novatube.sponsorblock.SponsorSegment
import com.appnova.novatube.youtube.QualityOption
import com.appnova.novatube.youtube.StreamDetails
import com.appnova.novatube.youtube.VideoSummary
import com.appnova.novatube.youtube.YouTubeRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NovaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NovaConfigRepository(application)
    private val ytRepo = YouTubeRepository()
    private val sponsorClient = SponsorBlockClient()

    val config: StateFlow<NovaConfig> = repository.config

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _trendingVideos = MutableStateFlow<List<VideoSummary>>(emptyList())
    val trendingVideos: StateFlow<List<VideoSummary>> = _trendingVideos.asStateFlow()

    private val _isLoadingFeed = MutableStateFlow(false)
    val isLoadingFeed: StateFlow<Boolean> = _isLoadingFeed.asStateFlow()

    private val _feedError = MutableStateFlow<String?>(null)
    val feedError: StateFlow<String?> = _feedError.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<VideoSummary>>(emptyList())
    val searchResults: StateFlow<List<VideoSummary>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<VideoSummary>>(emptyList())
    val bookmarks: StateFlow<List<VideoSummary>> = _bookmarks.asStateFlow()

    private val _activeStream = MutableStateFlow<StreamDetails?>(null)
    val activeStream: StateFlow<StreamDetails?> = _activeStream.asStateFlow()

    private val _isPlayerMinimized = MutableStateFlow(false)
    val isPlayerMinimized: StateFlow<Boolean> = _isPlayerMinimized.asStateFlow()

    private val _selectedQuality = MutableStateFlow<QualityOption?>(null)
    val selectedQuality: StateFlow<QualityOption?> = _selectedQuality.asStateFlow()

    private val _isLoadingStream = MutableStateFlow(false)
    val isLoadingStream: StateFlow<Boolean> = _isLoadingStream.asStateFlow()

    private val _streamError = MutableStateFlow<String?>(null)
    val streamError: StateFlow<String?> = _streamError.asStateFlow()

    private val _sponsorSegments = MutableStateFlow<List<SponsorSegment>>(emptyList())
    val sponsorSegments: StateFlow<List<SponsorSegment>> = _sponsorSegments.asStateFlow()

    private val _player = MutableStateFlow<Player?>(NovaPlayerHolder.getOrCreate(application))
    val player: StateFlow<Player?> = _player.asStateFlow()

    private val _isWebVideoPlaying = MutableStateFlow(false)
    val isWebVideoPlaying: StateFlow<Boolean> = _isWebVideoPlaying.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null

    init {
        connectToPlaybackService()
        loadTrending()
    }

    private fun connectToPlaybackService() {
        val ctx = getApplication<Application>()
        val token = SessionToken(ctx, ComponentName(ctx, NovaPlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                // Keep ExoPlayer from NovaPlayerHolder for direct MergingMediaSource control;
                // MediaSessionService keeps the foreground notification & background audio alive.
                runCatching { future.get() }
            },
            MoreExecutors.directExecutor()
        )
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        if (category == "All") {
            loadTrending()
        } else {
            viewModelScope.launch {
                _isLoadingFeed.value = true
                _feedError.value = null
                ytRepo.searchVideos(category)
                    .onSuccess { _trendingVideos.value = it }
                    .onFailure { _feedError.value = it.message ?: "Failed to load $category" }
                _isLoadingFeed.value = false
            }
        }
    }

    fun loadTrending() {
        viewModelScope.launch {
            _isLoadingFeed.value = true
            _feedError.value = null
            ytRepo.fetchTrending()
                .onSuccess { _trendingVideos.value = it }
                .onFailure { _feedError.value = it.message ?: "Failed to load feed" }
            _isLoadingFeed.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun performSearch(query: String = _searchQuery.value) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSearching.value = true
            ytRepo.searchVideos(query)
                .onSuccess { _searchResults.value = it }
            _isSearching.value = false
        }
    }

    fun toggleBookmark(video: VideoSummary) {
        val current = _bookmarks.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.id == video.id }
        if (existingIdx >= 0) {
            current.removeAt(existingIdx)
        } else {
            current.add(0, video)
        }
        _bookmarks.value = current
    }

    fun playVideo(video: VideoSummary) {
        viewModelScope.launch {
            _isLoadingStream.value = true
            _streamError.value = null
            _sponsorSegments.value = emptyList()
            sponsorClient.clearSessionForVideo(video.id)

            val sbConfig = config.value.sponsorBlock
            if (sbConfig.enabled) {
                sponsorClient.fetchSegmentsAsync(video.id, sbConfig.enabledCategories) { segments ->
                    _sponsorSegments.value = segments
                }
            }

            ytRepo.loadStreamDetails(video.id)
                .onSuccess { details ->
                    _activeStream.value = details
                    _isPlayerMinimized.value = false
                    // Prefer 360p (Fast) muxed stream if available for instant startup,
                    // or 720p adaptive stream.
                    val defaultQuality = details.qualityOptions.firstOrNull { it.isMuxed }
                        ?: details.qualityOptions.firstOrNull { it.height <= 720 }
                        ?: details.qualityOptions.firstOrNull()
                    _selectedQuality.value = defaultQuality
                    NovaPlayerHolder.playStream(
                        context = getApplication(),
                        details = details,
                        selectedVideoUrl = defaultQuality?.url,
                        isMuxedStream = defaultQuality?.isMuxed == true,
                        speed = config.value.playback.defaultSpeed,
                        loop = config.value.playback.loopByDefault
                    )
                }
                .onFailure { err ->
                    _streamError.value = err.message ?: "Failed to load video stream"
                }
            _isLoadingStream.value = false
        }
    }

    fun switchQuality(option: QualityOption) {
        val details = _activeStream.value ?: return
        val currentPos = (_player.value?.currentPosition ?: 0L).coerceAtLeast(0L)
        _selectedQuality.value = option
        NovaPlayerHolder.playStream(
            context = getApplication(),
            details = details,
            selectedVideoUrl = option.url,
            isMuxedStream = option.isMuxed,
            speed = _player.value?.playbackParameters?.speed ?: config.value.playback.defaultSpeed,
            loop = _player.value?.repeatMode == Player.REPEAT_MODE_ONE,
            startPositionMs = currentPos
        )
    }

    fun minimizePlayer() {
        _isPlayerMinimized.value = true
        if (!config.value.playback.backgroundPlayEnabled) {
            _player.value?.pause()
        }
    }

    fun expandPlayer() {
        _isPlayerMinimized.value = false
    }

    fun dismissMiniPlayer() {
        _player.value?.pause()
        _activeStream.value = null
        _isPlayerMinimized.value = false
    }

    fun checkSponsorSegmentAt(positionMs: Long): SponsorSegment? {
        val sb = config.value.sponsorBlock
        if (!sb.enabled) return null
        return sponsorClient.findActiveSegment(_sponsorSegments.value, positionMs)
    }

    fun fetchSponsorSegmentsForClone(videoId: String, onResult: (List<SponsorSegment>) -> Unit) {
        sponsorClient.clearSessionForVideo(videoId)
        val sb = config.value.sponsorBlock
        if (!sb.enabled) {
            onResult(emptyList())
            return
        }
        sponsorClient.fetchSegmentsAsync(videoId, sb.enabledCategories, onResult)
    }

    fun checkSponsorInList(segments: List<SponsorSegment>, positionMs: Long): SponsorSegment? {
        if (!config.value.sponsorBlock.enabled) return null
        return sponsorClient.findActiveSegment(segments, positionMs)
    }

    fun setWebVideoPlaying(playing: Boolean) {
        _isWebVideoPlaying.value = playing
    }

    fun markSponsorSkipped(videoId: String, segment: SponsorSegment): Boolean =
        sponsorClient.markSkipped(videoId, segment)

    fun updatePlayback(transform: (PlaybackConfig) -> PlaybackConfig) {
        repository.update { it.copy(playback = transform(it.playback)) }
    }

    fun updateSponsorBlock(transform: (SponsorBlockConfig) -> SponsorBlockConfig) {
        repository.update { it.copy(sponsorBlock = transform(it.sponsorBlock)) }
    }

    fun toggleSponsorCategory(category: SponsorCategory, enabled: Boolean) {
        updateSponsorBlock { sb ->
            val updated = if (enabled) sb.enabledCategories + category else sb.enabledCategories - category
            sb.copy(enabledCategories = updated)
        }
    }

    fun setSkipMode(mode: SkipMode) {
        updateSponsorBlock { it.copy(skipMode = mode) }
    }

    fun updateUi(transform: (UiConfig) -> UiConfig) {
        repository.update { it.copy(ui = transform(it.ui)) }
    }

    fun toggleTabVisibility(tabId: String) {
        if (tabId == "settings") return
        updateUi { ui ->
            val updatedTabs = ui.tabs.map { tab ->
                if (tab.id == tabId) tab.copy(visible = !tab.visible) else tab
            }
            ui.copy(tabs = updatedTabs)
        }
    }

    fun moveTab(tabId: String, direction: Int) {
        updateUi { ui ->
            val list = ui.tabs.toMutableList()
            val idx = list.indexOfFirst { it.id == tabId }
            val targetIdx = idx + direction
            if (idx in list.indices && targetIdx in list.indices) {
                val item = list.removeAt(idx)
                list.add(targetIdx, item)
            }
            ui.copy(tabs = list)
        }
    }

    fun resetDefaults() {
        repository.resetToDefaults()
    }

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
