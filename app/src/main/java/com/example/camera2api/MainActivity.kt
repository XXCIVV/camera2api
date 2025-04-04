package com.example.camera2api

import android.Manifest
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.graphics.Bitmap
import com.example.camera2api.ui.theme.ProcessedImageResponse

class MainActivity : AppCompatActivity() {
    private lateinit var textureView: TextureView
    private lateinit var imageView: ImageView
    private lateinit var captureButton: Button
    private lateinit var cameraHandler: CameraHandler

    @RequiresPermission(Manifest.permission.CAMERA)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                    } else {
                        Log.e("ServerResponse", "❌ Upload failed: ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("ServerResponse", "❌ Error: ${e.message}", e)
            }
        }
    }
}
