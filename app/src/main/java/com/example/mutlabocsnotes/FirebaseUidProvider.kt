package com.example.mutlabocsnotes

import com.google.firebase.auth.FirebaseAuth

/**
 * Пока FirebaseAuth остаётся только источником firebase uid,
 * который прокидывается в backend через X-Firebase-Uid.
 */
class FirebaseUidProvider(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun getRequiredUid(): String {
        return firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("Firebase user is not authenticated")
    }

    fun getUidOrNull(): String? = firebaseAuth.currentUser?.uid
}
