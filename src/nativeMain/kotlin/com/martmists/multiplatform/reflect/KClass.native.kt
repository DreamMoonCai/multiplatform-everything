package com.martmists.multiplatform.reflect

import kotlin.reflect.KClass

internal fun String.withDollar(): String {
    val parts = this.split(".")
    if (parts.size <= 1) return this

    val stable = mutableListOf<String>()
    val nested = mutableListOf<String>()

    // 从右往左扫描
    for (i in parts.indices.reversed()) {
        val part = parts[i]
        if (part.firstOrNull()?.isUpperCase() == true) {
            nested.add(part)
        } else {
            // 遇到非大写开头，停止，并把该片段和前面的作为 package/class
            stable.addAll(parts.subList(0, i + 1))
            break
        }
    }

    return if (nested.isEmpty()) {
        this
    } else {
        val stablePart = stable.joinToString(".")
        val nestedPart = nested.asReversed().joinToString("$")
        if (stablePart.isEmpty()) nestedPart else "$stablePart.$nestedPart"
    }
}

val KClass<*>.nameWithDollar: String? get() = qualifiedName?.withDollar()