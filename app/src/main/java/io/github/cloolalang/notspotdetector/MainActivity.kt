package io.github.cloolalang.notspotdetector

import android.content.Intent
import android.media.AudioManager
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import io.github.cloolalang.notspotdetector.data.SettingsProfilesRepository
import io.github.cloolalang.notspotdetector.model.ProfileImportResult
import io.github.cloolalang.notspotdetector.ui.MonitorApp
import io.github.cloolalang.notspotdetector.ui.theme.NotspotDetectorTheme
import io.github.cloolalang.notspotdetector.util.BackgroundHelper
import io.github.cloolalang.notspotdetector.viewmodel.MonitorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MonitorViewModel by viewModels()

    private var pendingImportCallback: ((ProfileImportResult) -> Unit)? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* proceed regardless; notification may be denied on API 33+ */ }

    private val signalPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.refreshCellularSignal() }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshCellularSignal() }

    private val importProfileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val callback = pendingImportCallback
        pendingImportCallback = null
        if (callback == null) return@registerForActivityResult
        val result = if (uri == null) {
            ProfileImportResult.InvalidFile
        } else {
            viewModel.importSettingsProfile(uri)
        }
        callback(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        requestSignalPermissionsIfNeeded()

        setContent {
            NotspotDetectorTheme {
                MonitorApp(
                    viewModel = viewModel,
                    onRequestBatteryExemption = {
                        startActivity(
                            BackgroundHelper.buildBatteryOptimizationIntent(this@MainActivity)
                        )
                    },
                    onRequestCellIdentityPermission = ::requestCellIdentityPermission,
                    onImportSettingsProfile = { onResult ->
                        pendingImportCallback = onResult
                        importProfileLauncher.launch(
                            arrayOf(
                                SettingsProfilesRepository.PROFILE_MIME_TYPE,
                                "application/*",
                                "text/*",
                                "*/*"
                            )
                        )
                    },
                    onShareSettingsProfile = ::shareSettingsProfile
                )
            }
        }
    }

    private fun shareSettingsProfile(profileId: String) {
        val file = viewModel.exportSettingsProfile(profileId) ?: return
        val label = viewModel.profileShareLabel(profileId) ?: profileId
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = SettingsProfilesRepository.PROFILE_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, label)
            putExtra(Intent.EXTRA_TITLE, label)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri(label, uri)
        }
        startActivity(
            Intent.createChooser(
                shareIntent,
                getString(R.string.settings_profiles_share_chooser)
            )
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestSignalPermissionsIfNeeded() {
        val permissions = buildList {
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.READ_PHONE_STATE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        if (permissions.isNotEmpty()) {
            signalPermissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun requestCellIdentityPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}
