package com.example.camera2api

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.os.*
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.nio.ByteBuffer
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import com.example.camera2api.network.RetrofitClient
import com.example.camera2api.ui.theme.ProcessedImageResponse

class MainActivity : AppCompatActivity() {
    private lateinit var textureView: TextureView
    private lateinit var imageView: ImageView
    private lateinit var captureButton: Button
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var imageReader: ImageReader
    private lateinit var cameraId: String
    private val cameraManager by lazy { getSystemService(CAMERA_SERVICE) as CameraManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.textureView)
        imageView = findViewById(R.id.imageView)
        captureButton = findViewById(R.id.captureButton)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera()
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        captureButton.setOnClickListener {
            captureImage()
        }
    }

    private fun openCamera() {
        try {
            cameraId = cameraManager.cameraIdList[0]
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val streamConfigMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val previewSize = streamConfigMap?.getOutputSizes(SurfaceTexture::class.java)?.get(0) ?: Size(640, 480)

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 100)
                return
            }

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCameraSession(previewSize)
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            Log.e("Camera", "Error opening camera: ${e.message}")
        }
    }

    private fun createCameraSession(previewSize: Size) {
        val surfaceTexture = textureView.surfaceTexture
        surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(surfaceTexture)

        imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let {
                val bitmap = imageToBitmap(it)
                image.close()

                runOnUiThread {
                    imageView.setImageBitmap(bitmap)
                    uploadImage(bitmap)
                }
            }
        }, Handler(Looper.getMainLooper()))

        cameraDevice?.createCaptureSession(
            listOf(previewSurface, imageReader.surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    val captureRequest = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                        addTarget(previewSurface)
                    }
                    session.setRepeatingRequest(captureRequest.build(), null, null)
                }
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e("Camera", "Capture session configuration failed")
                }
            },
            Handler(Looper.getMainLooper())
        )
    }

    private fun captureImage() {
        val captureRequest = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
            addTarget(imageReader.surface)
        }
        captureSession?.capture(captureRequest.build(), null, null)
    }

    private fun imageToBitmap(image: android.media.Image): Bitmap {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun uploadImage(bitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val imagePart = ImageUtils.bitmapToMultipart(bitmap)
                val response: Response<ProcessedImageResponse> = RetrofitClient.apiService.uploadImage(imagePart)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.d("Upload", "Success: ${response.body()?.detections?.size} detections found")

                        // Decode YOLO-processed image from hex string
                        val processedImageBytes = decodeHexToBytes(response.body()?.image ?: "")
                        val processedBitmap = BitmapFactory.decodeByteArray(processedImageBytes, 0, processedImageBytes.size)

                        // Show the YOLO-processed image in the ImageView
                        imageView.setImageBitmap(processedBitmap)
                    } else {
                        Log.e("Upload", "Failed: ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("Upload", "Error: ${e.message}")
            }
        }
    }

    private fun decodeHexToBytes(hexString: String): ByteArray {
        val decoded = ByteArray(hexString.length / 2)
        for (i in decoded.indices) {
            val index = i * 2
            val hex = hexString.substring(index, index + 2)
            decoded[i] = hex.toInt(16).toByte()
        }
        return decoded
    }
}
