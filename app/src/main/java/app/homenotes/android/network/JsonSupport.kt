package app.homenotes.android.network

import kotlinx.serialization.json.Json

/**
 * Shared kotlinx.serialization config for both the Retrofit converter and the ad-hoc
 * (de)serialization of Room JSON columns / outbox payloads. Lenient/ignoreUnknownKeys
 * mirrors the previous Gson behaviour, which never failed on extra or missing fields.
 */
val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}
