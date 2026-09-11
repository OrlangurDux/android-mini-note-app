package app.mininote.mininote.data.repository

import android.content.Context
import app.mininote.mininote.data.remote.ApiResult
import app.mininote.mininote.data.remote.api.SupportApi
import app.mininote.mininote.data.remote.dto.ErrorEnvelopeDto
import app.mininote.mininote.data.remote.dto.FeedbackRequestDto
import app.mininote.mininote.data.remote.safeApiCall
import com.squareup.moshi.JsonAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supportApi: SupportApi,
    private val errorAdapter: JsonAdapter<ErrorEnvelopeDto>,
) {
    /** Экран "О приложении" — форма обратной связи. [contact] — телефон или email, см. FeedbackRequestDto. */
    suspend fun sendFeedback(name: String, contact: String, message: String): ApiResult<Unit> {
        return when (
            val result = safeApiCall(context, errorAdapter) { supportApi.sendFeedback(FeedbackRequestDto(name, contact, message)) }
        ) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> result
        }
    }
}
