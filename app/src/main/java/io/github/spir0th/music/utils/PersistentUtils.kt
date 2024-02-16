package io.github.spir0th.music.utils

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import java.io.File

fun Context.generatePersistentUri(uri: Uri): Uri {
    val dir = File(dataDir, "persistence")
    val noMedia = File(dir, ".nomedia")
    val original = contentResolver.openInputStream(uri)
    val cached = uri.lastPathSegment?.let { File(dir, it) }

    dir.mkdirs()
    noMedia.createNewFile()

    original.use { input ->
        cached?.outputStream().use { output ->
            output?.let { input?.copyTo(it) } ?: 0
        }
    }

    original?.close()
    return cached?.toUri() ?: Uri.EMPTY
}

fun Context.cleanPersistentUris() {
    File(dataDir, "persistence").deleteRecursively()
}