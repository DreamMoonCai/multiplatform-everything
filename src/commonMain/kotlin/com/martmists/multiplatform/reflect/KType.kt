package com.martmists.multiplatform.reflect

import kotlin.reflect.KClass
import kotlin.reflect.KType

expect fun KType.withNullability(nullable: Boolean): KType

val KType.asClass: KClass<*>
    get() = classifier as KClass<*>