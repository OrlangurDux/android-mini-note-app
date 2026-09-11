package app.mininote.mininote.util

import app.mininote.mininote.R

/** Значения — задокументированный enum бэкенда (`draft`/`public`/`archive`, swagger.json), не
 * произвольная клиентская конвенция (более ранняя находка "бэкенд не валидирует status" была
 * верна для более старой версии API — см. project memory про эту переоценку). */
enum class NoteStatus(val apiValue: String, val labelRes: Int) {
    ACTIVE("public", R.string.status_active),
    ARCHIVED("archive", R.string.status_archived),
    DRAFT("draft", R.string.status_draft),
    ;

    companion object {
        fun fromApiValue(value: String): NoteStatus = entries.firstOrNull { it.apiValue == value } ?: ACTIVE
    }
}
