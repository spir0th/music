package io.github.spir0th.music.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import java.io.File

object MediaUtils {
    @JvmStatic fun generateMediaPersistence(context: Context, uri: Uri?): Uri {
        val dir = File(context.dataDir, "persistence")
        val noMedia = File(dir, ".nomedia")
        val original = context.contentResolver.openInputStream(uri!!)!!
        val cached = uri.lastPathSegment?.let { File(dir, it) }!!

        dir.mkdirs()
        noMedia.createNewFile()

        original.use { input ->
            cached.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        original.close()
        return cached.toUri()
    }

    @JvmStatic fun cleanMediaPersists(context: Context) {
        File(context.dataDir, "persistence").deleteRecursively()
    }

    @SuppressLint("Range")
    @JvmStatic fun getFilenameFromUri(uri: Uri?): String? {
        return uri?.lastPathSegment
    }

    @JvmStatic fun getTitleFromUri(context: Context, uri: Uri?): String? {
        val metadataRetriever = MediaMetadataRetriever()

        return try {
            metadataRetriever.setDataSource(context, uri)
            metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        } catch (_: Exception) {
            null
        }
    }

    @JvmStatic fun getArtistFromUri(context: Context, uri: Uri?): String? {
        val metadataRetriever = MediaMetadataRetriever()

        return try {
            metadataRetriever.setDataSource(context, uri)
            metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        } catch (_: Exception) {
            null
        }
    }

    @JvmStatic fun getCoverArtFromUri(context: Context, uri: Uri?): Bitmap? {
        val metadataRetriever = MediaMetadataRetriever()

        val artData = try {
            metadataRetriever.setDataSource(context, uri)
            metadataRetriever.embeddedPicture!!
        } catch (_: Exception) {
            ByteArray(1)
        }

        return BitmapFactory.decodeByteArray(artData, 0, artData.size)
    }
}