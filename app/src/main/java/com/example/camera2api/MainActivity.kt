package com.example.camera2api

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import android.hardware.camera2.CameraManager
import android.util.Log
import android.view.TextureView
import androidx.annotation.RequiresPermission
import androidx.core.graphics.drawable.toBitmap
import com.example.camera2api.ui.theme.ProcessedImageResponse

class MainActivity : AppCompatActivity() {
    private lateinit var textureView: TextureView
    private lateinit var imageView: ImageView
    private lateinit var captureButton: Button
    private lateinit var cameraHandler: CameraHandler
    private lateinit var firebaseHelper: FirebaseHelper

    @RequiresPermission(Manifest.permission.CAMERA)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.textureView)
        imageView = findViewById(R.id.imageView)
        captureButton = findViewById(R.id.captureButton)

        firebaseHelper = FirebaseHelper()

        cameraHandler = CameraHandler(
            cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager,
            textureView = textureView,
            imageView = imageView,
            captureButtonClickListener = { captureImage() } // Pass the captureImage function here
        )

        cameraHandler.setupCamera()
    }

    private fun captureImage() {
        // Upload image to server
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = imageView.drawable.toBitmap()
                val imagePart = ImageUtils.bitmapToMultipart(bitmap)
                val response: Response<ProcessedImageResponse> = RetrofitClient.apiService.uploadImage(imagePart)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        val firebaseUrl = responseBody?.firebase_url

                        // Log full response for debugging
                        Log.d("ServerResponse", "Full Response: $responseBody")

                        responseBody?.let {
                            // Log detections data from responseBody
                            Log.d("ServerResponse", "Detections: ${it.detection_data.detections}")

                            // Fetch detection result from Firebase
                            if (firebaseUrl != null) {
                                firebaseHelper.fetchResultFromFirebase(firebaseUrl) { jsonResult ->
                                    if (jsonResult != null) {
                                        // Process and display the detections
                                        Log.d("MainActivity", "✅ Detection Result: $jsonResult")
                                    } else {
                                        Log.e("MainActivity", "❌ Failed to fetch JSON from Firebase")
                                    }
                                }
                            } else {
                                Log.e("MainActivity", "❌ Firebase URL is null")
                            }
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("ServerResponse", "Failed: HTTP ${response.code()} - $errorBody")
                    }
                }
            } catch (e: Exception) {
                Log.e("ServerResponse", "Error: ${e.message}", e)
            }
        }
    }
}
