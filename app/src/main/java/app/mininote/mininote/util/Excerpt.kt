package app.mininote.mininote.util

import org.commonmark.parser.Parser
import org.commonmark.renderer.text.TextContentRenderer

private val excerptParser: Parser by lazy { Parser.builder().build() }
private val excerptRenderer: TextContentRenderer by lazy { TextContentRenderer.builder().build() }

/** В API нет отдельного поля excerpt — вырезаем превью из контента на клиенте. Рендерим через
 * тот же commonmark, что и [app.mininote.mininote.ui.components.markdown.MarkdownViewer], чтобы
 * в превью не мелькали сырые `**`/`#`/`` ` `` — только читаемый текст. */
fun deriveExcerpt(content: String, maxLength: Int = 140): String {
    // Пункты чек-листа показываем как ☐/☑, а не сырыми `[ ]`/`[x]`.
    val plain = excerptRenderer.render(excerptParser.parse(content))
        .replace(Regex("(?m)^(\\s*(?:[-*+]|\\d{1,9}[.)]))\\s+\\[ ]"), "$1 \u2610")
        .replace(Regex("(?m)^(\\s*(?:[-*+]|\\d{1,9}[.)]))\\s+\\[[xX]]"), "$1 \u2611")
        .replace(Regex("\\s+"), " ")
        .trim()
    return if (plain.length > maxLength) {
        plain.take(maxLength).trimEnd() + "…"
    } else {
        plain
    }
}
