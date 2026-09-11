package app.mininote.mininote.data.remote.dto

import com.squareup.moshi.JsonClass

/** Общий конверт ошибки бэкенда (`UniversalDTO.error`), одинаковый на всех эндпоинтах. */
@JsonClass(generateAdapter = true)
data class ErrorDto(
    val code: Int = 0,
    val message: String = "",
)

/**
 * Минимальная форма `UniversalDTO`, достаточная чтобы распарсить тело ошибки (HTTP 400/404/500) —
 * используется в [app.mininote.mininote.data.remote.safeApiCall] независимо от конкретного эндпоинта,
 * т.к. поле `data` варьируется по форме, а `success`/`status`/`error` — нет.
 */
@JsonClass(generateAdapter = true)
data class ErrorEnvelopeDto(
    val success: Boolean = false,
    val status: Int = 0,
    val error: ErrorDto? = null,
)

/** Ack-ответ для эндпоинтов, где нас интересует только success/error, а `data` — произвольная строка/объект. */
@JsonClass(generateAdapter = true)
data class AckEnvelopeDto(
    val success: Boolean,
    val status: Int,
    val error: ErrorDto? = null,
)
