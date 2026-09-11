package app.mininote.mininote.util

import app.mininote.mininote.data.local.db.entity.CategoryEntity

/** Категория вместе с глубиной вложенности — для отступа/группировки в дереве или чипах. */
data class CategoryNode(val category: CategoryEntity, val depth: Int)

/** Плоский список в порядке обхода дерева в глубину: родитель, затем сразу все его потомки. */
fun buildCategoryTree(categories: List<CategoryEntity>): List<CategoryNode> {
    val result = mutableListOf<CategoryNode>()
    fun addChildren(parentLocalId: Long?, depth: Int) {
        categories.filter { it.parentLocalId == parentLocalId }.forEach { category ->
            result += CategoryNode(category, depth)
            addChildren(category.localId, depth + 1)
        }
    }
    addChildren(null, 0)
    return result
}

/** Все потомки категории (рекурсивно, не включая её саму). */
fun descendantsOf(localId: Long, categories: List<CategoryEntity>): Set<Long> {
    val result = mutableSetOf<Long>()
    var frontier = listOf(localId)
    while (frontier.isNotEmpty()) {
        val children = categories.filter { it.parentLocalId in frontier }.map { it.localId }
        result += children
        frontier = children
    }
    return result
}
