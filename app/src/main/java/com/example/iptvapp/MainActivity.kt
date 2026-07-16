package com.example.iptvapp

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.ui.PlayerView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.iptvapp.core.playback.PlaybackDiagnosticsStore
import com.example.iptvapp.core.playback.IptvPlayerFactory
import com.example.iptvapp.core.playback.LiveStreamFormat
import com.example.iptvapp.core.playback.PlaybackMetrics
import com.example.iptvapp.core.playback.PlaybackTelemetryRecorder
import com.example.iptvapp.core.playback.PlaybackTelemetrySnapshot
import com.example.iptvapp.core.playback.codecLabel
import com.example.iptvapp.core.playback.formatBitrate
import com.example.iptvapp.core.playback.formatPlaybackDuration
import com.example.iptvapp.core.playback.playbackStateLabel
import com.example.iptvapp.core.playback.resolutionLabel
import com.example.iptvapp.core.playback.resolveLiveOffset
import com.example.iptvapp.core.playback.supportsLiveStreamFormatSwitch
import com.example.iptvapp.data.model.Channel
import com.example.iptvapp.data.model.CountryGroupFilter
import com.example.iptvapp.data.model.GuideProgram
import com.example.iptvapp.data.model.GuideProgramBlock
import com.example.iptvapp.data.model.IptvPlaylist
import com.example.iptvapp.data.model.availableCountryGroupFilters
import com.example.iptvapp.data.model.matchesSelectedCountryFilters
import com.example.iptvapp.data.model.isCurrentLiveSportsTitle
import com.example.iptvapp.ui.ConnectionTestState
import com.example.iptvapp.ui.GuideLoadState
import com.example.iptvapp.ui.MainViewModel
import com.example.iptvapp.ui.PlaylistSaveState
import com.example.iptvapp.ui.PlaylistRefreshState
import com.example.iptvapp.ui.theme.IPTVAppTheme
import com.example.iptvapp.sync.EpgSyncStatus
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private enum class AppScreen(val title: String) {
    Live("Live TV"),
    Search("Search"),
    Playlists("Playlists"),
    Settings("Settings")
}

private val AppBackground = Color(0xFF050A0F)
private val Panel = Color(0xFF101722)
private val PanelSoft = Color(0xFF151D2A)
private val Border = Color(0xFF273244)
private val Accent = Color(0xFF5364FF)
private val AccentAlt = Color(0xFF32D2C8)
private val TextPrimary = Color(0xFFF8FAFF)
private val TextSecondary = Color(0xFFB8C1D4)
private val TextMuted = Color(0xFF7D879A)
private val Success = Color(0xFF36D37E)
private val TimeSlotFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val DaySlotFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d")
private val ProgramDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
private const val TimelineDays = 7
private const val TimelineSlotsPerDay = 48
private const val TimelineCellCount = TimelineSlotsPerDay
private const val TimelineSlotMillis = 30L * 60L * 1_000L
private const val TimelineDurationMillis = 24L * 60L * 60L * 1_000L
private const val ClockTickMarginMillis = 50L
private const val RecentlyWatchedGroup = "Recently Watched"
private const val LiveSportsGroup = "Live Sports"
private val TimelineSlotWidth = 180.dp
private val TimelineProgramGap = 4.dp

object TestTags {
    const val LiveNav = "nav_live"
    const val SearchNav = "nav_search"
    const val PlaylistsNav = "nav_playlists"
    const val SettingsNav = "nav_settings"
    const val AddPlaylistAction = "action_add_playlist"
    const val AddPlaylistScreen = "screen_add_playlist"
    const val ChannelRowPrefix = "channel_"
    const val GroupRowPrefix = "group_"
    const val PlayerScreen = "screen_player"
    const val PlayerBack = "player_back"
    const val PlayerDiagnosticsToggle = "player_diagnostics_toggle"
    const val PlayerDiagnosticsPanel = "player_diagnostics_panel"
    const val CountryFilterPrefix = "country_filter_"
    const val CountryFilterEdit = "country_filter_edit"
    const val CountryFilterOptionPrefix = "country_filter_option_"
    const val GuideLoading = "guide_loading"
}

class MainActivity : ComponentActivity() {
    var isPipMode by mutableStateOf(false)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IPTVAppTheme {
                StreamHubApp()
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPipMode = isInPictureInPictureMode
    }

    fun setPipPlaybackEnabled(enabled: Boolean) {
        setPictureInPictureParams(
            PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setAutoEnterEnabled(enabled)
                .setSeamlessResizeEnabled(true)
                .build()
        )
    }
}

