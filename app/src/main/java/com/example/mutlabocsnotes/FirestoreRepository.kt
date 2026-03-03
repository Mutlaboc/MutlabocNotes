package com.example.mutlabocsnotes

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth
class FirestoreRepository {
    private val db = Firebase.firestore

// привязываем заметки к пользователю.
    private fun userNotesCollection() =
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).collection("notes")
        }

    suspend fun getAllNotes(): List<Note> {
        val collection = userNotesCollection() ?: return emptyList()
        return try {
            val snapshot = collection.get().await()
            snapshot.documents.mapNotNull { doc ->
                val title = doc.getString("title") ?: return@mapNotNull null
                val content = doc.getString("content") ?: ""
                val categoryName = doc.getString("category")
                val category = NoteCategory.values().firstOrNull {
                    it.name == categoryName
                }
                    ?: NoteCategory.NOTES
                val checklist = (doc.get("checklist") as? List<*>)
                    ?.mapNotNull {
                        rawItem ->
                        (rawItem as? Map <*, *>)?.let {
                            itemMap ->
                            val text = itemMap["text"] as? String ?: ""
                            val isChecked = itemMap["isChecked"] as? Boolean ?: false
                            CheklistItem(text = text, isChecked = isChecked)
                        }
                    } ?: emptyList()
                val deadlineMillis = doc.getLong("deadlineMillis")
                val isRepeating = doc.getBoolean("isRepeating") ?: false
                val coinCount = doc.getLong("coinCount")?.toInt() ?: 0
                val isCompleted = doc.getBoolean("isCompleted") ?: false
                Note(
                    id = doc.id,
                    title = title,
                    content = content,
                    category = category,
                    checklist = checklist,
                    deadlineMillis = deadlineMillis,
                    isRepeating = isRepeating,
                    coinCount = coinCount,
                    isCompleted = isCompleted
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun noteMap(note: Note): Map<String, Any?> = mapOf(
        "title" to note.title,
        "content" to note.content,
        "category" to note.category.name,
        "checklist" to note.checklist.map {
            item -> mapOf(
                "text" to item.text,
                "isChecked" to item.isChecked
            )
        },
        "deadlineMillis" to note.deadlineMillis,
        "isRepeating" to note.isRepeating,
        "coinCount" to note.coinCount,
        "isCompleted" to note.isCompleted
    )
    suspend fun insert(note: Note): String? {
        val collection = userNotesCollection() ?: return null
        return try {
            val doc = collection.document()
            doc.set(noteMap(note)).await()
            doc.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun update(note: Note): Boolean {
        val collection = userNotesCollection() ?: return false
        if (note.id.isEmpty()) return false
        return try {
            collection.document(note.id)
                .set(noteMap(note)).await()
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

