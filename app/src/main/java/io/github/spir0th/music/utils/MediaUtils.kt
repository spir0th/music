package io.github.spir0th.music.utils

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import java.io.File

fun Uri.generateMediaPersistence(context: Context): Uri {
    val dir = File(context.dataDir, "persistence")
    val noMedia = File(dir, ".nomedia")
    val original = context.contentResolver.openInputStream(this)!!
    val cached = lastPathSegment?.let { File(dir, it) }!!

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

fun Context.cleanMediaPersists() {
    File(dataDir, "persistence").deleteRecursively()
}