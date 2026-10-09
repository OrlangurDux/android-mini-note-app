package app.mininote.mininote.util

private val taskMarker = Regex("""^(\s*(?:>\s*)*(?:[-*+]|\d{1,9}[.)])\s+)\[([ xX])]""")

/**
 * Переключает чекбокс `[ ]`/`[x]` у пункта списка на строке [lineIndex] (0-based) исходного markdown.
 * Номер строки приходит из source spans commonmark-парсера, поэтому `[ ]` внутри блоков кода
 * сюда не попадает. Возвращает исходный текст без изменений, если на строке нет чекбокса.
 */
fun toggleTaskAtLine(markdown: String, lineIndex: Int): String {
    val lines = markdown.split("\n").toMutableList()
    val line = lines.getOrNull(lineIndex) ?: return markdown
    val match = taskMarker.find(line) ?: return markdown
    val mark = match.groupValues[2]
    val flipped = if (mark == " ") "x" else " "
    val markIndex = match.groups[2]!!.range.first
    lines[lineIndex] = line.substring(0, markIndex) + flipped + line.substring(markIndex + 1)
    return lines.joinToString("\n")
}

/** null — не чекбокс; true/false — отмечен/не отмечен. Маркер ищется в начале первого абзаца пункта. */
fun taskStateOf(item: org.commonmark.node.ListItem): Boolean? {
    val text = (item.firstChild as? org.commonmark.node.Paragraph)?.firstChild as? org.commonmark.node.Text ?: return null
    val literal = text.literal
    if (literal.length < 3 || (literal.length > 3 && literal[3] != ' ')) return null
    return when (literal.substring(0, 3)) {
        "[ ]" -> false
        "[x]", "[X]" -> true
        else -> null
    }
}

private const val EDITOR_UNCHECKED = '\u2610' // ☐
private const val EDITOR_CHECKED = '\u2611'   // ☑
private const val LIST_PREFIX = """^(\s*(?:[-*+]|\d{1,9}[.)])\s+)"""
private val mdTaskItem = Regex("(?m)$LIST_PREFIX\\[([ xX]?)]\\s?")
private val editorTaskItem = Regex("(?m)$LIST_PREFIX([$EDITOR_UNCHECKED$EDITOR_CHECKED])\\s?")

/**
 * Rich-text-движок редактора не знает синтаксиса `- [ ]` и молча отбрасывает скобки, поэтому внутри
 * редактора пункт чек-листа живёт как обычный элемент списка с символом ☐/☑ в начале текста.
 * Принимает и испорченный прежним редактором вариант `[]` без пробела.
 */
fun tasksToEditor(markdown: String): String = mdTaskItem.replace(markdown) { m ->
    val box = if (m.groupValues[2].isBlank()) EDITOR_UNCHECKED else EDITOR_CHECKED
    "${m.groupValues[1]}$box "
}

/** Обратное преобразование на выходе редактора: ☐/☑ → `[ ]`/`[x]`. */
fun tasksFromEditor(markdown: String): String = editorTaskItem.replace(markdown) { m ->
    val box = if (m.groupValues[2][0] == EDITOR_CHECKED) "[x]" else "[ ]"
    "${m.groupValues[1]}$box "
}
