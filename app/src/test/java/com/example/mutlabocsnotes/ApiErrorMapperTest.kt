package com.example.mutlabocsnotes

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
    fun map_timeoutReturnsNetworkMessage() {
        val message = ApiErrorMapper.map(SocketTimeoutException("timeout"))

        assertStringResource(R.string.api_error_network, message)
    }

    @Test
    fun map_unknownHostReturnsNetworkMessage() {
        val message = ApiErrorMapper.map(UnknownHostException("no network"))

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

    @Test
    fun map_apiUnauthorizedCodeReturnsSessionMessage() {
        val message = ApiErrorMapper.map(httpException(401, """{"code":"unauthorized"}"""))

        assertStringResource(R.string.api_error_unauthorized, message)
    }

    @Test
    fun map_apiInvalidRequestCodeReturnsValidationMessage() {
        val message = ApiErrorMapper.map(httpException(400, """{"code":"invalid_request","message":"Email is required"}"""))

        assertStringResource(R.string.api_error_validation, message)
    }

    @Test
    fun map_apiEmailAlreadyRegisteredCodeReturnsConflictMessage() {
        val message = ApiErrorMapper.map(httpException(409, """{"code":"email_already_registered"}"""))

        assertStringResource(R.string.api_error_conflict, message)
    }

    @Test
    fun map_apiInvalidCredentialsCodeReturnsCredentialsMessage() {
        val message = ApiErrorMapper.map(httpException(401, """{"code":"invalid_email_or_password"}"""))

        assertStringResource(R.string.auth_error_invalid_credentials, message)
    }

    @Test
    fun map_apiInternalServerCodeReturnsServerMessage() {
        val message = ApiErrorMapper.map(httpException(500, """{"code":"internal_server_error"}"""))

        assertStringResource(R.string.api_error_server, message)
    }

    @Test
    fun map_apiUserInactiveCodeReturnsSessionMessage() {
        val message = ApiErrorMapper.map(httpException(403, """{"code":"user_inactive"}"""))

        assertStringResource(R.string.api_error_unauthorized, message)
    }

    @Test
    fun map_apiMissingResourceCodesReturnValidationMessage() {
        val note = ApiErrorMapper.map(httpException(404, """{"code":"note_not_found"}"""))
        val card = ApiErrorMapper.map(httpException(404, """{"code":"card_not_found"}"""))

        assertStringResource(R.string.api_error_validation, note)
        assertStringResource(R.string.api_error_validation, card)
    }

    @Test
    fun map_apiInvalidCardIdCodeReturnsValidationMessage() {
        val message = ApiErrorMapper.map(httpException(400, """{"code":"invalid_card_id"}"""))

        assertStringResource(R.string.api_error_validation, message)
    }

    private fun httpException(code: Int, body: String = "error"): HttpException {
        return HttpException(
            Response.error<Unit>(
                code,
                body.toResponseBody("application/json".toMediaType())
            )
        )
    }

    private fun assertStringResource(expectedResId: Int, text: UiText) {
        val resource = text as UiText.StringResource
        assertEquals(expectedResId, resource.resId)
    }
}
