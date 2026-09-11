package app.mininote.mininote.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CategoryDto(
    val id: String,
    val name: String,
    val parent_id: String,
    val sort: Int,
)

// items приходит как JSON null (а не []), когда список пуст — подтверждено эмпирически.
@JsonClass(generateAdapter = true)
data class CategoriesListDto(
    val total: Int,
    val items: List<CategoryDto>?,
)

@JsonClass(generateAdapter = true)
data class CategoriesListEnvelopeDto(
    val success: Boolean,
    val status: Int,
    val data: CategoriesListDto?,
    val error: ErrorDto? = null,
)
