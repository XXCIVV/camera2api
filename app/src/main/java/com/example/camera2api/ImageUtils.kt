package com.example.camera2api

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody

object ImageUtils {
    fun bitmapToMultipart(bitmap: Bitmap): MultipartBody.Part {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        val requestBody = RequestBody.create("image/jpeg".toMediaTypeOrNull(), byteArray)
        return MultipartBody.Part.createFormData("file", "image.jpg", requestBody)
    }
}
