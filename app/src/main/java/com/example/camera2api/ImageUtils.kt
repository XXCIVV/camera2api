package com.example.camera2api

import android.graphics.Bitmap
import com.example.camera2api.ui.theme.ProcessedImageResponse
import java.io.ByteArrayOutputStream
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.opencv.android.CameraBridgeViewBase
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

private lateinit var openCvCameraView: CameraBridgeViewBase
private lateinit var inputMat: Mat
private lateinit var processedMat: Mat

object ImageUtils {

    fun bitmapToMultipart(bitmap: Bitmap): MultipartBody.Part {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("file", "image.jpg", requestBody)
    }

    private fun drawBoundingBox(mat: Mat, x: Int, y: Int, width: Int, height: Int, label: String? = null) {
        val topLeft = Point(x.toDouble(), y.toDouble())
        val bottomRight = Point((x + width).toDouble(), (y + height).toDouble())

        // Scale thickness and font size based on image size
        val thickness = (mat.width() / 200).coerceAtLeast(3)
        val fontScale = (mat.width() / 800.0).coerceAtLeast(1.0)
        val fontThickness = (thickness / 2).coerceAtLeast(1)

        // Draw bounding rectangle
        Imgproc.rectangle(mat, topLeft, bottomRight, Scalar(0.0, 255.0, 0.0), thickness)

        // Draw label with background
        label?.let {
            val labelPosition = Point(x.toDouble(), y.toDouble() - 10)
            val textSize = Imgproc.getTextSize(label, Imgproc.FONT_HERSHEY_SIMPLEX, fontScale, fontThickness, null)

            val bgTopLeft = Point(labelPosition.x, labelPosition.y - textSize.height)
            val bgBottomRight = Point(labelPosition.x + textSize.width, labelPosition.y + 5.0)

            // Draw green background rectangle for label
            Imgproc.rectangle(mat, bgTopLeft, bgBottomRight, Scalar(0.0, 255.0, 0.0), Imgproc.FILLED)

            // Draw label text in white
            Imgproc.putText(
                mat, label, labelPosition,
                Imgproc.FONT_HERSHEY_SIMPLEX, fontScale, Scalar(255.0, 255.0, 255.0), fontThickness
            )
        }
    }

    fun drawDetections(mat: Mat, response: ProcessedImageResponse) {
        for (detection in response.detection_data.detections) {
            val bbox = detection.bbox
            if (bbox.size == 4) {
                val x1 = bbox[0].toInt()
                val y1 = bbox[1].toInt()
                val x2 = bbox[2].toInt()
                val y2 = bbox[3].toInt()
                val width = x2 - x1
                val height = y2 - y1
                val label = "${detection.`class`} ${(detection.confidence * 100).toInt()}%"
                drawBoundingBox(mat, x1, y1, width, height, label)
            }
        }
    }
}
