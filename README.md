# IPTV-Android-App

Ultra fast native Android IPTV app built with Kotlin and Jetpack Compose.

## Device Test Pass

Run after an emulator or Android device is connected:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat connectedDebugAndroidTest
```

Or run the managed virtual device target after the Android SDK system image is installed:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat pixel6Api35DebugAndroidTest
```

Manual checks:

- App launches to Live TV without waiting on network.
- Bottom navigation opens Guide, Search, Playlists, and Settings.
- Tapping a channel opens the player.
- Previous/Next switches channels inside the player.
- Player diagnostics update startup, rebuffer, switch, and error counters.
- Add Playlist can test XCODES details and reports success/failure.
- Saved playlists can resync and delete.
