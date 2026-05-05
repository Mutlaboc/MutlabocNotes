package com.example.mutlabocsnotes

import retrofit2.HttpException
import java.io.IOException

object ApiErrorMapper {
    fun map(error: Throwable): UiText {
        val resId = when (error) {
            is HttpException -> when (error.code()) {
                401 -> R.string.api_error_unauthorized
                409 -> R.string.api_error_conflict
                in 400..499 -> R.string.api_error_validation
                in 500..599 -> R.string.api_error_server
                else -> R.string.api_error_unknown
            }

            is IOException -> R.string.api_error_network
            is IllegalArgumentException -> R.string.api_error_validation
            else -> R.string.api_error_unknown
        }

        return UiText.StringResource(resId)
    }
}
