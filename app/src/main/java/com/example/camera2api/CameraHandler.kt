package com.example.camera2api

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import android.hardware.camera2.*
import android.media.ImageReader
import android.widget.ImageView

class CameraHandler(
    private val cameraManager: CameraManager,
    private val textureView: TextureView,
    private val imageView: ImageView,
    private val captureButtonClickListener: () -> Unit // Capture button click listener
) {

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var imageReader: ImageReader
    private lateinit var cameraId: String

    @RequiresPermission(Manifest.permission.CAMERA)
    fun setupCamera() {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            @RequiresPermission(Manifest.permission.CAMERA)
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        // Set up the capture button click listener to call captureImage()
        captureButtonClickListener.invoke()
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private fun openCamera() {
        try {
            cameraId = cameraManager.cameraIdList[0]
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val streamConfigMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val previewSize = streamConfigMap?.getOutputSizes(SurfaceTexture::class.java)?.get(0) ?: Size(640, 480)

            if (ActivityCompat.checkSelfPermission(textureView.context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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

        imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 5)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let {
                val bitmap = imageToBitmap(it)
                image.close()

                imageView.post {
                    imageView.setImageBitmap(bitmap)
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

    // Method to capture the image
    fun captureImage() {
        val captureRequest = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
            addTarget(imageReader.surface)
        }
        captureSession?.capture(captureRequest.build(), null, null)
    }

    // Convert image to Bitmap
    private fun imageToBitmap(image: android.media.Image): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
