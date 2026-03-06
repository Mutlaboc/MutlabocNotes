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
            db.collection("users").document(uid).collection("homeCards")
        }
    }

    suspend fun getAllCards(): Result<List<HomeInfoCard>> {
        val collection = userHomeCardsCollection()
            ?: return Result.failure(IllegalStateException("Пользователь не авторизован"))
        return runCatching  {
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

    suspend fun insert(card: HomeInfoCard): Result<String> {
        val collection = userHomeCardsCollection() ?: return Result.failure(IllegalStateException("Пользователь не авторизован"))
        return runCatching  {
            val doc = collection.document()
            doc.set(cardMap(card)).await()
            doc.id
        }
    }

    suspend fun update(card: HomeInfoCard): Result<Unit> {
        val collection = userHomeCardsCollection()  ?: return Result.failure(IllegalStateException("Пользователь не авторизован"))
        if (card.id.isEmpty()) return Result.failure(IllegalArgumentException("Пустой идентификатор карточки"))
        return runCatching  {
            collection.document(card.id)
                .set(cardMap(card)).await()
                    }
    }

    suspend fun delete(cardId: String): Result<Unit>{
        val collection = userHomeCardsCollection() ?: return Result.failure(IllegalStateException("Пользователь не авторизован"))
        return runCatching  {
            collection.document(cardId).delete().await()
        }
    }
}
