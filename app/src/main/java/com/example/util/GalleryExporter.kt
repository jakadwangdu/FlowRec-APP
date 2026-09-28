package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object GalleryExporter {
    private const val TAG = "GalleryExporter"

    /**
     * Saves a recorded video file directly to the device's public Gallery (Movies/FlowRec).
     * Automatically triggers Android MediaScanner so Samsung Gallery, Google Photos, etc.
     * instantly index and show the video.
     */
    fun saveVideoToGallery(
        context: Context,
        sourceVideoFile: File,
        displayName: String
    ): Uri? {
        if (!sourceVideoFile.exists() || sourceVideoFile.length() == 0L) {
            Log.e(TAG, "Source video file does not exist or is empty: ${sourceVideoFile.absolutePath}")
            return null
        }

        val cleanName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val finalFileName = if (cleanName.endsWith(".mp4", ignoreCase = true)) cleanName else "$cleanName.mp4"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.TITLE, cleanName.removeSuffix(".mp4"))
                    put(MediaStore.Video.Media.DISPLAY_NAME, finalFileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/FlowRec")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                    put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                }

                val resolver = context.contentResolver
                val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = resolver.insert(collection, values)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        FileInputStream(sourceVideoFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)

                    Log.d(TAG, "Successfully exported video to MediaStore: $uri")
                    return uri
                }
            } else {
                // Android 9 and lower: write to public Movies directory
                @Suppress("DEPRECATION")
                val moviesDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "FlowRec"
                )
                if (!moviesDir.exists()) {
                    moviesDir.mkdirs()
                }

                val targetFile = File(moviesDir, finalFileName)
                FileInputStream(sourceVideoFile).use { inStream ->
                    FileOutputStream(targetFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }

                var resultUri: Uri? = null
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf("video/mp4")
                ) { _, uri ->
                    resultUri = uri
                }

                return resultUri ?: Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving video to gallery: ${e.message}", e)
        }

        return null
    }

    /**
     * Opens the exported video in the phone's native Gallery / Video player
     */
    fun openVideoInGallery(context: Context, videoPath: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri: Uri = if (videoPath.startsWith("content://")) {
                Uri.parse(videoPath)
            } else {
                val file = File(videoPath)
                if (file.exists()) {
                    try {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } catch (e: Exception) {
                        Uri.fromFile(file)
                    }
                } else {
                    Uri.parse(videoPath)
                }
            }

            intent.setDataAndType(uri, "video/mp4")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(Intent.createChooser(intent, "Play Video"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open video player: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares the exported video file with other apps (WhatsApp, Instagram, Drive, etc.)
     */
    fun shareVideo(context: Context, videoPath: String, title: String = "FlowRec Recording") {
        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "video/mp4"

            val uri: Uri = if (videoPath.startsWith("content://")) {
                Uri.parse(videoPath)
            } else {
                val file = File(videoPath)
                if (file.exists()) {
                    try {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } catch (e: Exception) {
                        Uri.fromFile(file)
                    }
                } else {
                    Uri.parse(videoPath)
                }
            }

            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.putExtra(Intent.EXTRA_SUBJECT, title)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            context.startActivity(Intent.createChooser(intent, "Share Recording"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
