package app.mininote.mininote.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Login : Route

    /** Второй фактор при логине (см. AuthRepository.LoginOutcome.MfaRequired) — mfaToken короткоживущий,
     * передаётся через маршрут как обычный аргумент, не персистится нигде дальше этого экрана. */
    @Serializable data class LoginOtp(val mfaToken: String, val expiresInSeconds: Long) : Route
    @Serializable data object Signup : Route
    @Serializable data class SignupSent(val email: String) : Route

    // Вложенный граф "Забыли пароль" — шаги делят один ViewModel через scope графа.
    @Serializable data object ForgotGraph : Route
    @Serializable data object ForgotEmail : Route
    @Serializable data object ForgotCode : Route
    @Serializable data object ForgotNew : Route
    @Serializable data object ForgotDone : Route

    @Serializable data object Servers : Route

    /** Пост-логин хост: bottom nav (телефон) / navigation rail (планшет) с табами "Заметки"/"Профиль". */
    @Serializable data object Home : Route
    @Serializable data class NoteDetail(val localId: Long? = null, val startInEdit: Boolean = false) : Route

    @Serializable data object About : Route

    /** Полноэкранный, недисмиссящийся экран принудительного входа при истечении токена (Фаза 3). */
    @Serializable data object SessionExpired : Route
}
