package io.github.spir0th.music.utils

import android.content.Context
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
}