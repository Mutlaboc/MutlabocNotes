package com.example.mutlabocsnotes

class BridgeUserKeyProvider(
    private val sessionManager: SessionManager
) {
    fun getBridgeUserKeyOrNull(): String? = sessionManager.getBridgeUserKey()

    fun getRequiredBridgeUserKey(): String {
        return getBridgeUserKeyOrNull()
            ?: throw IllegalStateException("Bridge user key is missing in local session")
    }
}