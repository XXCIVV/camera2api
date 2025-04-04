package com.example.camera2api

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

object ImageUtils {
    fun bitmapToMultipart(bitmap: Bitmap): MultipartBody.Part {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()


        val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())

        return MultipartBody.Part.createFormData("file", "image.jpg", requestBody)
    }
}
