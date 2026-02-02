package com.example.mutlabocsnotes

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class HomeInfoRepository {
    private val db = Firebase.firestore

    private fun userHomeCardsCollection(): CollectionReference? {
        return FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).collection("home_cards")
        }
    }

    suspend fun getAllCards(): List<HomeInfoCard> {
        val collection = userHomeCardsCollection() ?: return emptyList()
        return try {
            val snapshot = collection.get().await()
            snapshot.documents.map { doc ->
                val title = doc.getString("title") ?: ""
                val sectionName = doc.getString("section")
                val section = HomeSection.values().firstOrNull() { it.name == sectionName}
                    ?: HomeSection.OTHER
                val fields = (doc.get("fields") as? List<*>)
                    ?.mapNotNull { rawItem ->
                        (rawItem as? Map<*, *>)?.let {itemMap ->
                            val key = itemMap["key"] as? String ?: ""
                            val value = itemMap["value"] as? String ?: ""
                            HomeField(key = key, value = value)
                        }
                    } ?: emptyList()

                val note = doc.getString("note") ?: ""
                val links = (doc.get("links") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?: emptyList()
                val  createdAt = doc.getLong("createdAt") ?: 0L
                val updatedAt = doc.getLong("updatedAt") ?: 0L
                HomeInfoCard(
                    id = doc.id,
                    title = title,
                    section = section,
                    fields = fields,
                    note = note,
                    links = links,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun cardMap(card: HomeInfoCard): Map<String, Any?> = mapOf(
        "title" to card.title,
        "section" to card.section.name,
        "fields" to card.fields.map { field ->
            mapOf(
                "key" to field.key,
                "value" to field.value
            )
        },
        "note" to card.note,
        "links" to card.links,
        "createdAt" to card.createdAt,
        "updatedAt" to card.updatedAt
    )

    suspend fun insert(card: HomeInfoCard): String? {
        val collection = userHomeCardsCollection() ?: return null
        return try {
            val doc = collection.document()
            doc.set(cardMap(card)).await()
            doc.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun update(card: HomeInfoCard): Boolean {
        val collection = userHomeCardsCollection() ?: return false
        if (card.id.isEmpty()) return false
        return try {
            collection.document(card.id)
                .set(cardMap(card)).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun delete(cardId: String): Boolean {
        val collection = userHomeCardsCollection() ?: return false
        return try {
            collection.document(cardId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
