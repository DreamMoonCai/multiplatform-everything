@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "ERROR_SUPPRESSION")
package com.martmists.multiplatform.reflect

import kotlin.reflect.KFunction
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KMutableProperty2
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KProperty2

val <T,V> KProperty1<T,V>.getter: KFunction1<T,V>
    get() = when (this) {
        is kotlin.native.internal.KProperty1ImplBase<T,V> -> this.getter
        else -> error("Not supported")
    }

val <D,E,V> KProperty2<D,E,V>.getter: KFunction2<D,E,V>
    get() = when (this) {
        is kotlin.native.internal.KProperty2ImplBase<D,E,V> -> this.getter
        else -> error("Not supported")
    }

val <V> KProperty<V>.getter: KFunction<V>
    get() = when (this) {
        is kotlin.native.internal.KProperty0ImplBase -> this.getter
        is kotlin.native.internal.KProperty1ImplBase<*,V> -> this.getter
        is kotlin.native.internal.KProperty2ImplBase<*,*,V> -> this.getter
        else -> error("Not supported")
    }

val <V> KMutableProperty0<V>.setter: KFunction1<V,Unit>
    get() = when (this) {
        is kotlin.native.internal.KMutableProperty0Impl<V> -> this.setter as KFunction1<V, Unit>
        else -> error("Not supported")
    }

val <T,V> KMutableProperty1<T,V>.setter: KFunction2<T,V,Unit>
    get() = when (this) {
        is kotlin.native.internal.KMutableProperty1Impl<T,V> -> this.setter as KFunction2<T, V, Unit>
        else -> error("Not supported")
    }

val <D,E,V> KMutableProperty2<D,E,V>.setter: KFunction3<D,E,V,Unit>
    get() = when (this) {
        is kotlin.native.internal.KMutableProperty2Impl<D,E,V> -> this.setter as KFunction3<D, E, V, Unit>
        else -> error("Not supported")
    }

val KMutableProperty<*>.setter: KFunction<Unit>
    get() = when(this) {
        is kotlin.native.internal.KMutableProperty0Impl<*> -> this.setter as KFunction1<*, Unit>
        is kotlin.native.internal.KMutableProperty1Impl<*,*> -> this.setter as KFunction2<*,*,Unit>
        is kotlin.native.internal.KMutableProperty2Impl<*,*,*> -> this.setter as KFunction3<*,*,*,Unit>
        else -> error("Not supported")
    }