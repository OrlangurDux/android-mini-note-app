package app.mininote.mininote.data.remote

import app.mininote.mininote.data.local.security.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Подставляет `Authorization: Bearer <token>` для эндпоинтов, требующих BearerAuth. */
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val session = tokenStore.load()
            ?: return chain.proceed(original)

        val authorized = original.newBuilder()
            .header("Authorization", "${session.tokenType} ${session.accessToken}")
            .build()
        return chain.proceed(authorized)
    }
}
