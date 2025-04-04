package com.example.camera2api

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import android.util.Log
import org.json.JSONObject

class FirebaseHelper {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    fun fetchResultFromFirebase(filePath: String, callback: (JSONObject?) -> Unit) {
        val fileRef: StorageReference = storageRef.child(filePath)

        fileRef.getStream().addOnSuccessListener { taskSnapshot ->
            try {
                taskSnapshot.stream.use { inputStream ->
                    val jsonContent = inputStream.bufferedReader().use { it.readText() }

                    if (jsonContent.isEmpty()) {
                        Log.e("FirebaseHelper", "❌ JSON file is empty")
                        callback(null)
                        return@addOnSuccessListener
                    }

                    // Convert to JSON Object
                    val jsonObject = JSONObject(jsonContent)
                    Log.d("FirebaseHelper", "✅ Detection Results: $jsonObject")
                    callback(jsonObject)
                }
            } catch (e: Exception) {
                Log.e("FirebaseHelper", "❌ Error parsing JSON", e)
                callback(null)
            }
        }.addOnFailureListener { exception ->
            Log.e("FirebaseHelper", "❌ Failed to fetch file from Firebase", exception)
            callback(null)
        }
    }
}
