package com.example.camera2api.ui.theme

data class ProcessedImageResponse(
    val status: String,
    val detection_data: DetectionData,
    val firebase_url: String
)

data class DetectionData(
    val detections: List<Detection>,
)

data class Detection(
    val `class`: String,
    val confidence: Float,
    val bbox: List<Float> // [x1, y1, x2, y2]
)
