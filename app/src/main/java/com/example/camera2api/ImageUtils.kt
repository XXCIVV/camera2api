package com.example.camera2api

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import org.opencv.android.Utils
import androidx.core.graphics.createBitmap

object ImageUtils {
    fun bitmapToMultipart(bitmap: Bitmap): MultipartBody.Part {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()


        val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())

        return MultipartBody.Part.createFormData("file", "image.jpg", requestBody)
    }

    fun drawBoundingBoxes(capturedBitmap: Bitmap, detectionData: JSONObject): Bitmap {
        // Convert the Bitmap to OpenCV Mat
        val mat = Mat()
        Utils.bitmapToMat(capturedBitmap, mat)

        // Loop through the detection data and draw the bounding boxes
        val detections = detectionData.getJSONArray("detections")
        for (i in 0 until detections.length()) {
            val detection = detections.getJSONObject(i)
            val bbox = detection.getJSONArray("bbox")

            // Get coordinates for bounding box
            val x1 = bbox.getDouble(0).toFloat()  // Use getDouble() and cast to Float
            val y1 = bbox.getDouble(1).toFloat()  // Use getDouble() and cast to Float
            val x2 = bbox.getDouble(2).toFloat()  // Use getDouble() and cast to Float
            val y2 = bbox.getDouble(3).toFloat()  // Use getDouble() and cast to Float

            // Draw the bounding box on the image
            val pt1 = Point(x1.toDouble(), y1.toDouble())
            val pt2 = Point(x2.toDouble(), y2.toDouble())
            Imgproc.rectangle(mat, pt1, pt2, Scalar(255.0, 0.0, 0.0), 3) // Red bounding box
        }

        // Convert the Mat back to Bitmap
        val resultBitmap = createBitmap(mat.cols(), mat.rows())
        Utils.matToBitmap(mat, resultBitmap)

        // Return the Bitmap with bounding boxes drawn
        return resultBitmap
    }
}
