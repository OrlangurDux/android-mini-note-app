package app.mininote.mininote.data.remote.api

import app.mininote.mininote.data.remote.dto.AckEnvelopeDto
import app.mininote.mininote.data.remote.dto.FeedbackRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SupportApi {
    @POST("send/request")
    suspend fun sendFeedback(@Body request: FeedbackRequestDto): Response<AckEnvelopeDto>
}
