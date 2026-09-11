package app.mininote.mininote.data.repository

import android.content.Context
import app.mininote.mininote.R
import app.mininote.mininote.data.remote.ApiResult
import app.mininote.mininote.data.remote.api.VersionApi
import app.mininote.mininote.data.remote.dto.ErrorEnvelopeDto
import app.mininote.mininote.data.remote.dto.VersionInfoDto
import app.mininote.mininote.data.remote.safeApiCall
import com.squareup.moshi.JsonAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VersionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val versionApi: VersionApi,
    private val errorAdapter: JsonAdapter<ErrorEnvelopeDto>,
) {
    suspend fun getVersion(): ApiResult<VersionInfoDto> {
        return when (val result = safeApiCall(context, errorAdapter) { versionApi.getVersion() }) {
            is ApiResult.Success -> result.data.data?.let { ApiResult.Success(it) }
                ?: ApiResult.Failure(-1, context.getString(R.string.error_no_data))
            is ApiResult.Failure -> result
        }
    }
}
