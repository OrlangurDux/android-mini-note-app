package app.mininote.mininote.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/**
 * Собственный механизм переключения языка приложения вместо AppCompatDelegate.setApplicationLocales():
 * этот метод молча ничего не делает, если ни разу не был создан AppCompatDelegate (что и есть наш
 * случай — MainActivity сознательно обычный ComponentActivity, без единой AppCompatActivity в графе,
 * см. project memory), — на API 33+ он резолвит system LocaleManager через `sActivityDelegates`
 * (список активных AppCompatDelegate), который в такой конфигурации всегда пуст, а на API 31-32 —
 * через тот же пустой список делегатов для применения override. Подтверждено эмпирически на
 * эмуляторе (API 34): `cmd locale get-app-locales` оставался пустым после вызова. Вместо этого
 * храним тег языка сами и оборачиваем Base Context в attachBaseContext (см. MainActivity) —
 * работает единообразно на всех API 31+, независимо от платформенных особенностей.
 */
private const val PREFS_NAME = "app_locale"
private const val KEY_LANGUAGE_TAG = "language_tag"

object AppLocale {
    fun getTag(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_LANGUAGE_TAG, null)

    /** [tag] — язык (например "ru"/"en"), null — сбросить на системный. */
    fun setTag(context: Context, tag: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putString(KEY_LANGUAGE_TAG, tag) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
            localeManager?.applicationLocales = if (tag != null) LocaleList.forLanguageTags(tag) else LocaleList.getEmptyLocaleList()
        }
    }

    /** Вызывается из Activity.attachBaseContext — единственная точка, гарантированно применяемая
     * до инфляции любых ресурсов, включая cold start. */
    fun wrap(base: Context): Context {
        val tag = getTag(base) ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
