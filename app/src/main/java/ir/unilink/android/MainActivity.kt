package ir.unilink.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ir.unilink.android.ui.AppRoot
import ir.unilink.android.ui.UniLinkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as UniLinkApp
        if (app.store.data.value.settings.autoSignIn) {
            runCatching { KeepAliveService.start(this) }
        }
        setContent {
            UniLinkTheme {
                AppRoot(app)
            }
        }
    }
}
