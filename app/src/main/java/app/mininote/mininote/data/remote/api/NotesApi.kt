package app.mininote.mininote.data.remote.api

import app.mininote.mininote.data.remote.dto.AckEnvelopeDto
import app.mininote.mininote.data.remote.dto.CreateResponseEnvelopeDto
import app.mininote.mininote.data.remote.dto.NoteEnvelopeDto
import app.mininote.mininote.data.remote.dto.NoteSearchEnvelopeDto
import app.mininote.mininote.data.remote.dto.NotesListEnvelopeDto
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NotesApi {
    @GET("notes")
    suspend fun getNotes(
        @Query("page") page: Int = 0,
        @Query("per_page") perPage: Int = 200,
    ): Response<NotesListEnvelopeDto>

    @GET("notes/search")
    suspend fun search(@Query("q") query: String): Response<NoteSearchEnvelopeDto>

    /** Используется для конфликт-чека перед отправкой локальной правки (см. SyncWorker). */
    @GET("notes/{id}")
    suspend fun getNote(@Path("id") id: String): Response<NoteEnvelopeDto>

    @FormUrlEncoded
    @POST("notes")
    suspend fun createNote(
        @Field("title") title: String,
        @Field("note") note: String,
        @Field("status") status: String,
        @Field("category_id") categoryId: String? = null,
    ): Response<CreateResponseEnvelopeDto>

    // category_id не в spec для PUT, но работает на практике (подтверждено эмпирически).
    @FormUrlEncoded
    @PUT("notes/{id}")
    suspend fun updateNote(
        @Path("id") id: String,
        @Field("title") title: String,
        @Field("note") note: String,
        @Field("status") status: String,
        @Field("category_id") categoryId: String? = null,
    ): Response<AckEnvelopeDto>

    @DELETE("notes/{id}")
    suspend fun deleteNote(@Path("id") id: String): Response<AckEnvelopeDto>

    // Метод сменился с GET на PUT на бэкенде (подтверждено эмпирически: GET теперь 405, PUT — 200);
    // тела всё равно нет — это по-прежнему тоггл, а не set/unset конкретного значения.
    @PUT("notes/favorite/{id}")
    suspend fun toggleFavorite(@Path("id") id: String): Response<AckEnvelopeDto>
}
