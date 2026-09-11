package app.mininote.mininote.data.remote.api

import app.mininote.mininote.data.remote.dto.AckEnvelopeDto
import app.mininote.mininote.data.remote.dto.JwtDto
import app.mininote.mininote.data.remote.dto.ProfileEnvelopeDto
import app.mininote.mininote.data.remote.dto.TfaSetupEnvelopeDto
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface AuthApi {
    @FormUrlEncoded
    @POST("users/register")
    suspend fun register(@Field("email") email: String, @Field("password") password: String): Response<AckEnvelopeDto>

    @FormUrlEncoded
    @POST("users/login")
    suspend fun login(@Field("email") email: String, @Field("password") password: String): Response<JwtDto>

    @FormUrlEncoded
    @POST("users/check")
    suspend fun checkEmail(@Field("email") email: String): Response<AckEnvelopeDto>

    @FormUrlEncoded
    @POST("users/forgot")
    suspend fun requestPasswordReset(@Field("email") email: String): Response<AckEnvelopeDto>

    @FormUrlEncoded
    @POST("users/forgot")
    suspend fun confirmPasswordReset(
        @Field("email") email: String,
        @Field("restore_token") restoreToken: String,
        @Field("password") newPassword: String,
    ): Response<AckEnvelopeDto>

    @FormUrlEncoded
    @PUT("users/password")
    suspend fun changePassword(@Field("password") newPassword: String): Response<AckEnvelopeDto>

    @GET("users/profile")
    suspend fun getProfile(): Response<ProfileEnvelopeDto>

    // PUT /users/profile — единственный эндпоинт, которому нужен multipart/form-data (см. project
    // memory: подтверждено эмпирически), т.к. в spec у него есть файловый параметр avatar.
    // Загрузка аватара сознательно вне MVP (см. план) — отправляем только имя.
    @Multipart
    @PUT("users/profile")
    suspend fun updateProfile(@Part("name") name: RequestBody): Response<AckEnvelopeDto>

    @DELETE("users/profile")
    suspend fun deleteProfile(): Response<AckEnvelopeDto>

    // Один эндпоинт (`PUT users/tfa`), два Retrofit-метода: тело ответа `data` имеет разную форму
    // в зависимости от status (объект {msg,url} при включении, произвольная строка при выключении —
    // см. TfaSetupDto), а Moshi-кодогену нужен фиксированный тип на каждый вызов. AckEnvelopeDto
    // не парсит `data` вовсе, поэтому безопасно переиспользуется для disable независимо от формы.
    @FormUrlEncoded
    @PUT("users/tfa")
    suspend fun enableTfa(@Field("status") status: Boolean = true): Response<TfaSetupEnvelopeDto>

    @FormUrlEncoded
    @PUT("users/tfa")
    suspend fun disableTfa(@Field("status") status: Boolean = false): Response<AckEnvelopeDto>

    // Второй шаг логина при token_type="mfa" (см. project memory про JwtDto.expires_in). Успешный
    // ответ — снова JwtDto (token_type="Bearer" на этот раз), несмотря на то что swagger.json
    // документирует UniversalDTO — подтверждено эмпирически: тело успеха реально JWT-формы, только
    // ошибка (400/404/500) — настоящий UniversalDTO, что уже покрыто общим errorAdapter в safeApiCall.
    @FormUrlEncoded
    @POST("users/otp")
    suspend fun verifyOtp(@Field("token") token: String, @Field("code") code: String): Response<JwtDto>
}
