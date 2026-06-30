package com.example.homenotes

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshTokenAuthenticatorTest {

    @Test
    fun authenticate_refreshesOnceAndReusesFreshTokenForSecondExpiredRequest() {
        val server = MockWebServer()
        val sessionStore = FakeAuthenticatorSessionStore().apply {
            saveSession("expired-access", "refresh", "user@example.com")
        }

        try {
            server.enqueue(refreshResponse("fresh-access", "fresh-refresh"))
            server.start()
            val authenticator = RefreshTokenAuthenticator(
                sessionManager = sessionStore,
                baseUrl = server.url("/").toString()
            )

            val firstRetry = authenticator.authenticate(null, unauthorizedResponse("expired-access"))
            val secondRetry = authenticator.authenticate(null, unauthorizedResponse("expired-access"))

            assertEquals("Bearer fresh-access", firstRetry?.header("Authorization"))
            assertEquals("Bearer fresh-access", secondRetry?.header("Authorization"))
            assertEquals(1, server.requestCount)
            assertEquals(1, sessionStore.guardedSaveCalls)
            assertEquals(AuthSessionSnapshot("fresh-access", "fresh-refresh", "user@example.com"), sessionStore.snapshot)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun authenticate_failedRefreshClearsSessionAndEmitsExpiredEvent() = runTest {
        val server = MockWebServer()
        val sessionStore = FakeAuthenticatorSessionStore().apply {
            saveSession("expired-access", "revoked-refresh", "user@example.com")
        }
        val events = mutableListOf<SessionEvent>()
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            SessionEventBus.events.take(1).toList(events)
        }

        try {
            server.enqueue(MockResponse().setResponseCode(401))
            server.start()
            val authenticator = RefreshTokenAuthenticator(
                sessionManager = sessionStore,
                baseUrl = server.url("/").toString()
            )

            val retry = authenticator.authenticate(null, unauthorizedResponse("expired-access"))

            assertNull(retry)
            assertNull(sessionStore.snapshot)
            assertEquals(1, sessionStore.clearCalls)
            assertEquals(listOf(SessionEvent.SessionExpired), events)
        } finally {
            collection.cancel()
            server.shutdown()
        }
    }

    @Test
    fun authenticate_clearDuringRefreshDoesNotResurrectSession() {
        val server = MockWebServer()
        val sessionStore = FakeAuthenticatorSessionStore().apply {
            saveSession("expired-access", "refresh", "user@example.com")
        }

        try {
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    sessionStore.clear()
                    return refreshResponse("fresh-access", "fresh-refresh")
                }
            }
            server.start()
            val authenticator = RefreshTokenAuthenticator(
                sessionManager = sessionStore,
                baseUrl = server.url("/").toString()
            )

            val retry = authenticator.authenticate(null, unauthorizedResponse("expired-access"))

            assertNull(retry)
            assertNull(sessionStore.snapshot)
            assertEquals(1, server.requestCount)
            assertEquals(1, sessionStore.clearCalls)
            assertEquals(1, sessionStore.guardedSaveCalls)
        } finally {
            server.shutdown()
        }
    }

    private fun refreshResponse(accessToken: String, refreshToken: String): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                """
                {
                  "accessToken": "$accessToken",
                  "refreshToken": "$refreshToken"
                }
                """.trimIndent()
            )
    }

    private fun unauthorizedResponse(accessToken: String): Response {
        val request = Request.Builder()
            .url("https://example.test/notes")
            .header("Authorization", "Bearer $accessToken")
            .build()

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()
    }
}

private class FakeAuthenticatorSessionStore : AuthSessionStore {
    private val lock = Any()

    var snapshot: AuthSessionSnapshot? = null
        private set
    var guardedSaveCalls: Int = 0
        private set
    var clearCalls: Int = 0
        private set

    override fun saveSession(accessToken: String, refreshToken: String, email: String?) {
        synchronized(lock) {
            snapshot = AuthSessionSnapshot(accessToken, refreshToken, email)
        }
    }

    override fun getSessionSnapshot(): AuthSessionSnapshot? = synchronized(lock) {
        snapshot
    }

    override fun saveSessionIfRefreshTokenMatches(
        expectedRefreshToken: String,
        accessToken: String,
        refreshToken: String,
        email: String?
    ): Boolean = synchronized(lock) {
        guardedSaveCalls++
        if (snapshot?.refreshToken != expectedRefreshToken) {
            return@synchronized false
        }

        snapshot = AuthSessionSnapshot(accessToken, refreshToken, email)
        true
    }

    override fun clearSessionIfRefreshTokenMatches(expectedRefreshToken: String): Boolean = synchronized(lock) {
        if (snapshot?.refreshToken != expectedRefreshToken) {
            return@synchronized false
        }

        clear()
        true
    }

    override fun getAccessToken(): String? = synchronized(lock) {
        snapshot?.accessToken
    }

    override fun getRefreshToken(): String? = synchronized(lock) {
        snapshot?.refreshToken
    }

    override fun getEmail(): String? = synchronized(lock) {
        snapshot?.email
    }

    override fun hasSession(): Boolean = !getAccessToken().isNullOrBlank()

    override fun clear() {
        synchronized(lock) {
            snapshot = null
            clearCalls++
        }
    }
}
