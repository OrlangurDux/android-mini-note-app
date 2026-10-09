package app.mininote.mininote.ui.components.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser
import app.mininote.mininote.util.taskStateOf
import org.commonmark.node.Text as MdText

// Source spans нужны, чтобы чекбокс знал свою строку в исходном markdown (см. toggleTaskAtLine).
private val markdownParser: Parser by lazy {
    Parser.builder().includeSourceSpans(IncludeSourceSpans.BLOCKS).build()
}

private val LocalTaskToggle = staticCompositionLocalOf<((Int) -> Unit)?> { null }

/**
 * Рендерит markdown как обычный текст заметки. Собственный небольшой рендерер поверх
 * commonmark-java (чистый JVM-парсер, без Compose-зависимостей) вместо стороннего
 * Compose-markdown-движка — так рендер гарантированно совместим с текущей версией Compose
 * Compiler и раскрашивается ровно в M3-токены темы приложения, без отдельного адаптера.
 * Покрывает: заголовки, абзацы, жирный/курсивный/моноширинный текст, ссылки, списки
 * (маркированные и нумерованные), блоки кода, цитаты, разделители. Не покрывает: изображения
 * (рендерятся как подпись — в проекте нет загрузчика картинок), таблицы, HTML-вставки.
 */
@Composable
fun MarkdownViewer(
    markdown: String,
    modifier: Modifier = Modifier,
    /** Вызывается с номером строки (0-based) исходного markdown при тапе по чекбоксу `[ ]`; null — чекбоксы только для чтения. */
    onToggleTask: ((lineIndex: Int) -> Unit)? = null,
) {
    val document = remember(markdown) { markdownParser.parse(markdown) }
    CompositionLocalProvider(LocalTaskToggle provides onToggleTask) {
        Column(modifier = modifier) {
            RenderBlock(document)
        }
    }
}

@Composable
private fun RenderBlock(node: Node) {
    when (node) {
        is Heading -> {
            val style = when (node.level) {
                1 -> MaterialTheme.typography.headlineMedium
                2 -> MaterialTheme.typography.headlineSmall
                3 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            }
            Text(
                text = buildInline(node),
                style = style,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
        }
        is Paragraph -> Text(
            text = buildInline(node),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        is BulletList -> RenderList(node, ordered = false)
        is OrderedList -> RenderList(node, ordered = true)
        is FencedCodeBlock -> CodeBlock(node.literal)
        is IndentedCodeBlock -> CodeBlock(node.literal)
        is BlockQuote -> Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                renderChildren(node)
            }
        }
        is ThematicBreak -> HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        else -> renderChildren(node) // Document и прочие контейнеры — просто разворачиваем детей.
    }
}

@Composable
private fun renderChildren(node: Node) {
    var child = node.firstChild
    while (child != null) {
        RenderBlock(child)
        child = child.next
    }
}

@Composable
private fun RenderList(listNode: Node, ordered: Boolean) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        var index = if (listNode is OrderedList) listNode.markerStartNumber ?: 1 else 1
        var item: Node? = listNode.firstChild
        while (item != null) {
            if (item is ListItem && taskStateOf(item) != null) {
                TaskItem(item, checked = taskStateOf(item) == true)
            } else if (item is ListItem) {
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        text = if (ordered) "$index." else "•",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        renderChildren(item)
                    }
                }
                index++
            }
            item = item.next
        }
    }
}

@Composable
private fun TaskItem(item: ListItem, checked: Boolean) {
    val toggle = LocalTaskToggle.current
    val lineIndex = item.sourceSpans.firstOrNull()?.lineIndex
    val paragraph = item.firstChild as Paragraph
    val full = buildInline(paragraph)
    var skip = 3
    while (skip < full.length && full[skip] == ' ') skip++
    val label = full.subSequence(skip, full.length)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Checkbox(
            checked = checked,
            onCheckedChange = { if (lineIndex != null) toggle?.invoke(lineIndex) },
            enabled = toggle != null && lineIndex != null,
        )
        Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (checked) TextDecoration.LineThrough else null,
                ),
                color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            var sibling = paragraph.next
            while (sibling != null) {
                RenderBlock(sibling)
                sibling = sibling.next
            }
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Text(
            text = code.trimEnd('\n'),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun buildInline(node: Node): AnnotatedString {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    return buildAnnotatedString { appendInline(node, linkColor, codeBackground) }
}

private fun AnnotatedString.Builder.appendInline(node: Node, linkColor: androidx.compose.ui.graphics.Color, codeBackground: androidx.compose.ui.graphics.Color) {
    var child = node.firstChild
    while (child != null) {
        when (val current = child) {
            is MdText -> append(current.literal)
            is Emphasis -> {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendInline(current, linkColor, codeBackground)
                pop()
            }
            is StrongEmphasis -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendInline(current, linkColor, codeBackground)
                pop()
            }
            is Code -> {
                pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground))
                append(current.literal)
                pop()
            }
            is Link -> {
                withLink(
                    LinkAnnotation.Url(
                        current.destination,
                        TextLinkStyles(style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                    ),
                ) {
                    appendInline(current, linkColor, codeBackground)
                }
            }
            is Image -> append(current.title ?: "")
            is SoftLineBreak -> append(" ")
            is HardLineBreak -> append("\n")
            else -> appendInline(current, linkColor, codeBackground)
        }
        child = child.next
    }
}
