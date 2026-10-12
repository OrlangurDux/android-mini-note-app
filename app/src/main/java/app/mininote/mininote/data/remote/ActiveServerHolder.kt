package app.mininote.mininote.data.remote

import app.mininote.mininote.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ServerConfig(val name: String, val baseUrl: String)

// Debug — локальный бэкенд эмулятора, release — продовый сервер (см. buildTypes в app/build.gradle.kts).
val DEFAULT_SERVER = ServerConfig(name = BuildConfig.DEFAULT_SERVER_NAME, baseUrl = BuildConfig.DEFAULT_SERVER_URL)

/**
 * In-memory источник истины об активном сервере для [RoutingInterceptor] и [AuthInterceptor].
 * Interceptor'ы OkHttp работают на диспетчерских потоках и не должны делать suspend-чтение из
 * Room/DataStore на каждый запрос — поэтому [ServerRepository] гидрирует и обновляет этот holder,
 * а interceptor'ы читают [current] синхронно.
 */
@Singleton
class ActiveServerHolder @Inject constructor() {
    private val state = MutableStateFlow(DEFAULT_SERVER)
    val flow: StateFlow<ServerConfig> = state

    val current: ServerConfig get() = state.value

    fun update(config: ServerConfig) {
        state.value = config
    }
}
