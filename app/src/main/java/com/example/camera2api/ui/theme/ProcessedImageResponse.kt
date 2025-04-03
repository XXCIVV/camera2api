package com.example.camera2api.ui.theme

data class ProcessedImageResponse(
    val status: String,
    val detection_data: DetectionData,
    val firebase_storage_path: String,
    val firebase_url: String
)

data class DetectionData(
    val detections: List<Detection>,
    val original_filename: String,
    val timestamp: String
)

data class Detection(
    val `class`: String,
    val confidence: Float,
    val bbox: List<Float> // [x1, y1, x2, y2]
)
