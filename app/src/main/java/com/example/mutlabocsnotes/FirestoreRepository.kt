package com.example.mutlabocsnotes

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = Firebase.firestore
    private val notesCollection = db.collection( "notes")

    suspend fun getAllNotes(): List<Note> {
        return try {
            val snapshot = notesCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                val title = doc.getString("title")
                val content = doc.getString("content")
                if (title != null && content != null) {
                    Note(id = doc.id, title = title, content = content)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun insert(title: String, content: String): String? {
        return try {
            val doc = notesCollection.document()
            doc.set(mapOf("title" to title, "content" to content)).await()
            doc.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun update(noteId: String, title: String, content: String): Boolean {
        return try {
            notesCollection.document(noteId)
                .set(mapOf("title" to title, "content" to content)).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun delete(noteId: String): Boolean {
        return try {
            notesCollection.document(noteId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

}