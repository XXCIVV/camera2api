package com.example.camera2api

import android.Manifest
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import android.hardware.camera2.CameraManager
import android.util.Log
import android.view.TextureView
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camera2api.ui.theme.ProcessedImageResponse
import org.opencv.android.OpenCVLoader


class MainActivity : AppCompatActivity() {
    private lateinit var textureView: TextureView
    private lateinit var imageView: ImageView
    private lateinit var captureButton: Button
    private lateinit var cameraHandler: CameraHandler
    private val firebaseHelper = FirebaseHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed.")
            Toast.makeText(this, "❌ OpenCV failed to initialize", Toast.LENGTH_LONG).show()
        } else {
            Log.d("OpenCV", "OpenCV initialized successfully.")
        }

        textureView = findViewById(R.id.textureView)
        imageView = findViewById(R.id.imageView)
        captureButton = findViewById(R.id.captureButton)

        // Request camera permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        } else {
            setupCamera()
        }

        captureButton.setOnClickListener {
            cameraHandler.captureImage()
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private fun setupCamera() {
        cameraHandler = CameraHandler(
            cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager,
            textureView = textureView,
            imageView = imageView,
            onImageCaptured = { bitmap ->
                uploadImageToServer(bitmap)
            }
        )
        cameraHandler.setupCamera()
    }

    private fun uploadImageToServer(bitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val imagePart = ImageUtils.bitmapToMultipart(bitmap)
                val response: Response<ProcessedImageResponse> = RetrofitClient.apiService.uploadImage(imagePart)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        Log.d("ServerResponse", "✅ Image uploaded. Response: $responseBody")

                        val firebaseUrl = responseBody?.firebase_url

                        // If detection data exists, draw bounding boxes
                        if (responseBody != null) {
                            val mat = org.opencv.core.Mat()
                            org.opencv.android.Utils.bitmapToMat(bitmap, mat)
                            ImageUtils.drawDetections(mat, responseBody)
                            val resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
                            org.opencv.android.Utils.matToBitmap(mat, resultBitmap)
                            imageView.setImageBitmap(resultBitmap)
                        }

                        if (firebaseUrl != null) {
                            fetchDetectionResults(firebaseUrl)
                        } else {
                            Log.e("ServerResponse", "❌ Firebase URL is null")
                            Toast.makeText(this@MainActivity, "❌ Firebase URL is missing", Toast.LENGTH_LONG).show()
                        }

                    } else {
                        Log.e("ServerResponse", "❌ Upload failed: ${response.errorBody()?.string()}")
                        Toast.makeText(this@MainActivity, "❌ Upload failed", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ServerResponse", "❌ Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "❌ Error uploading image", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Fetch detection results from Firebase
    private fun fetchDetectionResults(firebaseUrl: String) {
        // For now, we'll just log the URL and show a message
        Log.d("MainActivity", "✅ Firebase URL: $firebaseUrl")
        Toast.makeText(this, "Detection data URL: $firebaseUrl", Toast.LENGTH_LONG).show()
    }

    // Handle permission result
    @RequiresPermission(Manifest.permission.CAMERA)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera()
            } else {
                Toast.makeText(this, "❌ Camera permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }
}
