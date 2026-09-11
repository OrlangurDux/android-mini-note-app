package app.mininote.mininote.data.remote.api

import app.mininote.mininote.data.remote.dto.VersionEnvelopeDto
import retrofit2.Response
import retrofit2.http.GET

interface VersionApi {
    @GET("version")
    suspend fun getVersion(): Response<VersionEnvelopeDto>
}
