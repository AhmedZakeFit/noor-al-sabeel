package io.github.ahmedzakefit.noor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.ahmedzakefit.noor.ui.NoorApp
import io.github.ahmedzakefit.noor.ui.theme.NOORTheme

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Notification permission is requested when the user first lands on the
        // prayer-times screen with alerts enabled (see HomeScreen); the location
        // permission is requested inside the location picker at the moment the
        // user taps "use GPS", so every request happens in context.
        val requestNotificationPermission: () -> Unit = {
            if (Build.VERSION.SDK_INT >= 33) {
                val granted = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            NOORTheme {
                NoorApp(
                    onRequestNotificationPermission = requestNotificationPermission,
                )
            }
        }
    }
}
