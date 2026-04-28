package moravian.mobileclass.mobilefinal

import android.app.Application
import android.util.Log
import org.osmdroid.config.Configuration

class MobileFinalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize OsmDroid configuration as early as possible.
        // This is required for tile downloads (including DNS resolution) to work reliably.
        val config = Configuration.getInstance()
        config.load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        
        // A descriptive user-agent is required by OpenStreetMap's tile usage policy.
        config.userAgentValue = "${packageName}/1.0 (Android)"
        config.osmdroidBasePath = cacheDir
        config.osmdroidTileCache = cacheDir.resolve("osmdroid/tiles")
        
        // Log configuration for debugging
        Log.d("MobileFinalApp", "OsmDroid initialized:")
        Log.d("MobileFinalApp", "  User Agent: ${config.userAgentValue}")
        Log.d("MobileFinalApp", "  Base Path: ${config.osmdroidBasePath}")
        Log.d("MobileFinalApp", "  Tile Cache: ${config.osmdroidTileCache}")
    }
}

