package app.mininote.mininote.data.remote.api

import app.mininote.mininote.data.remote.dto.AckEnvelopeDto
import app.mininote.mininote.data.remote.dto.CategoriesListEnvelopeDto
import app.mininote.mininote.data.remote.dto.CreateResponseEnvelopeDto
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CategoriesApi {
    @GET("categories")
    suspend fun getCategories(): Response<CategoriesListEnvelopeDto>

    // sort нужно отправлять всегда, даже нулевым: бэкенд падает с "strconv.Atoi: parsing \"\":
    // invalid syntax", если поле отсутствует в форме, несмотря на то что в spec оно опционально
    // (подтверждено эмпирически).
    @FormUrlEncoded
    @POST("categories")
    suspend fun createCategory(
        @Field("name") name: String,
        @Field("parent_id") parentId: String? = null,
        @Field("sort") sort: Int = 0,
    ): Response<CreateResponseEnvelopeDto>

    @FormUrlEncoded
    @PUT("categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: String,
        @Field("name") name: String,
        @Field("parent_id") parentId: String? = null,
        @Field("sort") sort: Int = 0,
    ): Response<AckEnvelopeDto>

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Response<AckEnvelopeDto>
}
