package com.example.mutlabocsnotes

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiErrorMapperTest {

    @Test
    fun map_unauthorizedReturnsSessionMessage() {
        val message = ApiErrorMapper.map(httpException(401))

        assertStringResource(R.string.api_error_unauthorized, message)
    }

    @Test
    fun map_conflictReturnsConflictMessage() {
        val message = ApiErrorMapper.map(httpException(409))

        assertStringResource(R.string.api_error_conflict, message)
    }

    @Test
    fun map_serverErrorReturnsServerMessage() {
        val message = ApiErrorMapper.map(httpException(500))

        assertStringResource(R.string.api_error_server, message)
    }

    @Test
    fun map_networkErrorReturnsNetworkMessage() {
        val message = ApiErrorMapper.map(IOException("offline"))

        assertStringResource(R.string.api_error_network, message)
    }

    @Test
    fun map_validationErrorReturnsValidationMessage() {
        val message = ApiErrorMapper.map(IllegalArgumentException("Blank note id"))

        assertStringResource(R.string.api_error_validation, message)
    }

    @Test
    fun map_unknownErrorReturnsUnknownMessage() {
        val message = ApiErrorMapper.map(IllegalStateException("backend down"))

        assertStringResource(R.string.api_error_unknown, message)
    }

    private fun httpException(code: Int): HttpException {
        return HttpException(Response.error<Unit>(code, "error".toResponseBody()))
    }

    private fun assertStringResource(expectedResId: Int, text: UiText) {
        val resource = text as UiText.StringResource
        assertEquals(expectedResId, resource.resId)
    }
}
