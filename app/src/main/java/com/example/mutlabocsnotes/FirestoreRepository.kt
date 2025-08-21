package com.example.mutlabocsnotes

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth
class FirestoreRepository {
    private val db = Firebase.firestore

    /**
     * Returns a reference to the notes collection for the currently
     * authenticated user.  Notes are stored under
     * `users/{uid}/notes` in Firestore so that each user only sees
     * their own notes.
     */
    private fun userNotesCollection() =
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).collection("notes")
        }

    suspend fun getAllNotes(): List<Note> {
        val collection = userNotesCollection() ?: return emptyList()
        return try {
            val snapshot = collection.get().await()
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
        val collection = userNotesCollection() ?: return null
        return try {
            val doc = collection.document()
            doc.set(mapOf("title" to title, "content" to content)).await()
            doc.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun update(noteId: String, title: String, content: String): Boolean {
        val collection = userNotesCollection() ?: return false
        return try {
            collection.document(noteId)
                .set(mapOf("title" to title, "content" to content)).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun delete(noteId: String): Boolean {
        val collection = userNotesCollection() ?: return false
        return try {
            collection.document(noteId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

}