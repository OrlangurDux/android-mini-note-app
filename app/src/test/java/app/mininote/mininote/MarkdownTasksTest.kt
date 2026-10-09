package app.mininote.mininote

import app.mininote.mininote.util.taskStateOf
import app.mininote.mininote.util.tasksFromEditor
import app.mininote.mininote.util.tasksToEditor
import app.mininote.mininote.util.toggleTaskAtLine
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownTasksTest {
    @Test fun checksUnchecked() = assertEquals("- [x] a\n- [ ] b", toggleTaskAtLine("- [ ] a\n- [ ] b", 0))
    @Test fun uncheckChecked() = assertEquals("- [ ] a", toggleTaskAtLine("- [x] a", 0))
    @Test fun uppercaseX() = assertEquals("* [ ] a", toggleTaskAtLine("* [X] a", 0))
    @Test fun nestedAndOrdered() = assertEquals("1. a\n   - [x] b", toggleTaskAtLine("1. a\n   - [ ] b", 1))
    @Test fun nonTaskLineUntouched() = assertEquals("- plain [ ] x", toggleTaskAtLine("- plain [ ] x", 0))
    @Test fun outOfRangeUntouched() = assertEquals("- [ ] a", toggleTaskAtLine("- [ ] a", 5))
}

class TaskStateTest {
    private val parser = org.commonmark.parser.Parser.builder()
        .includeSourceSpans(org.commonmark.parser.IncludeSourceSpans.BLOCKS).build()

    @Test fun detectsStatesAndLines() {
        val doc = parser.parse("- [ ] a\n- [x] **b**\n- plain\n\n```\n- [ ] code\n```")
        val items = generateSequence(doc.firstChild?.firstChild) { it.next }
            .filterIsInstance<org.commonmark.node.ListItem>().toList()
        assertEquals(listOf(false, true, null), items.map { taskStateOf(it) })
        assertEquals(listOf(0, 1, 2), items.map { it.sourceSpans.first().lineIndex })
    }
}

class EditorTasksTest {
    @Test fun toEditorConvertsAllVariants() {
        val md = "- [ ] a\n* [x] b\n1. [X] c\n- [] broken\n- plain\n"
        assertEquals("- \u2610 a\n* \u2611 b\n1. \u2611 c\n- \u2610 broken\n- plain\n", tasksToEditor(md))
    }

    @Test fun roundTrip() {
        val md = "- [ ] a\n- [x] b\n\ntext [ ] inline"
        assertEquals(md, tasksFromEditor(tasksToEditor(md)))
    }
}
