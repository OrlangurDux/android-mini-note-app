package app.mininote.mininote.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VersionInfoDto(
    val version: String,
    val author: String,
    val contact: String,
)

@JsonClass(generateAdapter = true)
data class VersionEnvelopeDto(
    val success: Boolean,
    val status: Int,
    val data: VersionInfoDto?,
    val error: ErrorDto? = null,
)
