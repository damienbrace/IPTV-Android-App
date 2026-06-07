package com.example.iptvapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.iptvapp.core.playback.IptvPlayerFactory
import com.example.iptvapp.core.playback.PlaybackTelemetryRecorder
import com.example.iptvapp.core.playback.PlaybackTelemetrySnapshot
import com.example.iptvapp.data.model.Channel
import com.example.iptvapp.data.model.GuideProgram
import com.example.iptvapp.data.model.IptvPlaylist
import com.example.iptvapp.ui.ConnectionTestState
import com.example.iptvapp.ui.MainViewModel
import com.example.iptvapp.ui.PlaylistSaveState
import com.example.iptvapp.ui.theme.IPTVAppTheme
import coil3.compose.AsyncImage

private enum class AppScreen(val title: String) {
    Live("Live TV"),
    Guide("Guide"),
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IPTVAppTheme {
                StreamHubApp()
            }
        }
    }
}

@Composable
private fun StreamHubApp(viewModel: MainViewModel = viewModel()) {
    val homeState by viewModel.homeState.collectAsState()
    val connectionTestState by viewModel.connectionTestState.collectAsState()
    val playlistSaveState by viewModel.playlistSaveState.collectAsState()
    var currentScreen by remember { mutableStateOf(AppScreen.Live) }
    var showAddPlaylist by remember { mutableStateOf(false) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }

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
                            onSelected = { currentScreen = it }
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
                            onChannelSelected = { selectedChannel = it }
                        )
                    } else if (showAddPlaylist) {
                        AddPlaylistScreen(
                            connectionTestState = connectionTestState,
                            playlistSaveState = playlistSaveState,
                            onBack = {
                                viewModel.clearConnectionTest()
                                viewModel.clearPlaylistSaveState()
                                showAddPlaylist = false
                            },
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
                                categories = homeState.categories,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onPlayChannel = { selectedChannel = it }
                            )
                            AppScreen.Guide -> GuideScreen(
                                programs = homeState.guidePrograms,
                                onPlayChannel = { selectedChannel = it }
                            )
                            AppScreen.Search -> SearchScreen(
                                channels = homeState.channels,
                                recentSearches = homeState.recentSearches,
                                onPlayChannel = { selectedChannel = it }
                            )
                            AppScreen.Playlists -> PlaylistsScreen(
                                playlists = homeState.playlists,
                                onAddPlaylist = { showAddPlaylist = true }
                            )
                            AppScreen.Settings -> SettingsScreen()
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
    categories: List<String>,
    onToggleFavorite: (String) -> Unit,
    onPlayChannel: (Channel) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All Channels") }
    val visibleChannels = channels.filter {
        selectedCategory == "All Channels" ||
            selectedCategory == it.category ||
            (selectedCategory == "Favourites" && it.favorite)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(
            title = {
                Text(
                    buildAnnotatedString {
                        append("Stream")
                        withStyle(SpanStyle(color = Accent)) { append("Hub") }
                        append(" TV")
                    },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 0.sp
                )
            },
            actions = {
                GlyphButton(kind = GlyphKind.Bell, onClick = {})
            }
        )

        CategoryTabs(
            categories = categories.ifEmpty { listOf("All Channels") },
            selected = selectedCategory,
            onSelected = { selectedCategory = it },
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 8.dp,
                bottom = 14.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visibleChannels, key = { it.number }) { channel ->
                ChannelRow(
                    channel = channel,
                    showNumber = true,
                    onFavoriteClick = { onToggleFavorite(channel.id) },
                    onPlayClick = { onPlayChannel(channel) }
                )
            }
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
                items(programs, key = { it.channel.number }) { program ->
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
            items(resultChannels.take(6), key = { it.number }) { channel ->
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
    onAddPlaylist: () -> Unit
) {
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
                GlyphButton(kind = GlyphKind.Plus, onClick = onAddPlaylist)
            }
        )

        if (playlists.isNotEmpty()) {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistCard(playlist = playlist)
                }
            }
        } else {
            EmptyPlaylistState(onAddPlaylist = onAddPlaylist)
        }
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
    var playbackState by remember { mutableStateOf(Player.STATE_IDLE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val telemetryRecorder = remember { PlaybackTelemetryRecorder() }
    var telemetry by remember { mutableStateOf(telemetryRecorder.snapshot()) }
    val currentIndex = channels.indexOfFirst { it.id == channel.id }
    val previousChannel = channels.getOrNull(currentIndex - 1)
    val nextChannel = channels.getOrNull(currentIndex + 1)

    val player = remember {
        IptvPlayerFactory(context).createLivePlayer()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackStateValue: Int) {
                playbackState = playbackStateValue
                telemetry = telemetryRecorder.onPlaybackStateChanged(playbackStateValue)
                if (playbackStateValue != Player.STATE_IDLE) {
                    errorMessage = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val message = error.localizedMessage ?: "Unable to play this stream."
                errorMessage = message
                telemetry = telemetryRecorder.onError(message)
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(channel.id) {
        errorMessage = null
        playbackState = Player.STATE_BUFFERING
        telemetry = telemetryRecorder.onChannelLoad(channel.id)
        val mediaItem = IptvPlayerFactory(context).buildLiveMediaItem(
            streamUrl = channel.streamUrl,
            channelId = channel.id,
            channelName = channel.name
        )
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    setUseController(true)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            },
            update = { playerView ->
                playerView.player = player
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .background(Color.Black.copy(alpha = 0.56f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlyphButton(kind = GlyphKind.Back, onClick = onBack)
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PanelSoft.copy(alpha = 0.88f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    "LIVE",
                    color = AccentAlt,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
            }
        }

        PlaybackTelemetryPanel(
            telemetry = telemetry,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 76.dp, start = 14.dp, end = 14.dp)
        )

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

        errorMessage?.let { message ->
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
private fun PlaybackTelemetryPanel(
    telemetry: PlaybackTelemetrySnapshot,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.48f))
            .border(1.dp, Border.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TelemetryValue("Start", telemetry.startupMs?.let { "${it}ms" } ?: "...")
        TelemetryValue("Rebuf", telemetry.rebufferCount.toString())
        TelemetryValue("Switch", telemetry.channelSwitchCount.toString())
        TelemetryValue("Err", telemetry.errorCount.toString())
    }
}

@Composable
private fun TelemetryValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            color = TextMuted,
            fontSize = 10.sp,
            letterSpacing = 0.sp
        )
        Text(
            value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun SettingsScreen() {
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
    }
}

@Composable
private fun CategoryTabs(
    categories: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
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
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!channel.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                channel.logo,
                color = channel.logoColor,
                fontSize = if (channel.logo.length > 3) 14.sp else 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
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
    Row(modifier = modifier.fillMaxWidth()) {
        listOf("7:00pm", "7:30pm", "8:00pm").forEach {
            Text(
                it,
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
private fun PlaylistCard(playlist: IptvPlaylist, modifier: Modifier = Modifier) {
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
            }
            SmallGlyph(kind = GlyphKind.More, tint = TextSecondary)
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

private fun AppScreen.toGlyphKind(): GlyphKind = when (this) {
    AppScreen.Live -> GlyphKind.Screen
    AppScreen.Guide -> GlyphKind.Calendar
    AppScreen.Search -> GlyphKind.Search
    AppScreen.Playlists -> GlyphKind.Playlist
    AppScreen.Settings -> GlyphKind.Settings
}

private enum class GlyphKind {
    Back,
    Bell,
    Calendar,
    Check,
    Chevron,
    Clock,
    Close,
    Eye,
    Filter,
    Hide,
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
private fun GlyphButton(kind: GlyphKind, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
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
