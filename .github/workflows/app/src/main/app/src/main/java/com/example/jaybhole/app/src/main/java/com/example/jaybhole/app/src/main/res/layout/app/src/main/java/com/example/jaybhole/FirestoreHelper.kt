package com.example.jaybhole

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.*

object FirestoreHelper {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveReport(location: String, text: String) {
        val userId = auth.currentUser?.uid ?: auth.signInAnonymously().await().user?.uid ?: return
        val data = mapOf(
            "reportText" to text,
            "location" to location,
            "date" to java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("hi", "IN")).format(Date()),
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        db.collection("reports")
            .document(userId)
            .collection("user_reports")
            .document(UUID.randomUUID().toString())
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun getReports(): List<Map<String, Any>> {
        val userId = auth.currentUser?.uid ?: auth.signInAnonymously().await().user?.uid ?: return emptyList()
        val snap = db.collection("reports")
            .document(userId)
            .collection("user_reports")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()

        return snap.documents.mapNotNull { it.data }
    }
}
