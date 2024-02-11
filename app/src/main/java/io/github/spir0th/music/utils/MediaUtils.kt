package io.github.spir0th.music.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri

object MediaUtils {
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