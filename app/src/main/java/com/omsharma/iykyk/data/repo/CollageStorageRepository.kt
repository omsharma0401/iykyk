package com.omsharma.iykyk.data.repo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

class CollageStorageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // Callers on API 26-28 must hold WRITE_EXTERNAL_STORAGE before calling this - on
    // API 29+ (scoped storage) MediaStore inserts don't need it at all.
    suspend fun saveToGallery(bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val uriString = MediaStore.Images.Media.insertImage(
            context.contentResolver,
            bitmap,
            "iykyk_collage_${System.currentTimeMillis()}",
            "Face collage created by IYKYK"
        ) ?: throw IOException("Failed to save the collage to the gallery")
        uriString.toUri()
    }

    suspend fun createShareIntent(bitmap: Bitmap): Intent = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "share_collage_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
