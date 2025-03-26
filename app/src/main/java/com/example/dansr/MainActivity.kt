package com.example.dansr

import com.example.dansr.preferences.UsageTracker
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.dansr.ui.theme.DansRTheme

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
                Log.d("com.example.dansr.preferences.UsageTracker", "Current usage time: $usedTime ms") // Log current usage time
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