@Composable
private fun StreamHubApp(viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)) {
    val homeState by viewModel.homeState.collectAsState()
    val connectionTestState by viewModel.connectionTestState.collectAsState()
    val playlistSaveState by viewModel.playlistSaveState.collectAsState()
    val playlistRefreshState by viewModel.playlistRefreshState.collectAsState()
    val guideLoadState by viewModel.guideLoadState.collectAsState()
    val frequentChannelGroups by viewModel.frequentChannelGroups.collectAsState()
    val recentlyWatchedChannelIds by viewModel.recentlyWatchedChannelIds.collectAsState()
    val enabledCountryFilters by viewModel.enabledCountryFilters.collectAsState()
    val epgSyncStatus by viewModel.epgSyncStatus.collectAsState()
    val diagnostics by PlaybackDiagnosticsStore.recentSnapshots.collectAsState()
    var currentScreen by remember { mutableStateOf(AppScreen.Live) }
    var screenHistory by remember { mutableStateOf(emptyList<AppScreen>()) }
    var selectedLiveGroup by remember { mutableStateOf<String?>(null) }
    var showAddPlaylist by remember { mutableStateOf(false) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }

    fun playChannel(channel: Channel) {
        viewModel.recordChannelWatch(channel.id)
        selectedChannel = channel
    }

    fun navigateToScreen(screen: AppScreen) {
        if (screen != currentScreen) {
            screenHistory = screenHistory + currentScreen
            currentScreen = screen
        }
    }

    fun closeAddPlaylist() {
        viewModel.clearConnectionTest()
        viewModel.clearPlaylistSaveState()
        showAddPlaylist = false
    }

    BackHandler(enabled = selectedChannel == null) {
        when {
            showAddPlaylist -> closeAddPlaylist()
            currentScreen == AppScreen.Live && selectedLiveGroup != null -> selectedLiveGroup = null
            screenHistory.isNotEmpty() -> {
                currentScreen = screenHistory.last()
                screenHistory = screenHistory.dropLast(1)
            }
            currentScreen != AppScreen.Live -> currentScreen = AppScreen.Live
            else -> Unit
        }
    }

    Surface(color = AppBackground, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF060E18), AppBackground, Color(0xFF03070B))
                    )
                )
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0.dp),
                bottomBar = {
                    if (!showAddPlaylist && selectedChannel == null) {
                        BottomNavigationBar(
                            selected = currentScreen,
                            onSelected = ::navigateToScreen
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val playingChannel = selectedChannel
                    if (playingChannel != null) {
                        PlayerScreen(
                            channel = playingChannel,
                            channels = homeState.channels,
                            onBack = { selectedChannel = null },
                            onChannelSelected = ::playChannel
                        )
                    } else if (showAddPlaylist) {
                        AddPlaylistScreen(
                            connectionTestState = connectionTestState,
                            playlistSaveState = playlistSaveState,
                            onBack = ::closeAddPlaylist,
                            onTestConnection = viewModel::testPlaylistConnection,
                            onSavePlaylist = { name, serverUrl, username, password ->
                                viewModel.addPlaylist(name, serverUrl, username, password)
                            },
                            onSaveComplete = {
                                viewModel.clearPlaylistSaveState()
                                showAddPlaylist = false
                            }
                        )
                    } else {
                        when (currentScreen) {
                            AppScreen.Live -> LiveScreen(
                                channels = homeState.channels,
                                guidePrograms = homeState.guidePrograms,
                                categories = homeState.categories,
                                guideLoadState = guideLoadState,
                                frequentGroups = frequentChannelGroups,
                                recentlyWatchedChannelIds = recentlyWatchedChannelIds,
                                enabledCountryFilters = enabledCountryFilters,
                                selectedGroup = selectedLiveGroup,
                                onSelectedGroupChange = { selectedLiveGroup = it },
                                onGroupVisited = viewModel::recordChannelGroupVisit,
                                onRefreshGuide = viewModel::refreshGuide,
                                onPreloadFrequentGroups = viewModel::preloadFrequentGuideGroups,
                                onUpdateCountryFilters = viewModel::updateCountryFilters,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onPlayChannel = ::playChannel
                            )
                            AppScreen.Search -> SearchScreen(
                                channels = homeState.channels,
                                recentSearches = homeState.recentSearches,
                                onPlayChannel = ::playChannel
                            )
                            AppScreen.Playlists -> PlaylistsScreen(
                                playlists = homeState.playlists,
                                refreshState = playlistRefreshState,
                                epgSyncStatus = epgSyncStatus,
                                onAddPlaylist = { showAddPlaylist = true },
                                onRefreshPlaylist = viewModel::refreshPlaylist,
                                onDeletePlaylist = viewModel::deletePlaylist,
                                onRefreshMessageShown = viewModel::clearPlaylistRefreshState
                            )
                            AppScreen.Settings -> SettingsScreen(diagnostics = diagnostics)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppHeader(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            title()
        }
        actions()
    }
}

@Composable
private fun LiveScreen(
    channels: List<Channel>,
    guidePrograms: List<GuideProgram>,
    categories: List<String>,
    guideLoadState: GuideLoadState,
    frequentGroups: List<String>,
    recentlyWatchedChannelIds: List<String>,
    enabledCountryFilters: List<CountryGroupFilter>,
    selectedGroup: String?,
    onSelectedGroupChange: (String?) -> Unit,
    onGroupVisited: (String) -> Unit,
    onRefreshGuide: (List<String>) -> Unit,
    onPreloadFrequentGroups: (List<List<String>>) -> Unit,
    onUpdateCountryFilters: (List<CountryGroupFilter>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPlayChannel: (Channel) -> Unit
) {
    var selectedCountryFilters by remember { mutableStateOf(emptySet<CountryGroupFilter>()) }
    var showCountryFilterEditor by remember { mutableStateOf(false) }
    val availableCountryFilters = remember(categories) {
        availableCountryGroupFilters(categories)
    }
    val visibleCountryFilters = remember(enabledCountryFilters, availableCountryFilters) {
        enabledCountryFilters.filter { it in availableCountryFilters }
            .ifEmpty { CountryGroupFilter.DefaultFilters.filter { it in availableCountryFilters } }
    }
    LaunchedEffect(visibleCountryFilters) {
        selectedCountryFilters = selectedCountryFilters.intersect(visibleCountryFilters.toSet())
    }
    val allRecentlyWatchedChannels = remember(channels, recentlyWatchedChannelIds) {
        val channelsById = channels.associateBy { it.id }
        recentlyWatchedChannelIds.mapNotNull(channelsById::get)
    }
    val allCurrentLiveSportsChannels = remember(guidePrograms) {
        guidePrograms.asSequence()
            .filter { program ->
                isCurrentLiveSportsTitle(
                    title = program.primaryTitle,
                    channelName = program.channel.name,
                    channelCategory = program.channel.category
                )
            }
            .map { it.channel }
            .distinctBy { it.id }
            .sortedBy { it.number }
            .toList()
    }
    val countryChannels = remember(channels, selectedCountryFilters) {
        channels.filter { channel ->
            matchesSelectedCountryFilters(channel.category, selectedCountryFilters)
        }
    }
    val recentlyWatchedChannels = remember(allRecentlyWatchedChannels, selectedCountryFilters) {
        allRecentlyWatchedChannels.filter { channel ->
            matchesSelectedCountryFilters(channel.category, selectedCountryFilters)
        }
    }
    val currentLiveSportsChannels = remember(allCurrentLiveSportsChannels, selectedCountryFilters) {
        allCurrentLiveSportsChannels.filter { channel ->
            matchesSelectedCountryFilters(channel.category, selectedCountryFilters)
        }
    }
    val groups = remember(
        categories,
        recentlyWatchedChannels,
        currentLiveSportsChannels,
        selectedCountryFilters
    ) {
        val providerGroups = categories
            .ifEmpty { listOf("All Channels") }
            .filterNot { it == RecentlyWatchedGroup || it == LiveSportsGroup }
            .filter { group ->
                group == "All Channels" ||
                    group == "Favourites" ||
                    matchesSelectedCountryFilters(group, selectedCountryFilters)
            }
        val insertionIndex = (providerGroups.indexOf("Favourites") + 1)
            .takeIf { it > 0 }
            ?: minOf(1, providerGroups.size)
        providerGroups.toMutableList().apply {
            var nextIndex = insertionIndex
            if (recentlyWatchedChannels.isNotEmpty()) {
                add(nextIndex++, RecentlyWatchedGroup)
            }
            if (currentLiveSportsChannels.isNotEmpty()) {
                add(nextIndex, LiveSportsGroup)
            }
        }
    }
    LaunchedEffect(groups) {
        if (selectedGroup != null && selectedGroup !in groups) {
            onSelectedGroupChange(null)
        }
    }
    val guideProgramsByChannelId = remember(guidePrograms) {
        guidePrograms.associateBy { it.channel.id }
    }
    val channelCountsByGroup = remember(countryChannels) {
        countryChannels.groupingBy { it.category }.eachCount()
    }
    val channelsByGroup = remember(countryChannels) { countryChannels.groupBy { it.category } }
    val channelIdsByGroup = remember(channelsByGroup) {
        channelsByGroup.mapValues { (_, groupChannels) -> groupChannels.map { it.id } }
    }
    val favouriteCount = remember(countryChannels) { countryChannels.count { it.favorite } }
    val visibleChannels = remember(
        countryChannels,
        selectedGroup,
        recentlyWatchedChannels,
        currentLiveSportsChannels
    ) {
        when (selectedGroup) {
            null -> emptyList()
            "All Channels" -> countryChannels
            "Favourites" -> countryChannels.filter { it.favorite }
            RecentlyWatchedGroup -> recentlyWatchedChannels
            LiveSportsGroup -> currentLiveSportsChannels
            else -> channelsByGroup[selectedGroup].orEmpty()
        }
    }
    val groupListState = rememberLazyListState()
    LaunchedEffect(selectedCountryFilters) {
        groupListState.scrollToItem(0)
    }
    LaunchedEffect(frequentGroups, channelIdsByGroup) {
        val channelGroups = frequentGroups.mapNotNull { group ->
            channelIdsByGroup[group].orEmpty().takeIf { it.isNotEmpty() }
        }
        onPreloadFrequentGroups(channelGroups)
    }
    val visibleChannelIds = remember(visibleChannels) {
        visibleChannels.mapTo(mutableSetOf()) { it.id }
    }
    LaunchedEffect(selectedGroup, visibleChannelIds) {
        if (selectedGroup != null) {
            onRefreshGuide(visibleChannels.map { it.id })
        }
    }
    val isGuideLoading = guideLoadState.isLoading &&
        guideLoadState.channelIds.any { it in visibleChannelIds }
    val titleText = selectedGroup?.toGuideTitle() ?: "Live TV"

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedGroup != null) {
                        GlyphButton(kind = GlyphKind.Back, onClick = { onSelectedGroupChange(null) })
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column {
                        Text(
                            titleText,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = 0.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (selectedGroup != null) {
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${visibleChannels.size} channels",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.sp
                                )
                                if (isGuideLoading) {
                                    if (guideLoadState.totalChannels > 0) {
                                        LinearProgressIndicator(
                                            progress = { guideLoadState.progress.coerceIn(0f, 1f) },
                                            color = Accent,
                                            trackColor = Border.copy(alpha = 0.55f),
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .width(52.dp)
                                                .height(3.dp)
                                                .testTag(TestTags.GuideLoading)
                                        )
                                        Text(
                                            "${(guideLoadState.progress * 100).toInt()}%",
                                            color = TextSecondary,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.sp,
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                    } else {
                                        CircularProgressIndicator(
                                            color = Accent,
                                            strokeWidth = 1.5.dp,
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .size(12.dp)
                                                .testTag(TestTags.GuideLoading)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            actions = {
                if (selectedGroup == null) {
                    GlyphButton(kind = GlyphKind.Bell, onClick = {})
                } else {
                    GlyphButton(kind = GlyphKind.Search, onClick = {})
                    GlyphButton(kind = GlyphKind.More, onClick = {})
                }
            }
        )

        if (selectedGroup == null) {
            CountryGroupFilterControls(
                filters = visibleCountryFilters,
                selected = selectedCountryFilters,
                onSelected = { filter ->
                    selectedCountryFilters = if (filter in selectedCountryFilters) {
                        selectedCountryFilters - filter
                    } else {
                        selectedCountryFilters + filter
                    }
                },
                onEdit = { showCountryFilterEditor = true },
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 8.dp)
            )
            LazyColumn(
                state = groupListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 4.dp,
                    bottom = 14.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groups, key = { it }) { group ->
                    LiveGroupRow(
                        group = group,
                        channelCount = when (group) {
                            "All Channels" -> countryChannels.size
                            "Favourites" -> favouriteCount
                            RecentlyWatchedGroup -> recentlyWatchedChannels.size
                            LiveSportsGroup -> currentLiveSportsChannels.size
                            else -> channelCountsByGroup[group] ?: 0
                        },
                        onClick = {
                            onGroupVisited(group)
                            onSelectedGroupChange(group)
                        }
                    )
                }
            }
        } else {
            LiveTimelineGuide(
                channels = visibleChannels,
                programsByChannelId = guideProgramsByChannelId,
                isGuideLoading = isGuideLoading,
                onToggleFavorite = onToggleFavorite,
                onPlayChannel = onPlayChannel
            )
        }
    }

    if (showCountryFilterEditor) {
        CountryFilterEditorDialog(
            availableFilters = availableCountryFilters,
            enabledFilters = visibleCountryFilters,
            onSave = { filters ->
                onUpdateCountryFilters(filters)
                selectedCountryFilters = selectedCountryFilters.intersect(filters.toSet())
                showCountryFilterEditor = false
            },
            onDismiss = { showCountryFilterEditor = false }
        )
    }
}

@Composable
private fun LiveTimelineGuide(
    channels: List<Channel>,
    programsByChannelId: Map<String, GuideProgram>,
    isGuideLoading: Boolean,
    onToggleFavorite: (String) -> Unit,
    onPlayChannel: (Channel) -> Unit
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var programInfo by remember { mutableStateOf<Pair<Channel, GuideProgramBlock>?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            val currentEpochMillis = System.currentTimeMillis()
            now = Instant.ofEpochMilli(currentEpochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
            val millisUntilNextMinute = 60_000L - (currentEpochMillis % 60_000L)
            delay(millisUntilNextMinute + ClockTickMarginMillis)
        }
    }
    val timelineScroll = rememberScrollState()
    var selectedDayOffset by remember { mutableStateOf(0) }
    val today = now.toLocalDate()
    val selectedDate = today.plusDays(selectedDayOffset.toLong())
    val slotStart = if (selectedDayOffset == 0) {
        now.truncatedTo(ChronoUnit.HOURS).plusMinutes((now.minute / 30L) * 30L)
    } else {
        selectedDate.atStartOfDay()
    }
    val slotStartEpochMillis = slotStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val nowEpochMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val slotLabels = List(TimelineCellCount) { index -> slotStart.plusMinutes(index * 30L) }
    val secondsIntoSlot = ChronoUnit.SECONDS.between(slotStart, now).coerceIn(0L, 30L * 60L)
    val currentTimeOffset = if (selectedDayOffset == 0) {
        TimelineSlotWidth * (secondsIntoSlot / (30f * 60f))
    } else {
        null
    }
    LaunchedEffect(selectedDayOffset) {
        timelineScroll.scrollTo(0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GuideTimelineHeader(
            slots = slotLabels,
            now = now,
            selectedDayOffset = selectedDayOffset,
            onDaySelected = { selectedDayOffset = it },
            currentTimeOffset = currentTimeOffset,
            scrollState = timelineScroll,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 14.dp,
                end = 14.dp,
                bottom = 14.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                GuideTimelineRow(
                    channel = channel,
                    program = programsByChannelId[channel.id],
                    timelineStartEpochMillis = slotStartEpochMillis,
                    nowEpochMillis = nowEpochMillis,
                    currentTimeOffset = currentTimeOffset,
                    isGuideLoading = isGuideLoading,
                    scrollState = timelineScroll,
                    onFavoriteClick = { onToggleFavorite(channel.id) },
                    onProgramInfo = { block -> programInfo = channel to block },
                    onPlayClick = { onPlayChannel(channel) }
                )
            }
        }
    }
    programInfo?.let { (channel, block) ->
        ProgramInfoDialog(
            channel = channel,
            program = block,
            onDismiss = { programInfo = null }
        )
    }
}

@Composable
private fun GuideTimelineHeader(
    slots: List<LocalDateTime>,
    now: LocalDateTime,
    selectedDayOffset: Int,
    onDaySelected: (Int) -> Unit,
    currentTimeOffset: Dp?,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var dayMenuExpanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.width(88.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .clickable { dayMenuExpanded = true }
                    .background(Panel.copy(alpha = 0.92f))
                    .border(1.dp, Border.copy(alpha = 0.6f), RoundedCornerShape(7.dp))
                    .padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallGlyph(kind = GlyphKind.Calendar, tint = TextSecondary, modifier = Modifier.size(15.dp))
                Text(
                    dayLabel(now, selectedDayOffset),
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 5.dp)
                )
            }
            DropdownMenu(
                expanded = dayMenuExpanded,
                onDismissRequest = { dayMenuExpanded = false }
            ) {
                repeat(TimelineDays) { dayOffset ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                dayLabel(now, dayOffset),
                                color = TextPrimary,
                                letterSpacing = 0.sp
                            )
                        },
                        onClick = {
                            onDaySelected(dayOffset)
                            dayMenuExpanded = false
                        }
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp)
                .height(42.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(timelineContentWidth(slots.size))
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        slots.forEach { slot ->
                            Text(
                                if (slot.hour == 0 && slot.minute == 0) {
                                    slot.format(DaySlotFormatter)
                                } else {
                                    slot.format(TimeSlotFormatter).lowercase()
                                },
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.sp,
                                maxLines = 1,
                                modifier = Modifier.width(TimelineSlotWidth)
                            )
                        }
                    }
                    if (currentTimeOffset != null) {
                        Box(
                            modifier = Modifier
                                .padding(start = currentTimeOffset)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Accent)
                                .padding(horizontal = 9.dp, vertical = 7.dp)
                                .align(Alignment.CenterStart),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                now.format(TimeSlotFormatter).lowercase(),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideTimelineRow(
    channel: Channel,
    program: GuideProgram?,
    timelineStartEpochMillis: Long,
    nowEpochMillis: Long,
    currentTimeOffset: Dp?,
    isGuideLoading: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    onFavoriteClick: () -> Unit,
    onProgramInfo: (GuideProgramBlock) -> Unit,
    onPlayClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${TestTags.ChannelRowPrefix}${channel.id}")
            .height(108.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GuideChannelCard(
            channel = channel,
            onFavoriteClick = onFavoriteClick,
            modifier = Modifier
                .width(88.dp)
                .fillMaxHeight()
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(timelineContentWidth(TimelineCellCount))
                        .fillMaxHeight()
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val slotWidthPx = TimelineSlotWidth.toPx()
                        val lineColor = Border.copy(alpha = 0.22f)
                        repeat(TimelineCellCount + 1) { index ->
                            val x = index * slotWidthPx
                            drawLine(
                                color = lineColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
                    val timelineEndEpochMillis = timelineStartEpochMillis + TimelineDurationMillis
                    val timelineBlocks = program?.timeline.orEmpty()
                        .filter { block ->
                            block.endsAtEpochMillis > timelineStartEpochMillis &&
                                block.startsAtEpochMillis < timelineEndEpochMillis
                        }
                    if (timelineBlocks.isEmpty()) {
                        GuideProgramCell(
                            title = "",
                            time = "",
                            isCurrent = false,
                            progress = 0f,
                            showPlaceholder = true,
                            placeholderText = if (isGuideLoading) "Loading guide" else "No guide data",
                            onInfoClick = null,
                            onClick = onPlayClick,
                            modifier = Modifier.width(TimelineSlotWidth - TimelineProgramGap)
                        )
                    } else {
                        timelineBlocks.forEach { block ->
                            val visibleStart = maxOf(block.startsAtEpochMillis, timelineStartEpochMillis)
                            val visibleEnd = minOf(block.endsAtEpochMillis, timelineEndEpochMillis)
                            val startSlots = (visibleStart - timelineStartEpochMillis).toFloat() / TimelineSlotMillis
                            val durationSlots = (visibleEnd - visibleStart).toFloat() / TimelineSlotMillis
                            val startOffset = TimelineSlotWidth * startSlots
                            val programWidth = (TimelineSlotWidth * durationSlots - TimelineProgramGap)
                                .coerceAtLeast(52.dp)
                            val isCurrent = block.startsAtEpochMillis <= nowEpochMillis &&
                                block.endsAtEpochMillis > nowEpochMillis
                            val liveProgress = if (isCurrent) {
                                val duration = block.endsAtEpochMillis - block.startsAtEpochMillis
                                if (duration > 0L) {
                                    (nowEpochMillis - block.startsAtEpochMillis).toFloat() / duration.toFloat()
                                } else {
                                    0f
                                }
                            } else {
                                0f
                            }
                            GuideProgramCell(
                                title = block.title,
                                time = block.time,
                                isCurrent = isCurrent,
                                progress = liveProgress,
                                showPlaceholder = false,
                                onInfoClick = { onProgramInfo(block) },
                                onClick = onPlayClick,
                                modifier = Modifier
                                    .offset(x = startOffset)
                                    .width(programWidth)
                            )
                        }
                    }
                    if (currentTimeOffset != null) {
                        Box(
                            modifier = Modifier
                                .padding(start = currentTimeOffset)
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(Accent.copy(alpha = 0.32f))
                                .align(Alignment.CenterStart)
                        )
                    }
                }
            }
        }
    }
}

private fun timelineContentWidth(cellCount: Int): Dp {
    return TimelineSlotWidth * cellCount
}

private fun dayLabel(now: LocalDateTime, dayOffset: Int): String {
    return when (dayOffset) {
        0 -> "Today"
        1 -> "Tomorrow"
        else -> now.toLocalDate().plusDays(dayOffset.toLong()).format(DaySlotFormatter)
    }
}

@Composable
private fun GuideChannelCard(
    channel: Channel,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Panel.copy(alpha = 0.92f))
            .border(1.dp, Border.copy(alpha = 0.28f), RoundedCornerShape(6.dp))
    ) {
        Text(
            channel.name,
            color = TextSecondary,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 4.dp, top = 5.dp, end = 4.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 6.dp)
        ) {
            LogoBadge(channel = channel)
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(24.dp)
                .padding(start = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                channel.number.toString(),
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onFavoriteClick),
                contentAlignment = Alignment.Center
            ) {
                SmallGlyph(
                    kind = GlyphKind.Star,
                    tint = if (channel.favorite) Accent else TextSecondary,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
private fun GuideProgramCell(
    title: String,
    time: String,
    isCurrent: Boolean,
    progress: Float,
    showPlaceholder: Boolean,
    placeholderText: String = "No guide data",
    onInfoClick: (() -> Unit)?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasProgram = title.isNotBlank()
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .background(
                if (hasProgram) {
                    PanelSoft
                } else {
                    Panel.copy(alpha = 0.38f)
                }
            )
            .border(
                1.dp,
                if (isCurrent) Accent.copy(alpha = 0.55f) else Border.copy(alpha = 0.55f),
                RoundedCornerShape(6.dp)
            )
    ) {
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(Accent)
                    .align(Alignment.CenterStart)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 10.dp,
                    end = 10.dp,
                    top = 8.dp,
                    bottom = if (onInfoClick != null) 25.dp else 8.dp
                ),
            verticalArrangement = Arrangement.Center
        ) {
            if (hasProgram || showPlaceholder) {
                Text(
                    title.ifBlank { placeholderText },
                    color = if (hasProgram) TextPrimary else TextMuted,
                    fontSize = if (hasProgram) 12.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp,
                    letterSpacing = 0.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Clip
                )
            }
            if (time.isNotBlank()) {
                Text(
                    time,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    letterSpacing = 0.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        if (onInfoClick != null) {
            Box(
                modifier = Modifier
                    .padding(start = 5.dp, bottom = 4.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onInfoClick)
                    .background(Panel.copy(alpha = 0.9f))
                    .border(1.dp, Border.copy(alpha = 0.7f), CircleShape)
                    .align(Alignment.BottomStart),
                contentAlignment = Alignment.Center
            ) {
                SmallGlyph(
                    kind = GlyphKind.Info,
                    tint = TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(Accent)
                    .align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun ProgramInfoDialog(
    channel: Channel,
    program: GuideProgramBlock,
    onDismiss: () -> Unit
) {
    val start = Instant.ofEpochMilli(program.startsAtEpochMillis)
        .atZone(ZoneId.systemDefault())
    val end = Instant.ofEpochMilli(program.endsAtEpochMillis)
        .atZone(ZoneId.systemDefault())
    val schedule = buildString {
        append(start.format(ProgramDateFormatter))
        append(", ")
        append(start.format(TimeSlotFormatter).lowercase())
        append(" - ")
        append(end.format(TimeSlotFormatter).lowercase())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        shape = RoundedCornerShape(8.dp),
        title = {
            Text(
                program.title,
                color = TextPrimary,
                fontSize = 19.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
        },
        text = {
            Column {
                Text(
                    channel.name,
                    color = AccentAlt,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallGlyph(
                        kind = GlyphKind.Clock,
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        schedule,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        letterSpacing = 0.sp,
                        modifier = Modifier.padding(start = 7.dp)
                    )
                }
                Text(
                    program.description?.takeIf { it.isNotBlank() }
                        ?: "No programme description is available.",
                    color = if (program.description.isNullOrBlank()) TextMuted else TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.sp,
                    modifier = Modifier.padding(top = 18.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Accent, letterSpacing = 0.sp)
            }
        }
    )
}

@Composable
private fun CountryGroupFilterControls(
    filters: List<CountryGroupFilter>,
    selected: Set<CountryGroupFilter>,
    onSelected: (CountryGroupFilter) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CountryGroupFilterBar(
            filters = filters,
            selected = selected,
            onSelected = onSelected,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .width(52.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onEdit)
                .background(Panel.copy(alpha = 0.9f))
                .border(1.dp, Border, RoundedCornerShape(6.dp))
                .testTag(TestTags.CountryFilterEdit),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Edit",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun CountryGroupFilterBar(
    filters: List<CountryGroupFilter>,
    selected: Set<CountryGroupFilter>,
    onSelected: (CountryGroupFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Panel.copy(alpha = 0.9f))
            .border(1.dp, Border, RoundedCornerShape(6.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEachIndexed { index, filter ->
            if (index > 0) {
                Spacer(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Border.copy(alpha = 0.7f))
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelected(filter) }
                    .background(if (filter in selected) Accent.copy(alpha = 0.8f) else Color.Transparent)
                    .testTag("${TestTags.CountryFilterPrefix}${filter.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    filter.label,
                    color = if (filter in selected) TextPrimary else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (filter in selected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CountryFilterEditorDialog(
    availableFilters: List<CountryGroupFilter>,
    enabledFilters: List<CountryGroupFilter>,
    onSave: (List<CountryGroupFilter>) -> Unit,
    onDismiss: () -> Unit
) {
    var pendingFilters by remember(availableFilters, enabledFilters) {
        mutableStateOf(enabledFilters.toSet())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        shape = RoundedCornerShape(8.dp),
        title = {
            Text(
                "Choose countries",
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(availableFilters, key = { it.name }) { filter ->
                    val checked = filter in pendingFilters
                    val enabled = checked || pendingFilters.size < CountryGroupFilter.MaxEnabledFilters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = enabled) {
                                pendingFilters = if (checked) {
                                    pendingFilters - filter
                                } else {
                                    pendingFilters + filter
                                }
                            }
                            .testTag("${TestTags.CountryFilterOptionPrefix}${filter.name.lowercase()}")
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { shouldCheck ->
                                pendingFilters = if (shouldCheck) {
                                    if (pendingFilters.size < CountryGroupFilter.MaxEnabledFilters) pendingFilters + filter else pendingFilters
                                } else {
                                    pendingFilters - filter
                                }
                            },
                            enabled = enabled,
                            colors = CheckboxDefaults.colors(
                                checkedColor = Accent,
                                uncheckedColor = TextSecondary,
                                checkmarkColor = TextPrimary,
                                disabledCheckedColor = Accent.copy(alpha = 0.45f),
                                disabledUncheckedColor = TextMuted.copy(alpha = 0.45f)
                            )
                        )
                        Text(
                            filter.displayName,
                            color = if (enabled) TextPrimary else TextMuted,
                            fontSize = 14.sp,
                            letterSpacing = 0.sp,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .weight(1f)
                        )
                        Text(
                            filter.label,
                            color = if (checked) Accent else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(availableFilters.filter { it in pendingFilters })
                },
                enabled = pendingFilters.isNotEmpty()
            ) {
                Text("Save", color = if (pendingFilters.isNotEmpty()) Accent else TextMuted, letterSpacing = 0.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary, letterSpacing = 0.sp)
            }
        }
    )
}

@Composable
private fun LiveGroupRow(
    group: String,
    channelCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${TestTags.GroupRowPrefix}${group.toStableTag()}")
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(Panel.copy(alpha = 0.94f))
            .border(1.dp, Border.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmallGlyph(
            kind = when (group) {
                "Favourites" -> GlyphKind.Star
                RecentlyWatchedGroup -> GlyphKind.Clock
                LiveSportsGroup -> GlyphKind.Screen
                else -> GlyphKind.List
            },
            tint = if (group == "Favourites") AccentAlt else TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier
                .padding(start = 14.dp)
                .weight(1f)
        ) {
            Text(
                group,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Text(
                "$channelCount channels",
                color = TextMuted,
                fontSize = 12.sp,
                letterSpacing = 0.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        SmallGlyph(kind = GlyphKind.Chevron, tint = TextMuted)
    }
}

@Composable
private fun LiveGuideChannelRow(
    channel: Channel,
    program: GuideProgram?,
    onFavoriteClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val hasProgram = !program?.primaryTitle.isNullOrBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${TestTags.ChannelRowPrefix}${channel.id}")
            .height(82.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onPlayClick)
            .background(Panel.copy(alpha = 0.9f))
            .border(1.dp, Border.copy(alpha = 0.38f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LogoBadge(channel = channel)
        Text(
            channel.number.toString(),
            color = TextMuted,
            fontSize = 12.sp,
            letterSpacing = 0.sp,
            modifier = Modifier
                .width(30.dp)
                .padding(start = 8.dp)
        )
        Column(modifier = Modifier.width(116.dp)) {
            Text(
                channel.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
                maxLines = 2,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                channel.category,
                color = TextMuted,
                fontSize = 11.sp,
                letterSpacing = 0.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 6.dp, top = 8.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(PanelSoft.copy(alpha = 0.72f))
                .border(1.dp, Border.copy(alpha = 0.45f), RoundedCornerShape(5.dp))
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                program?.primaryTitle?.takeIf { it.isNotBlank() } ?: "No guide data",
                color = TextPrimary,
                fontSize = if (hasProgram) 12.sp else 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
                lineHeight = 14.sp,
                maxLines = if (hasProgram) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
            if (hasProgram) {
                Text(
                    program?.primaryTime?.takeIf { it.isNotBlank() }.orEmpty(),
                    color = TextSecondary,
                    fontSize = 10.sp,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF2A3341))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(program?.progress?.coerceIn(0f, 1f) ?: 0f)
                            .height(3.dp)
                            .background(Accent)
                    )
                }
            }
        }
        Box(modifier = Modifier.padding(start = 8.dp).clickable(onClick = onFavoriteClick)) {
            SmallGlyph(
                kind = if (channel.favorite) GlyphKind.Star else GlyphKind.Playlist,
                tint = if (channel.favorite) AccentAlt else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun GuideScreen(
    programs: List<GuideProgram>,
    onPlayChannel: (Channel) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlyphButton(kind = GlyphKind.Menu, onClick = {})
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        "TV Guide",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = 0.sp
                    )
                }
            },
            actions = {
                GlyphButton(kind = GlyphKind.Filter, onClick = {})
                GlyphButton(kind = GlyphKind.Calendar, onClick = {})
            }
        )

        DateStrip(modifier = Modifier.padding(horizontal = 18.dp))
        TimeStrip(modifier = Modifier.padding(start = 126.dp, end = 18.dp, top = 18.dp, bottom = 6.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    bottom = 16.dp
                )
            ) {
                items(programs, key = { it.channel.id }) { program ->
                    GuideRow(
                        program = program,
                        onPlayChannel = { onPlayChannel(program.channel) }
                    )
                }
            }
            Box(
                modifier = Modifier
                    .padding(start = 126.dp)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Accent)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(
    channels: List<Channel>,
    recentSearches: List<String>,
    onPlayChannel: (Channel) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val resultChannels = channels.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(
            title = {
                Text(
                    "Search",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 0.sp
                )
            }
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            placeholder = { Text("Search channels...", color = TextMuted, fontSize = 14.sp) },
            leadingIcon = { SmallGlyph(kind = GlyphKind.Search, tint = TextSecondary) },
            trailingIcon = { SmallGlyph(kind = GlyphKind.Mic, tint = TextPrimary) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 14.sp, letterSpacing = 0.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PanelSoft,
                unfocusedContainerColor = PanelSoft,
                focusedBorderColor = Border,
                unfocusedBorderColor = Border,
                cursorColor = Accent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        SectionHeader(
            title = "Recent Searches",
            action = "Clear",
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 8.dp)
        )

        Column(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            recentSearches.forEach { item ->
                RecentSearchRow(label = item)
            }
        }

        Text(
            "Categories",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(start = 18.dp, top = 22.dp, bottom = 10.dp)
        )
        CategoryRows()

        Text(
            "Channels",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(start = 18.dp, top = 22.dp, bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp,
                end = 18.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(resultChannels, key = { it.id }) { channel ->
                ChannelRow(
                    channel = channel,
                    compact = true,
                    onPlayClick = { onPlayChannel(channel) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistsScreen(
    playlists: List<IptvPlaylist>,
    refreshState: PlaylistRefreshState,
    epgSyncStatus: EpgSyncStatus,
    onAddPlaylist: () -> Unit,
    onRefreshPlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRefreshMessageShown: () -> Unit
) {
    LaunchedEffect(refreshState) {
        if (refreshState is PlaylistRefreshState.Success || refreshState is PlaylistRefreshState.Error) {
            delay(3_500)
            onRefreshMessageShown()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(
            title = {
                Text(
                    "Playlists",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 0.sp
                )
            },
            actions = {
                GlyphButton(
                    kind = GlyphKind.Plus,
                    onClick = onAddPlaylist,
                    modifier = Modifier.testTag(TestTags.AddPlaylistAction)
                )
            }
        )

        if (playlists.isNotEmpty()) {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "epg-sync-status") {
                    EpgSyncPanel(status = epgSyncStatus)
                }
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        refreshState = refreshState,
                        onRefresh = { onRefreshPlaylist(playlist.id) },
                        onDelete = { onDeletePlaylist(playlist.id) }
                    )
                }
            }
        } else {
            EmptyPlaylistState(onAddPlaylist = onAddPlaylist)
        }
    }
}

@Composable
private fun EpgSyncPanel(status: EpgSyncStatus) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Panel)
            .border(1.dp, Border.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallGlyph(
                kind = GlyphKind.Calendar,
                tint = if (status.isError) Color(0xFFFF8A8A) else AccentAlt,
                modifier = Modifier.size(18.dp)
            )
            Text(
                "Daily EPG Sync",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                modifier = Modifier.padding(start = 9.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            status.progressPercent?.let { percent ->
                Text(
                    "$percent%",
                    color = if (status.isRunning) AccentAlt else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
            }
        }
        Text(
            status.message,
            color = if (status.isError) Color(0xFFFFA3A3) else TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (status.isRunning) {
            if (status.progressPercent != null) {
                LinearProgressIndicator(
                    progress = { status.progressPercent / 100f },
                    color = AccentAlt,
                    trackColor = Border.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 11.dp)
                        .height(4.dp)
                )
            } else {
                LinearProgressIndicator(
                    color = AccentAlt,
                    trackColor = Border.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 11.dp)
                        .height(4.dp)
                )
            }
        }
        Text(
            "Runs daily on Wi-Fi when battery is not low.",
            color = TextMuted,
            fontSize = 10.sp,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(top = 9.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPlaylistScreen(
    connectionTestState: ConnectionTestState,
    playlistSaveState: PlaylistSaveState,
    onBack: () -> Unit,
    onTestConnection: (String, String, String) -> Unit,
    onSavePlaylist: (String, String, String, String) -> Unit,
    onSaveComplete: () -> Unit
) {
    var playlistName by remember { mutableStateOf("My IPTV") }
    var serverUrl by remember { mutableStateOf("http://server.com:8080") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(playlistSaveState) {
        if (playlistSaveState == PlaylistSaveState.Success) {
            onSaveComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.AddPlaylistScreen)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlyphButton(kind = GlyphKind.Back, onClick = onBack)
            Spacer(modifier = Modifier.width(20.dp))
            Text(
                "Add Playlist",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 0.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                "Save",
                color = Color(0xFFA9A5FF),
                fontSize = 14.sp,
                letterSpacing = 0.sp,
                modifier = Modifier.clickable {
                    if (playlistSaveState != PlaylistSaveState.Saving) {
                        onSavePlaylist(playlistName, serverUrl, username, password)
                    }
                }
            )
        }

        Text(
            "XCODES Details",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        AppTextField(label = "Playlist Name", value = playlistName, onValueChange = { playlistName = it })
        AppTextField(label = "Server URL", value = serverUrl, onValueChange = { serverUrl = it })
        AppTextField(label = "Username", value = username, onValueChange = { username = it }, placeholder = "Enter username")
        AppTextField(
            label = "Password",
            value = password,
            onValueChange = { password = it },
            placeholder = "Enter password",
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = {
                GlyphButton(
                    kind = if (passwordVisible) GlyphKind.Hide else GlyphKind.Eye,
                    onClick = { passwordVisible = !passwordVisible }
                )
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { onSavePlaylist(playlistName, serverUrl, username, password) },
            enabled = playlistSaveState != PlaylistSaveState.Saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            Text(
                if (playlistSaveState == PlaylistSaveState.Saving) "Syncing Playlist..." else "Save Playlist",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
        }

        PlaylistSavePanel(
            state = playlistSaveState,
            modifier = Modifier.padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = { onTestConnection(serverUrl, username, password) },
            enabled = connectionTestState != ConnectionTestState.Testing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, Color(0xFF736DFF)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC7C3FF))
        ) {
            Text(
                if (connectionTestState == ConnectionTestState.Testing) "Testing..." else "Test Connection",
                letterSpacing = 0.sp
            )
        }

        ConnectionTestPanel(
            state = connectionTestState,
            modifier = Modifier.padding(top = 12.dp)
        )

        InfoPanel(
            text = "Login details are stored on this device.",
            modifier = Modifier.padding(top = 22.dp)
        )
    }
}

@Composable
@androidx.annotation.OptIn(UnstableApi::class)
private fun PlayerScreen(
    channel: Channel,
    channels: List<Channel>,
    onBack: () -> Unit,
    onChannelSelected: (Channel) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activity = context as? Activity
    val mainActivity = activity as? MainActivity
    val isPipMode = mainActivity?.isPipMode == true
    var playbackState by remember { mutableStateOf(Player.STATE_IDLE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var streamFormat by remember { mutableStateOf(LiveStreamFormat.HLS) }
    var diagnosticsVisible by remember { mutableStateOf(false) }
    val telemetryRecorder = remember { PlaybackTelemetryRecorder() }
    var telemetry by remember { mutableStateOf(telemetryRecorder.snapshot()) }
    val currentIndex = channels.indexOfFirst { it.id == channel.id }
    val previousChannel = channels.getOrNull(currentIndex - 1)
    val nextChannel = channels.getOrNull(currentIndex + 1)

    val player = remember(streamFormat) {
        IptvPlayerFactory(context).createLivePlayer(streamFormat)
    }
    var bandwidthEstimate by remember(player) { mutableStateOf<Long?>(null) }
    var droppedFrames by remember(player) { mutableStateOf(0) }

    BackHandler(enabled = !isPipMode, onBack = onBack)

    DisposableEffect(activity) {
        val window = activity?.window
        val keepScreenOnWasSet = ((window?.attributes?.flags ?: 0) and
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            if (!keepScreenOnWasSet) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    DisposableEffect(mainActivity) {
        mainActivity?.setPipPlaybackEnabled(true)
        onDispose {
            mainActivity?.setPipPlaybackEnabled(false)
        }
    }

    DisposableEffect(activity, isLandscape, isPipMode) {
        val window = activity?.window
        val insetsController = window?.let {
            WindowCompat.getInsetsController(it, it.decorView)
        }
        if (isLandscape && !isPipMode) {
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (isLandscape && !isPipMode) {
                insetsController?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackStateValue: Int) {
                playbackState = playbackStateValue
                telemetry = telemetryRecorder.onPlaybackStateChanged(playbackStateValue)
                Log.d(
                    "StreamHubPlayer",
                    "state=$playbackStateValue channel=${player.currentMediaItem?.mediaId} " +
                        "live=${player.isCurrentMediaItemLive} bufferedMs=${player.totalBufferedDuration}"
                )
                if (playbackStateValue != Player.STATE_IDLE) {
                    errorMessage = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val message = error.localizedMessage ?: "Unable to play this stream."
                Log.w(
                    "StreamHubPlayer",
                    "error=${error.errorCodeName} channel=${player.currentMediaItem?.mediaId}",
                    error
                )
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    telemetry = telemetryRecorder.onError("Recovered at live edge")
                    errorMessage = null
                    player.seekToDefaultPosition()
                    player.prepare()
                    player.play()
                    return
                }
                errorMessage = message
                telemetry = telemetryRecorder.onError(message)
            }
        }
        val analyticsListener = object : AnalyticsListener {
            override fun onBandwidthEstimate(
                eventTime: AnalyticsListener.EventTime,
                totalLoadTimeMs: Int,
                totalBytesLoaded: Long,
                bitrateEstimate: Long
            ) {
                bandwidthEstimate = bitrateEstimate.takeIf { it > 0L }
            }

            override fun onDroppedVideoFrames(
                eventTime: AnalyticsListener.EventTime,
                droppedFrameCount: Int,
                elapsedMs: Long
            ) {
                droppedFrames += droppedFrameCount
            }
        }

        player.addListener(listener)
        player.addAnalyticsListener(analyticsListener)

        onDispose {
            player.removeListener(listener)
            player.removeAnalyticsListener(analyticsListener)
            player.release()
        }
    }

    LaunchedEffect(channel.id, streamFormat) {
        errorMessage = null
        playbackState = Player.STATE_BUFFERING
        telemetry = telemetryRecorder.onChannelLoad(channel.id, channel.name)
        Log.i(
            "StreamHubPlayer",
            "loading channel=${channel.id} source=${streamFormat.name}"
        )
        val mediaItem = IptvPlayerFactory(context).buildLiveMediaItem(
            streamUrl = channel.streamUrl,
            channelId = channel.id,
            channelName = channel.name,
            format = streamFormat
        )
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    LaunchedEffect(player, channel.id, streamFormat) {
        var healthLogTicks = 0
        while (true) {
            delay(1_000)
            val videoFormat = player.videoFormat
            val audioFormat = player.audioFormat
            val liveOffset = resolveLiveOffset(
                nativeLiveOffsetMs = player.currentLiveOffset,
                isLive = player.isCurrentMediaItemLive,
                durationMs = player.duration,
                currentPositionMs = player.currentPosition
            )
            telemetry = telemetryRecorder.onMetrics(
                PlaybackMetrics(
                    source = if (streamFormat == LiveStreamFormat.MPEG_TS) "MPEG-TS" else streamFormat.label,
                    playbackState = playbackStateLabel(player.playbackState, player.isPlaying),
                    resolution = videoFormat?.let { resolutionLabel(it.width, it.height) },
                    videoCodec = videoFormat?.let { codecLabel(it.sampleMimeType, it.codecs) },
                    audioCodec = audioFormat?.let { codecLabel(it.sampleMimeType, it.codecs) },
                    audioChannelCount = audioFormat?.channelCount?.takeIf { it > 0 },
                    audioSampleRateHz = audioFormat?.sampleRate?.takeIf { it > 0 },
                    bandwidthEstimateBitsPerSecond = bandwidthEstimate,
                    bufferedDurationMs = player.totalBufferedDuration.coerceAtLeast(0L),
                    liveOffsetMs = liveOffset.durationMs,
                    liveOffsetEstimated = liveOffset.estimated,
                    droppedFrames = droppedFrames
                )
            )
            healthLogTicks++
            if (healthLogTicks % 5 == 0) {
                Log.d(
                    "StreamHubPlayer",
                    "health channel=${channel.id} source=${streamFormat.name} " +
                        "state=${player.playbackState} bufferedMs=${player.totalBufferedDuration} " +
                        "liveOffsetMs=${player.currentLiveOffset} bitrate=$bandwidthEstimate " +
                        "droppedFrames=$droppedFrames"
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.PlayerScreen)
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    setUseController(!isPipMode)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.setUseController(!isPipMode)
                if (isPipMode) {
                    playerView.hideController()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isLandscape && !isPipMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .background(Color.Black.copy(alpha = 0.56f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlyphButton(
                    kind = GlyphKind.Back,
                    onClick = onBack,
                    modifier = Modifier.testTag(TestTags.PlayerBack)
                )
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        channel.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 0.sp
                    )
                    Text(
                        channel.currentProgramTime,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        letterSpacing = 0.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                PlaybackDiagnosticsToggle(
                    selected = diagnosticsVisible,
                    onClick = { diagnosticsVisible = !diagnosticsVisible },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.56f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { previousChannel?.let(onChannelSelected) },
                    enabled = previousChannel != null,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text("Previous", fontSize = 12.sp, letterSpacing = 0.sp)
                }
                StreamFormatSelector(
                    selected = streamFormat,
                    enabled = supportsLiveStreamFormatSwitch(channel.streamUrl),
                    onSelected = { streamFormat = it }
                )
                OutlinedButton(
                    onClick = { nextChannel?.let(onChannelSelected) },
                    enabled = nextChannel != null,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text("Next", fontSize = 12.sp, letterSpacing = 0.sp)
                }
            }
        }

        if (isLandscape && !isPipMode) {
            PlaybackDiagnosticsToggle(
                selected = diagnosticsVisible,
                onClick = { diagnosticsVisible = !diagnosticsVisible },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            )
        }

        if (diagnosticsVisible && !isPipMode) {
            PlaybackTelemetryPanel(
                telemetry = telemetry,
                modifier = if (isLandscape) {
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                } else {
                    Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 76.dp, start = 14.dp, end = 14.dp)
                }
            )
        }

        if (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Accent, strokeWidth = 3.dp)
                Text(
                    "Loading stream",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    letterSpacing = 0.sp,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }
        }

        errorMessage?.takeUnless { isPipMode }?.let { message ->
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Panel.copy(alpha = 0.94f))
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "Stream unavailable",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.sp
                )
                Text(
                    message,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StreamFormatSelector(
    selected: LiveStreamFormat,
    enabled: Boolean,
    onSelected: (LiveStreamFormat) -> Unit
) {
    Row(
        modifier = Modifier
            .width(116.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, Border, RoundedCornerShape(6.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiveStreamFormat.entries.forEach { format ->
            val isSelected = format == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isSelected) Accent.copy(alpha = 0.72f) else Color.Transparent)
                    .clickable(enabled = enabled && !isSelected) { onSelected(format) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (format == LiveStreamFormat.MPEG_TS) "TS" else format.label,
                    color = when {
                        !enabled -> TextMuted
                        isSelected -> TextPrimary
                        else -> TextSecondary
                    },
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PlaybackDiagnosticsToggle(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .width(44.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .border(
                1.dp,
                if (selected) Accent else Border.copy(alpha = 0.8f),
                RoundedCornerShape(6.dp)
            )
            .testTag(TestTags.PlayerDiagnosticsToggle)
    ) {
        SmallGlyph(
            kind = GlyphKind.Diagnostics,
            tint = if (selected) Accent else TextPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun PlaybackTelemetryPanel(
    telemetry: PlaybackTelemetrySnapshot,
    modifier: Modifier = Modifier
) {
    val videoDescription = listOfNotNull(telemetry.resolution, telemetry.videoCodec)
        .joinToString(" / ")
        .ifBlank { "Unknown" }
    val audioDescription = buildList {
        telemetry.audioCodec?.let(::add)
        telemetry.audioChannelCount?.let { channels ->
            add(
                when (channels) {
                    1 -> "Mono"
                    2 -> "Stereo"
                    else -> "$channels ch"
                }
            )
        }
        telemetry.audioSampleRateHz?.let { add("${it / 1_000} kHz") }
    }.joinToString(" / ").ifBlank { "Unknown" }
    val stateColor = when (telemetry.playbackState) {
        "Playing" -> Success
        "Buffering" -> Color(0xFFFFC857)
        else -> TextSecondary
    }

    Column(
        modifier = modifier
            .widthIn(max = 350.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.86f))
            .border(1.dp, Border.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
            .padding(12.dp)
            .testTag(TestTags.PlayerDiagnosticsPanel)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Playback diagnostics",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(stateColor)
            )
            Text(
                telemetry.playbackState,
                color = stateColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        DiagnosticsValueRow("Source", telemetry.source ?: "Unknown", "Bandwidth", formatBitrate(telemetry.bandwidthEstimateBitsPerSecond))
        DiagnosticsValueRow("Video", videoDescription, "Dropped", telemetry.droppedFrames.toString())
        DiagnosticsValueRow("Audio", audioDescription, "Buffer", formatPlaybackDuration(telemetry.bufferedDurationMs))
        val liveOffsetLabel = if (telemetry.liveOffsetEstimated) "Live edge" else "Live delay"
        val liveOffsetValue = telemetry.liveOffsetMs?.let { offset ->
            val prefix = if (telemetry.liveOffsetEstimated) "~" else ""
            prefix + formatPlaybackDuration(offset)
        } ?: "Not exposed"
        DiagnosticsValueRow(liveOffsetLabel, liveOffsetValue, "Startup", telemetry.startupMs?.let(::formatPlaybackDuration) ?: "Waiting")
        DiagnosticsValueRow("Rebuffers", telemetry.rebufferCount.toString(), "Errors", telemetry.errorCount.toString())
    }
}

@Composable
private fun DiagnosticsValueRow(
    firstLabel: String,
    firstValue: String,
    secondLabel: String,
    secondValue: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DiagnosticsValue(firstLabel, firstValue, Modifier.weight(1f))
        DiagnosticsValue(secondLabel, secondValue, Modifier.weight(1f))
    }
}

@Composable
private fun DiagnosticsValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            color = TextMuted,
            fontSize = 9.sp,
            letterSpacing = 0.sp
        )
        Text(
            value,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

@Composable
private fun SettingsScreen(diagnostics: List<PlaybackTelemetrySnapshot>) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(
            title = {
                Text(
                    "Settings",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 0.sp
                )
            }
        )
        listOf("Playback", "EPG sync", "Storage", "Appearance").forEach { label ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Panel)
                    .border(1.dp, Border.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = TextPrimary, fontSize = 15.sp, letterSpacing = 0.sp, modifier = Modifier.weight(1f))
                SmallGlyph(kind = GlyphKind.Chevron, tint = TextMuted)
            }
        }

        Text(
            "Diagnostics",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(start = 18.dp, top = 20.dp, bottom = 8.dp)
        )

        if (diagnostics.isEmpty()) {
            InfoPanel(
                text = "Playback diagnostics will appear after a stream starts.",
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(diagnostics, key = { "${it.channelId}-${it.channelSwitchCount}-${it.errorCount}" }) { item ->
                    DiagnosticsRow(item)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsRow(snapshot: PlaybackTelemetrySnapshot) {
    val streamDetails = listOfNotNull(snapshot.source, snapshot.resolution, snapshot.videoCodec)
        .joinToString(" / ")
        .ifBlank { "Stream details unavailable" }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Panel)
            .border(1.dp, Border.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Text(
            snapshot.channelName ?: "Unknown channel",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 0.sp
        )
        Text(
            "${snapshot.playbackState} / $streamDetails",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(top = 5.dp)
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Start ${snapshot.startupMs?.let { "${it}ms" } ?: "..."}", color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.sp)
            Text("Rebuf ${snapshot.rebufferCount}", color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.sp)
            Text("Err ${snapshot.errorCount}", color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.sp)
            Text("Drop ${snapshot.droppedFrames}", color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.sp)
        }
        Row(
            modifier = Modifier.padding(top = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Buffer ${formatPlaybackDuration(snapshot.bufferedDurationMs)}", color = TextMuted, fontSize = 11.sp, letterSpacing = 0.sp)
            val liveOffset = snapshot.liveOffsetMs?.let { offset ->
                val prefix = if (snapshot.liveOffsetEstimated) "~" else ""
                prefix + formatPlaybackDuration(offset)
            } ?: "Not exposed"
            Text("${if (snapshot.liveOffsetEstimated) "Edge" else "Delay"} $liveOffset", color = TextMuted, fontSize = 11.sp, letterSpacing = 0.sp)
            Text(formatBitrate(snapshot.bandwidthEstimateBitsPerSecond), color = TextMuted, fontSize = 11.sp, letterSpacing = 0.sp)
        }
        snapshot.lastError?.let {
            Text(
                it,
                color = Color(0xFFFFA3A3),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                letterSpacing = 0.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun CategoryTabs(
    categories: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { it }) { category ->
            val selectedTab = category == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selectedTab) Accent else Color.Transparent)
                    .clickable { onSelected(category) }
                    .padding(horizontal = 13.dp, vertical = 8.dp)
            ) {
                Text(
                    category,
                    color = if (selectedTab) TextPrimary else TextSecondary,
                    fontSize = 12.sp,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

@Composable
private fun ChannelRow(
    channel: Channel,
    showNumber: Boolean = false,
    compact: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onPlayClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${TestTags.ChannelRowPrefix}${channel.id}")
            .height(if (compact) 62.dp else 72.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onPlayClick)
            .background(Panel.copy(alpha = 0.94f))
            .border(1.dp, Border.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LogoBadge(channel = channel)
        if (showNumber) {
            Text(
                channel.number.toString(),
                color = TextPrimary,
                fontSize = 13.sp,
                letterSpacing = 0.sp,
                modifier = Modifier
                    .width(28.dp)
                    .padding(start = 12.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                channel.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Text(
                channel.currentProgramTime,
                color = TextSecondary,
                fontSize = 12.sp,
                letterSpacing = 0.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (!compact) {
                ProgressLine(progress = channel.progress, modifier = Modifier.padding(top = 8.dp))
            }
        }
        if (channel.favorite) {
            Box(modifier = Modifier.clickable(onClick = onFavoriteClick)) {
                SmallGlyph(kind = GlyphKind.Star, tint = TextSecondary)
            }
        } else {
            PlayButton()
        }
    }
}

@Composable
private fun LogoBadge(channel: Channel) {
    var showFallback by remember(channel.logoUrl) {
        mutableStateOf(channel.logoUrl.isNullOrBlank())
    }

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(PanelSoft),
        contentAlignment = Alignment.Center
    ) {
        if (showFallback) {
            Text(
                channel.logo,
                color = channel.logoColor,
                fontSize = if (channel.logo.length > 3) 14.sp else 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            )
        }
        if (!channel.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                onSuccess = { showFallback = false },
                onError = { showFallback = true }
            )
        }
    }
}

@Composable
private fun ProgressLine(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.78f)
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF2A3341))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(3.dp)
                .background(Accent)
        )
    }
}

@Composable
private fun PlayButton() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFF222B3A)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(13.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.2f, 0f)
                lineTo(size.width, size.height / 2f)
                lineTo(size.width * 0.2f, size.height)
                close()
            }
            drawPath(path, TextPrimary)
        }
    }
}

@Composable
private fun DateStrip(modifier: Modifier = Modifier) {
    val days = listOf("Today\n21", "Wed\n22", "Thu\n23", "Fri\n24", "Sat\n25", "Sun\n26")
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        days.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .width(if (index == 0) 56.dp else 42.dp)
                    .height(58.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (index == 0) Accent else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (index == 0) TextPrimary else TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

@Composable
private fun TimeStrip(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(60_000)
        }
    }
    val start = now.truncatedTo(ChronoUnit.HOURS).plusMinutes((now.minute / 30L) * 30L)
    val slots = listOf(start, start.plusMinutes(30), start.plusMinutes(60))
    Row(modifier = modifier.fillMaxWidth()) {
        slots.forEach {
            Text(
                it.format(TimeSlotFormatter).lowercase(),
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GuideRow(program: GuideProgram, onPlayChannel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onPlayChannel),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LogoBadge(channel = program.channel)
        Spacer(modifier = Modifier.width(14.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (program.startsAtHalfHour) {
                ProgramBlock(title = program.primaryTitle, time = program.channel.currentProgramTime, modifier = Modifier.weight(1f))
                ProgramBlock(title = program.secondaryTitle, time = "7:30 - 8:30pm", modifier = Modifier.weight(1f))
            } else {
                ProgramBlock(title = program.primaryTitle, time = program.channel.currentProgramTime, modifier = Modifier.weight(1.35f))
                if (program.secondaryTitle.isNotBlank()) {
                    ProgramBlock(title = program.secondaryTitle, time = "8:00 - 8:30pm", modifier = Modifier.weight(0.85f))
                }
            }
        }
    }
}

@Composable
private fun ProgramBlock(title: String, time: String, modifier: Modifier = Modifier) {
    if (title.isBlank()) {
        Box(modifier = modifier.fillMaxHeight())
        return
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(end = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF152234))
            .border(1.dp, Color(0xFF31405A), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Text(title, color = TextPrimary, fontSize = 13.sp, maxLines = 1, letterSpacing = 0.sp)
        Text(time, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp), letterSpacing = 0.sp)
    }
}

@Composable
private fun SectionHeader(title: String, action: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(action, color = TextSecondary, fontSize = 13.sp, letterSpacing = 0.sp)
    }
}

@Composable
private fun RecentSearchRow(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Panel),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(12.dp))
        SmallGlyph(kind = GlyphKind.Clock, tint = TextSecondary)
        Text(
            label,
            color = TextPrimary,
            fontSize = 14.sp,
            letterSpacing = 0.sp,
            modifier = Modifier
                .weight(1f)
                .padding(start = 13.dp)
        )
        SmallGlyph(kind = GlyphKind.Close, tint = TextSecondary)
        Spacer(modifier = Modifier.width(12.dp))
    }
}

@Composable
private fun CategoryRows() {
    val rows = listOf(
        listOf("All", "News", "Sports", "Kids", "Movies"),
        listOf("Entertainment", "Lifestyle", "Music")
    )
    Column(
        modifier = Modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { index, item ->
                    val selected = rowIndex == 0 && index == 0
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (selected) Accent else PanelSoft)
                            .padding(horizontal = 13.dp, vertical = 9.dp)
                    ) {
                        Text(
                            item,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            letterSpacing = 0.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPlaylistState(onAddPlaylist: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 42.dp)
            .padding(top = 92.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(98.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(PanelSoft)
                .border(1.dp, Border, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            SmallGlyph(kind = GlyphKind.List, tint = TextPrimary, modifier = Modifier.size(48.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Accent),
                contentAlignment = Alignment.Center
            ) {
                SmallGlyph(kind = GlyphKind.Plus, tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
        }

        Text(
            "No Playlists Added",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(top = 28.dp)
        )
        Text(
            "Add an XCODES playlist to load channels.",
            color = TextSecondary,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(top = 10.dp)
        )
        Button(
            onClick = onAddPlaylist,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(top = 26.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            Text("Add Playlist", color = TextPrimary, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: IptvPlaylist,
    refreshState: PlaylistRefreshState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onDelete: () -> Unit
) {
    val isSyncing = refreshState is PlaylistRefreshState.Syncing &&
        refreshState.playlistId == playlist.id
    val statusMessage = when (refreshState) {
        is PlaylistRefreshState.Success -> if (refreshState.playlistId == playlist.id) {
            "Resync complete. Channels and EPG updated."
        } else {
            null
        }
        is PlaylistRefreshState.Error -> if (refreshState.playlistId == playlist.id) {
            refreshState.message
        } else {
            null
        }
        else -> null
    }
    val statusColor = if (refreshState is PlaylistRefreshState.Error) Color(0xFFFFA3A3) else AccentAlt

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Border)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PanelSoft),
                contentAlignment = Alignment.Center
            ) {
                SmallGlyph(kind = GlyphKind.List, tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(playlist.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.sp)
                Text(playlist.serverUrl, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp), letterSpacing = 0.sp)
                Text("Username: ${playlist.username}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp), letterSpacing = 0.sp)
                Text("Last updated: ${playlist.lastUpdated}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp), letterSpacing = 0.sp)
                Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Success), contentAlignment = Alignment.Center) {
                        SmallGlyph(kind = GlyphKind.Check, tint = AppBackground, modifier = Modifier.size(9.dp))
                    }
                    Text(
                        if (playlist.connected) "Connected" else "Disconnected",
                        color = Success,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 7.dp),
                        letterSpacing = 0.sp
                    )
                }
                if (isSyncing) {
                    Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                        Text(
                            "Resyncing playlist and EPG...",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            letterSpacing = 0.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                statusMessage?.let { message ->
                    Text(
                        message,
                        color = statusColor,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        letterSpacing = 0.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !isSyncing,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (isSyncing) "Syncing" else "Resync", fontSize = 11.sp, letterSpacing = 0.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF6868).copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFA3A3)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Delete", fontSize = 11.sp, letterSpacing = 0.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        label = { Text(label, color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.sp) },
        placeholder = {
            if (placeholder.isNotBlank()) {
                Text(placeholder, color = TextMuted, fontSize = 14.sp, letterSpacing = 0.sp)
            }
        },
        trailingIcon = trailing,
        visualTransformation = visualTransformation,
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        textStyle = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 14.sp, letterSpacing = 0.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Panel,
            unfocusedContainerColor = Panel,
            focusedBorderColor = Border,
            unfocusedBorderColor = Border,
            cursorColor = Accent,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}

@Composable
private fun InfoPanel(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Panel)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .border(1.dp, TextSecondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("i", color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
        }
        Text(
            text,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}

@Composable
private fun ConnectionTestPanel(state: ConnectionTestState, modifier: Modifier = Modifier) {
    when (state) {
        ConnectionTestState.Idle -> Unit
        ConnectionTestState.Testing -> StatusPanel(
            title = "Testing connection",
            message = "Checking the XCODES server and account status.",
            color = Accent,
            modifier = modifier
        )
        ConnectionTestState.Success -> StatusPanel(
            title = "Connection successful",
            message = "The server accepted these XCODES details.",
            color = Success,
            modifier = modifier
        )
        is ConnectionTestState.Error -> StatusPanel(
            title = "Connection failed",
            message = state.message,
            color = Color(0xFFFF6868),
            modifier = modifier
        )
    }
}

@Composable
private fun PlaylistSavePanel(state: PlaylistSaveState, modifier: Modifier = Modifier) {
    when (state) {
        PlaylistSaveState.Idle,
        PlaylistSaveState.Success -> Unit
        PlaylistSaveState.Saving -> StatusPanel(
            title = "Syncing playlist",
            message = "Fetching live categories and channels.",
            color = Accent,
            modifier = modifier
        )
        is PlaylistSaveState.Error -> StatusPanel(
            title = "Save failed",
            message = state.message,
            color = Color(0xFFFF6868),
            modifier = modifier
        )
    }
}

@Composable
private fun StatusPanel(
    title: String,
    message: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Panel)
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.sp
            )
            Text(
                message,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                letterSpacing = 0.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun BottomNavigationBar(selected: AppScreen, onSelected: (AppScreen) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xF2050A0F))
            .border(1.dp, Border.copy(alpha = 0.45f))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppScreen.entries.forEach { screen ->
            BottomNavItem(
                screen = screen,
                selected = screen == selected,
                onClick = { onSelected(screen) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(screen: AppScreen, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) Accent else TextSecondary
    Column(
        modifier = Modifier
            .width(64.dp)
            .testTag(screen.testTag)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SmallGlyph(kind = screen.toGlyphKind(), tint = tint, modifier = Modifier.size(24.dp))
        Text(
            screen.title,
            color = tint,
            fontSize = 11.sp,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

private val AppScreen.testTag: String
    get() = when (this) {
        AppScreen.Live -> TestTags.LiveNav
        AppScreen.Search -> TestTags.SearchNav
        AppScreen.Playlists -> TestTags.PlaylistsNav
        AppScreen.Settings -> TestTags.SettingsNav
    }

private fun AppScreen.toGlyphKind(): GlyphKind = when (this) {
    AppScreen.Live -> GlyphKind.Screen
    AppScreen.Search -> GlyphKind.Search
    AppScreen.Playlists -> GlyphKind.Playlist
    AppScreen.Settings -> GlyphKind.Settings
}

private fun String.toStableTag(): String {
    return lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "group" }
}

private fun String.toGuideTitle(): String {
    return replace("|", "•").replace(Regex("\\s+"), " ").trim()
}

private enum class GlyphKind {
    Back,
    Bell,
    Calendar,
    Check,
    Chevron,
    Clock,
    Close,
    Diagnostics,
    Eye,
    Filter,
    Hide,
    Info,
    List,
    Menu,
    Mic,
    More,
    Playlist,
    Plus,
    Screen,
    Search,
    Settings,
    Star
}

@Composable
private fun GlyphButton(
    kind: GlyphKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier.size(36.dp)) {
        SmallGlyph(kind = kind, tint = TextPrimary)
    }
}

@Composable
private fun SmallGlyph(kind: GlyphKind, tint: Color, modifier: Modifier = Modifier.size(22.dp)) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        when (kind) {
            GlyphKind.Back -> {
                drawLine(tint, Offset(w * 0.65f, h * 0.18f), Offset(w * 0.28f, h * 0.5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.28f, h * 0.5f), Offset(w * 0.65f, h * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GlyphKind.Bell -> {
                drawArc(tint, 200f, 140f, false, topLeft = Offset(w * 0.22f, h * 0.2f), size = Size(w * 0.56f, h * 0.62f), style = stroke)
                drawLine(tint, Offset(w * 0.22f, h * 0.64f), Offset(w * 0.78f, h * 0.64f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawCircle(tint, radius = w * 0.05f, center = Offset(w * 0.5f, h * 0.82f))
            }
            GlyphKind.Calendar -> {
                drawRoundRect(tint, topLeft = Offset(w * 0.18f, h * 0.22f), size = Size(w * 0.64f, h * 0.62f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f), style = stroke)
                drawLine(tint, Offset(w * 0.18f, h * 0.4f), Offset(w * 0.82f, h * 0.4f), strokeWidth = stroke.width)
                drawLine(tint, Offset(w * 0.34f, h * 0.15f), Offset(w * 0.34f, h * 0.29f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.66f, h * 0.15f), Offset(w * 0.66f, h * 0.29f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GlyphKind.Check -> {
                drawLine(tint, Offset(w * 0.24f, h * 0.52f), Offset(w * 0.42f, h * 0.68f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.68f), Offset(w * 0.78f, h * 0.32f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GlyphKind.Chevron -> {
                drawLine(tint, Offset(w * 0.38f, h * 0.25f), Offset(w * 0.62f, h * 0.5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.62f, h * 0.5f), Offset(w * 0.38f, h * 0.75f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GlyphKind.Clock -> {
                drawCircle(tint, radius = w * 0.32f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.32f), Offset(w * 0.5f, h * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.52f), Offset(w * 0.64f, h * 0.6f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GlyphKind.Close -> {
                drawLine(tint, Offset(w * 0.28f, h * 0.28f), Offset(w * 0.72f, h * 0.72f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.72f, h * 0.28f), Offset(w * 0.28f, h * 0.72f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GlyphKind.Diagnostics -> {
                drawLine(tint, Offset(w * 0.18f, h * 0.82f), Offset(w * 0.82f, h * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.28f, h * 0.68f), Offset(w * 0.28f, h * 0.54f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.68f), Offset(w * 0.5f, h * 0.38f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.72f, h * 0.68f), Offset(w * 0.72f, h * 0.22f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.2f, h * 0.45f), Offset(w * 0.42f, h * 0.3f), strokeWidth = stroke.width * 0.7f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.3f), Offset(w * 0.58f, h * 0.4f), strokeWidth = stroke.width * 0.7f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.58f, h * 0.4f), Offset(w * 0.8f, h * 0.16f), strokeWidth = stroke.width * 0.7f, cap = StrokeCap.Round)
            }
            GlyphKind.Eye, GlyphKind.Hide -> {
                drawOval(tint, topLeft = Offset(w * 0.16f, h * 0.32f), size = Size(w * 0.68f, h * 0.36f), style = stroke)
                drawCircle(tint, radius = w * 0.1f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                if (kind == GlyphKind.Hide) {
                    drawLine(tint, Offset(w * 0.22f, h * 0.78f), Offset(w * 0.78f, h * 0.22f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
            }
            GlyphKind.Filter -> {
                val path = Path().apply {
                    moveTo(w * 0.2f, h * 0.25f)
                    lineTo(w * 0.8f, h * 0.25f)
                    lineTo(w * 0.58f, h * 0.52f)
                    lineTo(w * 0.58f, h * 0.78f)
                    lineTo(w * 0.42f, h * 0.68f)
                    lineTo(w * 0.42f, h * 0.52f)
                    close()
                }
                drawPath(path, tint, style = stroke)
            }
            GlyphKind.Info -> {
                drawCircle(
                    tint,
                    radius = w * 0.38f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = stroke
                )
                drawCircle(tint, radius = w * 0.045f, center = Offset(w * 0.5f, h * 0.32f))
                drawLine(
                    tint,
                    Offset(w * 0.5f, h * 0.47f),
                    Offset(w * 0.5f, h * 0.7f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
            }
            GlyphKind.List, GlyphKind.Playlist -> {
                repeat(3) { index ->
                    val y = h * (0.3f + index * 0.22f)
                    drawCircle(tint, radius = w * 0.035f, center = Offset(w * 0.24f, y))
                    drawLine(tint, Offset(w * 0.36f, y), Offset(w * 0.76f, y), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
            }
            GlyphKind.Menu -> {
                repeat(3) { index ->
                    val y = h * (0.28f + index * 0.22f)
                    drawLine(tint, Offset(w * 0.2f, y), Offset(w * 0.8f, y), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
            }
            GlyphKind.Mic -> {
                drawRoundRect(tint, topLeft = Offset(w * 0.38f, h * 0.18f), size = Size(w * 0.24f, h * 0.44f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f), style = stroke)
                drawArc(tint, 0f, 180f, false, topLeft = Offset(w * 0.25f, h * 0.4f), size = Size(w * 0.5f, h * 0.38f), style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.78f), Offset(w * 0.5f, h * 0.9f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GlyphKind.More -> {
                repeat(3) { index ->
                    drawCircle(tint, radius = w * 0.045f, center = Offset(w * 0.5f, h * (0.28f + index * 0.22f)))
                }
            }
            GlyphKind.Plus -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.22f), Offset(w * 0.5f, h * 0.78f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.22f, h * 0.5f), Offset(w * 0.78f, h * 0.5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GlyphKind.Screen -> {
                drawRoundRect(tint, topLeft = Offset(w * 0.16f, h * 0.24f), size = Size(w * 0.68f, h * 0.48f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f), style = stroke)
                drawLine(tint, Offset(w * 0.42f, h * 0.82f), Offset(w * 0.58f, h * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GlyphKind.Search -> {
                drawCircle(tint, radius = w * 0.25f, center = Offset(w * 0.43f, h * 0.43f), style = stroke)
                drawLine(tint, Offset(w * 0.62f, h * 0.62f), Offset(w * 0.82f, h * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GlyphKind.Settings -> {
                drawCircle(tint, radius = w * 0.26f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                repeat(6) { index ->
                    val angle = Math.toRadians((index * 60).toDouble())
                    val start = Offset((w * 0.5f + kotlin.math.cos(angle).toFloat() * w * 0.34f), (h * 0.5f + kotlin.math.sin(angle).toFloat() * h * 0.34f))
                    val end = Offset((w * 0.5f + kotlin.math.cos(angle).toFloat() * w * 0.42f), (h * 0.5f + kotlin.math.sin(angle).toFloat() * h * 0.42f))
                    drawLine(tint, start, end, strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
            }
            GlyphKind.Star -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.16f)
                    lineTo(w * 0.61f, h * 0.39f)
                    lineTo(w * 0.86f, h * 0.42f)
                    lineTo(w * 0.68f, h * 0.59f)
                    lineTo(w * 0.73f, h * 0.84f)
                    lineTo(w * 0.5f, h * 0.71f)
                    lineTo(w * 0.27f, h * 0.84f)
                    lineTo(w * 0.32f, h * 0.59f)
                    lineTo(w * 0.14f, h * 0.42f)
                    lineTo(w * 0.39f, h * 0.39f)
                    close()
                }
                drawPath(path, tint, style = stroke)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050A0F)
@Composable
private fun StreamHubPreview() {
    IPTVAppTheme {
        StreamHubApp()
    }
}
