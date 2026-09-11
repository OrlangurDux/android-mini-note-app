package app.mininote.mininote.data.repository

import app.mininote.mininote.data.local.db.dao.ServerDao
import app.mininote.mininote.data.local.db.entity.ServerEntity
import app.mininote.mininote.data.local.prefs.SettingsDataStore
import app.mininote.mininote.data.remote.ActiveServerHolder
import app.mininote.mininote.data.remote.DEFAULT_SERVER
import app.mininote.mininote.data.remote.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Приводит введённый пользователем адрес к полному base URL с фиксированным basePath бэкенда. */
fun normalizeServerAddress(input: String): String {
    var address = input.trim().trimEnd('/')
    if (!address.startsWith("http://") && !address.startsWith("https://")) {
        address = "http://$address"
    }
    return "$address/api/v1/"
}

@Singleton
class ServerRepository @Inject constructor(
    private val serverDao: ServerDao,
    private val settingsDataStore: SettingsDataStore,
    private val activeServerHolder: ActiveServerHolder,
) {
    fun observeServers(): Flow<List<ServerEntity>> = serverDao.observeAll()

    /** Засеивает сервер по умолчанию (один раз) и гидрирует [ActiveServerHolder] выбранным сервером.
     * Должно вызываться один раз при старте приложения, до показа auth-экранов. */
    suspend fun ensureInitialized() {
        if (serverDao.count() == 0) {
            serverDao.insert(
                ServerEntity(name = DEFAULT_SERVER.name, baseUrl = DEFAULT_SERVER.baseUrl, isDefault = true),
            )
        }
        val selectedId = settingsDataStore.selectedServerId.first()
        val active = selectedId?.let { serverDao.getById(it) } ?: findDefault()
        activeServerHolder.update(ServerConfig(name = active.name, baseUrl = active.baseUrl))
    }

    suspend fun addServer(name: String, address: String): Long {
        val entity = ServerEntity(name = name, baseUrl = normalizeServerAddress(address), isDefault = false)
        return serverDao.insert(entity)
    }

    suspend fun deleteServer(server: ServerEntity) {
        require(!server.isDefault) { "Сервер по умолчанию нельзя удалить" }
        serverDao.delete(server)
        // Если удалили активный сервер — откатываемся на дефолтный.
        if (activeServerHolder.current.baseUrl == server.baseUrl) {
            selectServer(findDefault())
        }
    }

    suspend fun selectServer(server: ServerEntity) {
        settingsDataStore.setSelectedServerId(server.id)
        activeServerHolder.update(ServerConfig(name = server.name, baseUrl = server.baseUrl))
    }

    private suspend fun findDefault(): ServerEntity =
        observeServers().first().firstOrNull { it.isDefault }
            ?: ServerEntity(name = DEFAULT_SERVER.name, baseUrl = DEFAULT_SERVER.baseUrl, isDefault = true)
}
