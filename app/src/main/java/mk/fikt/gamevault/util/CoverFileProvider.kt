package mk.fikt.gamevault.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object CoverFileProvider {

    private const val AUTHORITY_SUFFIX = ".fileprovider"
    private const val DIR = "covers"

    fun newCoverFile(context: Context): File {
        val dir = File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }
        return File(dir, "cover_${System.currentTimeMillis()}.jpg")
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
}
