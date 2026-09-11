package app.mininote.mininote.data.repository

import android.content.Context
import app.mininote.mininote.R
import app.mininote.mininote.data.local.db.dao.CategoryDao
import app.mininote.mininote.data.local.db.dao.NoteDao
import app.mininote.mininote.data.local.db.dao.OutboxDao
import app.mininote.mininote.data.local.security.StoredSession
import app.mininote.mininote.data.local.security.TokenStore
import app.mininote.mininote.data.remote.ApiResult
import app.mininote.mininote.data.remote.api.AuthApi
import app.mininote.mininote.data.remote.dto.ErrorEnvelopeDto
import app.mininote.mininote.data.remote.dto.JwtDto
import app.mininote.mininote.data.remote.dto.UserProfileDto
import app.mininote.mininote.data.remote.safeApiCall
import app.mininote.mininote.sync.SyncScheduler
import com.squareup.moshi.JsonAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/** Код ошибки бэкенда для "пользователь с таким email не найден" (см. project memory). */
private const val ERROR_CODE_USER_NOT_FOUND = 40

/** `JwtDto.token_type`, когда пароль верный, но у аккаунта включена 2FA — `access_token` в этом
 * случае не полноценная сессия, а промежуточный токен для `POST /users/otp` (см. [AuthRepository.login]). */
private const val TOKEN_TYPE_MFA = "mfa"

/** Итог `AuthRepository.login()`: обычный логин может либо сразу завершиться сессией, либо
 * потребовать второй фактор — вызывающая сторона (LoginViewModel) должна различать эти два случая,
 * а не считать любой успешный HTTP-ответ полноценным входом. */
