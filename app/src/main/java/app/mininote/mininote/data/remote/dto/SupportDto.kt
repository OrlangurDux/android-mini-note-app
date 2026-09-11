package app.mininote.mininote.data.remote.dto

import com.squareup.moshi.JsonClass

/**
 * Тело запроса POST /send/request — единственный эндпоинт с JSON-телом (не formData).
 * По swagger (`definitions.Request`) бэкенд реально принимает только `name`/`phone` — `phone`
 * здесь хранит телефон ИЛИ email (одно поле контакта в UI), `message` в спеке отсутствует и
 * сейчас будет молча проигнорирован Go-декодером; поле оставлено как задел на случай, если
 * бэкенд его добавит — от этого ничего не сломается уже сегодня.
 */
@JsonClass(generateAdapter = true)
data class FeedbackRequestDto(
    val name: String,
    val phone: String,
    val message: String,
)
