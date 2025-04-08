package com.example.camera2api

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import android.util.Log
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseHelper {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    // Fetch result from Firebase and return as a JSONObject
    suspend fun fetchResultFromFirebase(filePath: String): JSONObject? {
        return withContext(Dispatchers.IO) {
            val fileRef: StorageReference = storageRef.child(filePath)
            try {
                val taskSnapshot = fileRef.getStream().await()
                taskSnapshot.stream.use { inputStream ->
                    val jsonContent = inputStream.bufferedReader().use { it.readText() }

                    if (jsonContent.isEmpty()) {
                        Log.e("FirebaseHelper", "❌ JSON file is empty")
                        return@withContext null
                    }

                    // Convert to JSON Object
                    val jsonObject = JSONObject(jsonContent)
                    Log.d("FirebaseHelper", "✅ Detection Results: $jsonObject")
                    return@withContext jsonObject
                }
            } catch (e: Exception) {
                Log.e("FirebaseHelper", "❌ Error fetching or parsing JSON from Firebase", e)
                return@withContext null
            }
        }
    }
}
