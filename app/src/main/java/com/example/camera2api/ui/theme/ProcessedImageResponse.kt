package com.example.camera2api.ui.theme

data class ProcessedImageResponse(
    val detections: List<Detection>,
    val image: String // Image in Hex format
)

data class Detection(
    val `class`: String,
    val confidence: Float,
    val bbox: List<Float>
)
