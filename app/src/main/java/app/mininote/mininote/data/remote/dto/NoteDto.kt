package app.mininote.mininote.data.remote.dto

import com.squareup.moshi.JsonClass

// Поле "categories" (вложенный join) сознательно не парсим — подтверждён баг бэкенда:
// оно всегда {id: zero-id, name: ""} независимо от реального category_id (см. project memory).
// favorite — со значением по умолчанию: Go-бэкенды часто сериализуют bool-поля с `omitempty`,
// из-за чего "false" может просто отсутствовать в JSON целиком — non-null без дефолта уронил бы
// парсинг на такой записи, а не просто прочитал бы false (см. codegen-семантику Moshi).
@JsonClass(generateAdapter = true)
data class NoteDto(
    val id: String,
    val category_id: String,
    val title: String,
    val note: String,
    val status: String,
    val created_at: String,
    val updated_at: String,
    val favorite: Boolean = false,
)

// items приходит как JSON null (а не []), когда список пуст — подтверждено эмпирически.
@JsonClass(generateAdapter = true)
data class NotesListDto(
    val total: Int,
    val page: Int,
    val per_page: Int,
    val items: List<NoteDto>?,
)

@JsonClass(generateAdapter = true)
data class NotesListEnvelopeDto(
    val success: Boolean,
    val status: Int,
    val data: NotesListDto?,
    val error: ErrorDto? = null,
)

/** Ответ GET /notes/{id} — используется для конфликт-чека перед пушем локальной правки. */
@JsonClass(generateAdapter = true)
data class NoteEnvelopeDto(
    val success: Boolean,
    val status: Int,
    val data: NoteDto?,
    val error: ErrorDto? = null,
)

@JsonClass(generateAdapter = true)
data class NoteSearchEnvelopeDto(
    val success: Boolean,
    val status: Int,
    val data: List<NoteDto>?,
    val error: ErrorDto? = null,
)

/** Общая форма ответа на создание записи (заметки/категории): {id, message}. */
@JsonClass(generateAdapter = true)
data class CreateEntityResultDto(
    val id: String,
    val message: String,
)

@JsonClass(generateAdapter = true)
data class CreateResponseEnvelopeDto(
    val success: Boolean,
    val status: Int,
    val data: CreateEntityResultDto?,
    val error: ErrorDto? = null,
)
