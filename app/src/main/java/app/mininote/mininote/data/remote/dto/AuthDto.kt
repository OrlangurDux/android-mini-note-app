package app.mininote.mininote.data.remote.dto

import com.squareup.moshi.JsonClass

/**
 * ВАЖНО (подтверждено эмпирически против живого бэкенда, см. project memory): `expires_in` —
 * это НЕ всегда длительность в секундах. Для обычного `token_type="Bearer"` это абсолютный
 * Unix-таймстамп истечения токена, совпадающий с `exp` claim внутри самого JWT — прибавлять
 * к нему `now()` не нужно, сохранять как есть. НО для `token_type="mfa"` (промежуточный токен
 * второго фактора — см. [TfaSetupDto]/`AuthApi.verifyOtp`) `expires_in` — это НАСТОЯЩАЯ
 * длительность в секундах (подтверждено: значение вида 60, а не эпоха), т.к. `access_token` в
 * этом случае вообще не JWT, а непрозрачная одноразовая строка сессии второго фактора. Два разных
 * контракта под одним полем — не полагаться на общее "expires_in = абсолютный эпох" без проверки
 * `token_type` (см. AuthRepository.login()).
 */
@JsonClass(generateAdapter = true)
data class JwtDto(
    val success: Boolean,
    val access_token: String,
    val expires_in: Long,
    val token_type: String,
)

@JsonClass(generateAdapter = true)
data class UserProfileDto(
    val id: String,
    val name: String,
    val email: String,
    val avatar: String,
    val active: Boolean,
    val created_at: String,
    val updated_at: String,
    /** Включена ли двухфакторная аутентификация (`PUT /users/tfa`) — источник истины для
     * переключателя в профиле, не локальное клиентское состояние. */
    val is_2fa: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ProfileEnvelopeDto(
    val success: Boolean,
    val status: Int,
    val data: UserProfileDto?,
    val error: ErrorDto? = null,
)

/**
 * Тело `data` при включении 2FA (`PUT /users/tfa {status=true}`) — `url` это `otpauth://totp/...`
 * URI для QR-кода приложения-аутентификатора. При ВЫКЛЮЧЕНИИ (`status=false`) `data` на бэкенде —
 * произвольная строка ("2FA disabled"), а не объект такой формы — этот DTO используется только
 * для enable-вызова (см. AuthApi.enableTfa/disableTfa — намеренно два отдельных метода на один
 * эндпоинт, disable декодируется как [AckEnvelopeDto], который поле `data` не трогает вовсе).
 */
@JsonClass(generateAdapter = true)
data class TfaSetupDto(
    val msg: String = "",
    val url: String? = null,
)

@JsonClass(generateAdapter = true)
data class TfaSetupEnvelopeDto(
    val success: Boolean,
    val status: Int,
    val data: TfaSetupDto? = null,
    val error: ErrorDto? = null,
)
