package app.mininote.mininote.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Retrofit сконфигурирован с фиктивным placeholder-baseUrl (см. [ApiClientProvider]); этот
 * interceptor на лету переписывает запрос на реальный активный сервер из [ActiveServerHolder].
 * Переключение сервера в UI вступает в силу с самого следующего запроса — держим единственный
 * долгоживущий OkHttp/Retrofit синглтон вместо пересборки клиента на каждое переключение.
 */
class RoutingInterceptor @Inject constructor(
    private val activeServerHolder: ActiveServerHolder,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val originalUrl = original.url

        val relativePath = originalUrl.encodedPath.removePrefix("/")
        val relative = originalUrl.encodedQuery?.let { "$relativePath?$it" } ?: relativePath

        val activeBaseUrl = activeServerHolder.current.baseUrl.toHttpUrl()
        val newUrl = activeBaseUrl.resolve(relative) ?: activeBaseUrl

        val newRequest = original.newBuilder().url(newUrl).build()
        return chain.proceed(newRequest)
    }
}
