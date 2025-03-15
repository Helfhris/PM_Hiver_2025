import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class UsageTracker(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("AppUsagePrefs", Context.MODE_PRIVATE)

    fun getTodayUsage(): Long {
        val lastDate = prefs.getLong("last_used_date", 0L)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        // Reset if it's a new day
        return if (lastDate != today.toLong()) {
            resetUsage()
            0L
        } else {
            prefs.getLong("usage_time", 0L)
        }
    }

    fun updateUsage(timeSpent: Long) {
        val editor = prefs.edit()
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        editor.putLong("last_used_date", today.toLong())
        editor.putLong("usage_time", getTodayUsage() + timeSpent)
        editor.apply()
    }

    private fun resetUsage() {
        val editor = prefs.edit()
        editor.putLong("usage_time", 0L)
        editor.apply()
    }
}
