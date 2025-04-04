import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.common.reflect.TypeToken
import java.io.File
import com.google.gson.Gson

data class VideoStatus(
    val fileName: String,
    var isLiked: Boolean = false,
    var isSaved: Boolean = false,
    var isUploaded: Boolean = false
)
private fun getStatusFile(context: Context): File {
    return File(context.filesDir, "videos/status.json")
}

private val videoStatusCache = mutableMapOf<String, List<VideoStatus>>()

fun loadVideoStatuses(context: Context): List<VideoStatus> {
    return videoStatusCache.getOrPut("status") {
        // Original loading code
        val jsonFile = File(context.filesDir, "videos/status.json")
        if (jsonFile.exists()) {
            Gson().fromJson(jsonFile.readText(), object : TypeToken<List<VideoStatus>>() {}.type)
                ?: emptyList()
        } else {
            emptyList()
        }
    }
}

private fun saveVideoStatuses(context: Context, statuses: List<VideoStatus>) {
    val statusFile = getStatusFile(context)
    statusFile.parentFile?.mkdirs() // Create directory if needed
    val jsonString = Gson().toJson(statuses)
    statusFile.writeText(jsonString)
}

fun markVideoAsSaved(context: Context, fileName: String) {
    try {
        // Load existing statuses
        val statuses = loadVideoStatuses(context).toMutableList()

        // Find or create entry for this video
        val existingStatus = statuses.find { it.fileName == fileName }
        if (existingStatus != null) {
            // Update existing entry
            if (!existingStatus.isSaved) {
                existingStatus.isSaved = true
                Toast.makeText(context, "Video saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Video was already saved", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Create new entry
            statuses.add(VideoStatus(fileName, isSaved = true))
            Toast.makeText(context, "Video saved!", Toast.LENGTH_SHORT).show()
        }

        // Save back to file
        saveVideoStatuses(context, statuses)

    } catch (e: Exception) {
        Log.e("VideoStatus", "Error updating status", e)
    }
}

fun initVideoStatusFile(context: Context) {
    val statusFile = File(context.filesDir, "videos/status.json")
    if (!statusFile.exists()) {
        // Create default statuses for all videos in assets
        val videoFiles = context.assets.list("videos")?.filter { it.endsWith(".mp4") } ?: emptyList()

        val defaultStatuses = listOf(
            VideoStatus("Video1.mp4", isLiked = true, isSaved = false, isUploaded = false),
            VideoStatus("Video2.mp4", isLiked = true, isSaved = true, isUploaded = false),
            VideoStatus("Video3.mp4", isLiked = false, isSaved = false, isUploaded = true),
            VideoStatus("Video4.mp4", isLiked = true, isSaved = true, isUploaded = true),
            VideoStatus("Video5.mp4", isLiked = false, isSaved = false, isUploaded = false),
            VideoStatus("Video6.mp4", isLiked = true, isSaved = true, isUploaded = false),
            VideoStatus("Video7.mp4", isLiked = false, isSaved = false, isUploaded = true),
            VideoStatus("Video8.mp4", isLiked = true, isSaved = true, isUploaded = true),
            VideoStatus("Video9.mp4", isLiked = false, isSaved = false, isUploaded = false),
            VideoStatus("Video10.mp4", isLiked = true, isSaved = true, isUploaded = false),
            VideoStatus("Video11.mp4", isLiked = true, isSaved = false, isUploaded = true),
            VideoStatus("Video12.mp4", isLiked = false, isSaved = true, isUploaded = true)
        )

        // Only include statuses for videos that actually exist
        val validStatuses = defaultStatuses.filter { it.fileName in videoFiles }
        saveVideoStatuses(context, validStatuses)
    }
}