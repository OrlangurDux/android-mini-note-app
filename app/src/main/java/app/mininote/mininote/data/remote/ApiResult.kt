package app.mininote.mininote.data.remote

import android.content.Context
import app.mininote.mininote.R
import app.mininote.mininote.data.remote.dto.ErrorEnvelopeDto
import com.squareup.moshi.JsonAdapter
import retrofit2.Response
import java.io.IOException

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val code: Int, val message: String) : ApiResult<Nothing>
}

/**
 * Единая обёртка для всех Retrofit-вызовов: успешный ответ 2xx → [ApiResult.Success], ответ вида
 * `UniversalDTO{success:false, error:{code,message}}` на 400/404/500 → [ApiResult.Failure] с
 * реальным сообщением бэкенда, сетевая ошибка (нет соединения и т.п.) → [ApiResult.Failure] с
 * кодом -1 и обобщённым сообщением. [context] нужен только для локализации этих двух общих
 * сообщений (текст ошибки от бэкенда локализовать нечем — он приходит как есть).
 */
suspend fun <T> safeApiCall(
    context: Context,
    errorAdapter: JsonAdapter<ErrorEnvelopeDto>,
    call: suspend () -> Response<T>,
): ApiResult<T> {
    return try {
        val response = call()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            ApiResult.Success(body)
        } else {
            val raw = response.errorBody()?.string()
            val parsed = raw?.let { runCatching { errorAdapter.fromJson(it) }.getOrNull() }
            ApiResult.Failure(
                code = parsed?.error?.code ?: response.code(),
                message = parsed?.error?.message?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.error_server_code, response.code()),
            )
        }
    } catch (e: IOException) {
        ApiResult.Failure(code = -1, message = context.getString(R.string.error_no_connection))
    }
}