sealed interface LoginOutcome {
    data object Success : LoginOutcome
    data class MfaRequired(val mfaToken: String, val expiresInSeconds: Long) : LoginOutcome
}

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
    private val errorAdapter: JsonAdapter<ErrorEnvelopeDto>,
    private val syncScheduler: SyncScheduler,
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao,
    private val outboxDao: OutboxDao,
) {
    fun isLoggedIn(): Boolean = tokenStore.isValid()

    /** true — email занят существующим аккаунтом, false — свободен/не зарегистрирован. */
    suspend fun checkEmailExists(email: String): ApiResult<Boolean> {
        return when (val result = safeApiCall(context, errorAdapter) { authApi.checkEmail(email) }) {
            is ApiResult.Success -> ApiResult.Success(true)
            is ApiResult.Failure ->
                if (result.code == ERROR_CODE_USER_NOT_FOUND) ApiResult.Success(false) else result
        }
    }

    suspend fun register(email: String, password: String): ApiResult<Unit> {
        return when (val result = safeApiCall(context, errorAdapter) { authApi.register(email, password) }) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> result
        }
    }

    /**
     * `token_type="mfa"` в ответе — пароль верный, но аккаунт требует второй фактор: `access_token`
     * здесь НЕ полноценная сессия (см. [TOKEN_TYPE_MFA]), сохранять его в [TokenStore] нельзя —
     * `expires_in` для него секунды-до-истечения, а не абсолютный эпох, которого ждёт `TokenStore.isValid()`.
     * Вызывающая сторона обязана провести пользователя через [verifyOtp] прежде чем сессия появится.
     */
    suspend fun login(email: String, password: String): ApiResult<LoginOutcome> {
        return when (val result = safeApiCall(context, errorAdapter) { authApi.login(email, password) }) {
            is ApiResult.Success -> {
                val jwt = result.data
                if (jwt.token_type.equals(TOKEN_TYPE_MFA, ignoreCase = true)) {
                    ApiResult.Success(LoginOutcome.MfaRequired(mfaToken = jwt.access_token, expiresInSeconds = jwt.expires_in))
                } else {
                    persistSession(jwt)
                    ApiResult.Success(LoginOutcome.Success)
                }
            }
            is ApiResult.Failure -> result
        }
    }

    /** Второй шаг входа при `LoginOutcome.MfaRequired` — код из приложения-аутентификатора вместе
     * с временным mfa-токеном. Успех отдаёт полноценный JWT, который сохраняется тем же путём,
     * что и обычный логин без 2FA. */
    suspend fun verifyOtp(mfaToken: String, code: String): ApiResult<Unit> {
        return when (val result = safeApiCall(context, errorAdapter) { authApi.verifyOtp(mfaToken, code) }) {
            is ApiResult.Success -> {
                persistSession(result.data)
                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> result
        }
    }

    private fun persistSession(jwt: JwtDto) {
        tokenStore.save(
            StoredSession(
                accessToken = jwt.access_token,
                tokenType = jwt.token_type,
                expiresAtEpochSeconds = jwt.expires_in,
            ),
        )
        // Если офлайн-очередь не пуста, а предыдущая попытка SyncWorker'а отработала как
        // no-op из-за истёкшего/отсутствовавшего на тот момент токена (см. project memory:
        // воркер завершается успехом без ретрая, чтобы не долбить WorkManager впустую) —
        // без этого триггера очередь простаивала бы до 15-минутного бэкстопа.
        syncScheduler.enqueueOneTime()
    }

    /** Включает 2FA и возвращает `otpauth://` URL для QR-кода/ручного ввода в приложении-аутентификаторе. */
    suspend fun enableTfa(): ApiResult<String> {
        return when (val result = safeApiCall(context, errorAdapter) { authApi.enableTfa() }) {
            is ApiResult.Success -> result.data.data?.url?.let { ApiResult.Success(it) }
                ?: ApiResult.Failure(-1, context.getString(R.string.error_tfa_setup_failed))
            is ApiResult.Failure -> result
        }
    }

    suspend fun disableTfa(): ApiResult<Unit> {
        return when (val result = safeApiCall(context, errorAdapter) { authApi.disableTfa() }) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> result
        }
    }

    /**
     * Регистрация как единое логическое действие: register → login → (опционально) сохранение
     * имени. Бэкенд не поддерживает имя при регистрации (см. project memory), поэтому оно
     * сохраняется отдельным вызовом после получения токена; неудача этого последнего шага не
     * должна откатывать уже созданный и залогиненный аккаунт.
     */
    suspend fun signUp(name: String, email: String, password: String): ApiResult<Unit> {
        val registerResult = register(email, password)
        if (registerResult is ApiResult.Failure) return registerResult

        // Свежезарегистрированный аккаунт физически не мог успеть включить 2FA — ветка MfaRequired
        // сюда попасть не должна, но сигнатура login() теперь допускает её; трактуем как ошибку,
        // а не молча притворяемся, что сессия уже есть (signUp() обещает вызывающей стороне именно это).
        when (val loginResult = login(email, password)) {
            is ApiResult.Failure -> return loginResult
            is ApiResult.Success -> if (loginResult.data is LoginOutcome.MfaRequired) {
                return ApiResult.Failure(-1, context.getString(R.string.error_unexpected))
            }
        }

        if (name.isNotBlank()) {
            val body = name.toRequestBody("text/plain".toMediaType())
            safeApiCall(context, errorAdapter) { authApi.updateProfile(body) }
        }
        return ApiResult.Success(Unit)
    }

    suspend fun requestPasswordReset(email: String): ApiResult<Unit> {
        return when (val result = safeApiCall(context, errorAdapter) { authApi.requestPasswordReset(email) }) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> result
        }
    }

    suspend fun confirmPasswordReset(email: String, code: String, newPassword: String): ApiResult<Unit> {
        return when (
            val result = safeApiCall(context, errorAdapter) { authApi.confirmPasswordReset(email, code, newPassword) }
        ) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> result
        }
    }

    suspend fun changePassword(newPassword: String): ApiResult<Unit> {
        return when (val result = safeApiCall(context, errorAdapter) { authApi.changePassword(newPassword) }) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> result
        }
    }

    suspend fun getProfile(): ApiResult<UserProfileDto> {
        return when (val result = safeApiCall(context, errorAdapter) { authApi.getProfile() }) {
            is ApiResult.Success -> result.data.data?.let { ApiResult.Success(it) }
                ?: ApiResult.Failure(-1, context.getString(R.string.error_profile_not_found))
            is ApiResult.Failure -> result
        }
    }

    /** Локальный кэш заметок/категорий/outbox — данные одного аккаунта, чужие на общем устройстве
     * следующему пользователю. Список серверов НЕ трогаем — это настройка устройства, не аккаунта. */
    suspend fun logout() {
        tokenStore.clear()
        noteDao.deleteAll()
        categoryDao.deleteAll()
        outboxDao.deleteAll()
    }
}
