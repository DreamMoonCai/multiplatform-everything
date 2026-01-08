@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "ERROR_SUPPRESSION")

package com.martmists.multiplatform.reflect

import kotlin.reflect.KType
import kotlin.reflect.KTypeImpl

actual fun KType.withNullability(nullable: Boolean): KType {
    if (nullable == isMarkedNullable) return this
    return KTypeImpl(classifier, arguments, nullable)
}
