package app.homenotes.android

import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

object ApiErrorMapper {
    private val gson = Gson()

    fun map(error: Throwable): UiText {
        val resId = when (error) {
            is HttpException -> mapHttpException(error)

            is IOException -> R.string.api_error_network
            is IllegalArgumentException -> R.string.api_error_validation
            else -> R.string.api_error_unknown
        }

        return UiText.StringResource(resId)
    }

    private fun mapHttpException(error: HttpException): Int {
        val errorCode = error.apiErrorCode()
        if (errorCode != null) {
            return when (errorCode) {
                "unauthorized",
                "user_inactive" -> R.string.api_error_unauthorized

                "email_already_registered" -> R.string.api_error_conflict

                "invalid_email_or_password" -> R.string.auth_error_invalid_credentials

                "invalid_request",
                "invalid_note_id",
                "invalid_card_id",
                "note_not_found",
                "card_not_found" -> R.string.api_error_validation

                "internal_server_error" -> R.string.api_error_server

                else -> fallbackHttpStatus(error.code())
            }
        }

        return fallbackHttpStatus(error.code())
    }

    private fun fallbackHttpStatus(statusCode: Int): Int =
        when (statusCode) {
            401 -> R.string.api_error_unauthorized
            409 -> R.string.api_error_conflict
            in 400..499 -> R.string.api_error_validation
            in 500..599 -> R.string.api_error_server
            else -> R.string.api_error_unknown
        }

    private fun HttpException.apiErrorCode(): String? {
        val body = response()?.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            gson.fromJson(body, ApiErrorDto::class.java)?.code?.trim()?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    private data class ApiErrorDto(
        val code: String? = null,
        val message: String? = null
    )
}
