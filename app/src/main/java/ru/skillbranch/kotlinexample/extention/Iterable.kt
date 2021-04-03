package ru.skillbranch.kotlinexample.extention

fun <E> List<E>.dropLastUntil2(predicate: (E) -> Boolean): List<E> {
    val result = this.asReversed().toMutableList()
    val iterator = result.iterator()

    while (iterator.hasNext()) {
        val id = iterator.next()
        if (predicate.invoke(id)) {
            iterator.remove()
            break
        }
        iterator.remove()
    }
    return result
}