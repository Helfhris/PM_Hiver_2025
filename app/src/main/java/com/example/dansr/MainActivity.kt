package com.example.dansr

import UsageTracker
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.dansr.ui.theme.DansRTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var usageTracker: UsageTracker
    private var startTime: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val checkUsageRunnable = object : Runnable {
        override fun run() {
            val elapsedTime = System.currentTimeMillis() - startTime
            usageTracker.updateUsage(elapsedTime) // Update stored usage time
            startTime = System.currentTimeMillis() // Reset start time
            val usedTime = usageTracker.getTodayUsage()

            if (usedTime >= 30 * 60 * 1000) { // 30 minutes in milliseconds
                Toast.makeText(this@MainActivity, "Daily limit reached!", Toast.LENGTH_LONG).show()
                finishAffinity() // Close the app
            } else {
                Log.d("UsageTracker", "Current usage time: $usedTime ms") // Log current usage time
                handler.postDelayed(this, 1000) // Check every second
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DansRTheme {
                DansRApp()
            }
        }
        usageTracker = UsageTracker(this)
    }


    override fun onResume() {
        super.onResume()
        startTime = System.currentTimeMillis()
        handler.post(checkUsageRunnable) // Start checking usage
    }

    override fun onPause() {
        super.onPause()
        val elapsedTime = System.currentTimeMillis() - startTime
        usageTracker.updateUsage(elapsedTime)
        handler.removeCallbacks(checkUsageRunnable) // Stop checking when paused
    }
}

