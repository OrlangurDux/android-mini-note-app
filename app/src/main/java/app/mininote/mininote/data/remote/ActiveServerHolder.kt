package app.mininote.mininote.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ServerConfig(val name: String, val baseUrl: String)

val DEFAULT_SERVER = ServerConfig(name = "Локальный сервер", baseUrl = "http://10.0.2.2:9077/api/v1/")

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
