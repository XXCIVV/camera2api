package com.example.camera2api

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import android.util.Log

class FirebaseHelper {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    fun fetchResultFromFirebase(filePath: String, callback: (String?) -> Unit) {
        val fileRef: StorageReference = storageRef.child(filePath)

        fileRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { data ->
            val result = String(data)
            callback(result)
        }.addOnFailureListener { exception ->
            Log.e("FirebaseHelper", "Failed to fetch file from Firebase", exception)
            callback(null)
        }
    }
}
