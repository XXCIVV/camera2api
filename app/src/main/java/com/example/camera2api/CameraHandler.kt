package com.example.camera2api

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat

class CameraHandler(
    private val cameraManager: CameraManager,
    private val textureView: TextureView,
    private val imageView: ImageView,
    private val onImageCaptured: (Bitmap) -> Unit
) {

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var imageReader: ImageReader
    private lateinit var cameraId: String

    private val mainHandler = Handler(Looper.getMainLooper())

    @RequiresPermission(Manifest.permission.CAMERA)
    fun setupCamera() {
        Log.d("CameraHandler", "🔧 Setting up camera...")
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            @RequiresPermission(Manifest.permission.CAMERA)
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera()
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private fun openCamera() {
        try {
            cameraId = cameraManager.cameraIdList[0]
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val streamConfigMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val previewSize = streamConfigMap?.getOutputSizes(SurfaceTexture::class.java)?.get(0) ?: Size(640, 480)

            if (ActivityCompat.checkSelfPermission(textureView.context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e("CameraHandler", "❌ Camera permission not granted")
                return
            }

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d("CameraHandler", "✅ Camera opened")
                    cameraDevice = camera
                    createCameraSession(previewSize)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.w("CameraHandler", "⚠ Camera disconnected")
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e("CameraHandler", "❌ Camera error: $error")
                    camera.close()
                }
            }, mainHandler)
        } catch (e: Exception) {
            Log.e("CameraHandler", "❌ Error opening camera: ${e.message}")
        }
    }

    private fun createCameraSession(previewSize: Size) {
        val surfaceTexture = textureView.surfaceTexture
        surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(surfaceTexture)

        imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 5)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let {
                val bitmap = imageToBitmap(it)
                image.close()

                mainHandler.post {
                    Log.d("CameraHandler", "📸 Image captured and displayed")
                    imageView.setImageBitmap(bitmap)
                    onImageCaptured(bitmap)
                }
            }
        }, mainHandler)

        cameraDevice?.createCaptureSession(
            listOf(previewSurface, imageReader.surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    Log.d("CameraHandler", "✅ Camera session configured")
                    captureSession = session
                    val captureRequest = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                        addTarget(previewSurface)
                    }
                    session.setRepeatingRequest(captureRequest.build(), null, mainHandler)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e("CameraHandler", "❌ Capture session configuration failed")
                }
            },
            mainHandler
        )
    }

    fun captureImage() {
        if (cameraDevice == null) {
            Log.e("CameraHandler", "❌ Camera is not initialized")
            return
        }

        try {
            Log.d("CameraHandler", "📷 Capturing image...")
            val captureRequest = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(imageReader.surface)
            }

            captureSession?.capture(captureRequest.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    Log.d("CameraHandler", "✅ Image capture completed")
                }
            }, mainHandler)
        } catch (e: Exception) {
            Log.e("CameraHandler", "❌ Error capturing image: ${e.message}")
        }
    }

    private fun imageToBitmap(image: android.media.Image): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
