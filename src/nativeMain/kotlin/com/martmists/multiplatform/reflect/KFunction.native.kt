@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "ERROR_SUPPRESSION")

package com.martmists.multiplatform.reflect

import kotlinx.coroutines.Runnable
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlin.collections.component2
import kotlin.native.internal.fullName
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance

private fun kClassForName(className: String): KClass<*>? =
    when (className) {

        // primitive wrapper
        Int::class.nameWithDollar -> Int::class
        Long::class.nameWithDollar -> Long::class
        Byte::class.nameWithDollar -> Byte::class
        Short::class.nameWithDollar -> Short::class
        Double::class.nameWithDollar -> Double::class
        Float::class.nameWithDollar -> Float::class
        Char::class.nameWithDollar -> Char::class
        Boolean::class.nameWithDollar -> Boolean::class

        // primitive array
        IntArray::class.nameWithDollar -> IntArray::class
        LongArray::class.nameWithDollar -> LongArray::class
        ByteArray::class.nameWithDollar -> ByteArray::class
        ShortArray::class.nameWithDollar -> ShortArray::class
        DoubleArray::class.nameWithDollar -> DoubleArray::class
        FloatArray::class.nameWithDollar -> FloatArray::class
        CharArray::class.nameWithDollar -> CharArray::class
        BooleanArray::class.nameWithDollar -> BooleanArray::class

        // kotlin Array<*>
        Array::class.nameWithDollar -> Array::class

        // kotlin builtins
        String::class.nameWithDollar -> String::class
        CharSequence::class.nameWithDollar -> CharSequence::class
        Any::class.nameWithDollar -> Any::class
        Unit::class.nameWithDollar -> Unit::class

        // collections
        List::class.nameWithDollar -> List::class
        MutableList::class.nameWithDollar -> MutableList::class

        Set::class.nameWithDollar -> Set::class
        MutableSet::class.nameWithDollar -> MutableSet::class

        Map::class.nameWithDollar -> Map::class
        MutableMap::class.nameWithDollar -> MutableMap::class

        Collection::class.nameWithDollar -> Collection::class
        MutableCollection::class.nameWithDollar -> MutableCollection::class

        Iterable::class.nameWithDollar -> Iterable::class

        ArrayList::class.nameWithDollar -> ArrayList::class
        LinkedHashMap::class.nameWithDollar -> LinkedHashMap::class

        // functions
        Function0::class.nameWithDollar -> Function0::class
        Function1::class.nameWithDollar -> Function1::class
        Function2::class.nameWithDollar -> Function2::class

        // exceptions
        Throwable::class.nameWithDollar -> Throwable::class
        Exception::class.nameWithDollar -> Exception::class
        RuntimeException::class.nameWithDollar -> RuntimeException::class
        IllegalArgumentException::class.nameWithDollar -> IllegalArgumentException::class

        // java lang
        Runnable::class.nameWithDollar -> Runnable::class
        Number::class.nameWithDollar -> Number::class
        Comparable::class.nameWithDollar -> Comparable::class
        Enum::class.nameWithDollar -> Enum::class
        StringBuilder::class.nameWithDollar -> StringBuilder::class

        // helpers
        Pair::class.nameWithDollar -> Pair::class
        Triple::class.nameWithDollar -> Triple::class

        // kotlinx.serialization
        Json::class.nameWithDollar -> Json::class
        SerializationStrategy::class.nameWithDollar -> SerializationStrategy::class
        DeserializationStrategy::class.nameWithDollar -> DeserializationStrategy::class

        else -> null
    }

private fun createKType(className: String?, isMarkedNullable: Boolean, arguments: List<KType>): KType {
    var variance = KVariance.INVARIANT
    var className = className
    if (className != null && className.contains("|")) {
        val pair = className.split("|")
        variance = when (pair.first()) {
            "out" -> KVariance.OUT
            "in" -> KVariance.IN
            else -> KVariance.INVARIANT
        }
        className = pair.last()
    }
    return object : KType {
        override val classifier: KClassifier by lazy {
            className?.let { kClassForName(it) } ?: object : KClass<Any> {
                override fun equals(other: Any?): Boolean {
                    if (other == null) return false
                    return (other is KClass<*> && other.qualifiedName == className) || other::class.qualifiedName == className
                }

                override fun hashCode(): Int = className.hashCode()

                override fun isInstance(value: Any?): Boolean = equals(value)
                override fun toString(): String = "class $qualifiedName"

                override val qualifiedName: String?
                    get() = className
                override val simpleName: String?
                    get() = className?.substringAfterLast(".")
            }
        }

        override val arguments: List<KTypeProjection> by lazy { arguments.map { KTypeProjection(variance, it) } }

        override val isMarkedNullable: Boolean
            get() = isMarkedNullable

        override fun toString(): String {
            val classifierString = className ?: return "(non-denotable type)"

            return buildString {
                append(classifierString)

                if (arguments.isNotEmpty()) {
                    append('<')

                    arguments.forEachIndexed { index, argument ->
                        if (index > 0) append(", ")

                        append(argument)
                    }

                    append('>')
                }

                if (isMarkedNullable) append('?')
            }
        }
    }
}

class FqParser(val fq: String) {
    val isExpansion by lazy { fq.contains("@") }

    val qualifiedName by lazy {
        buildString {
            if (isExpansion) {
                append(fq.substringBefore("@"))
            } else {
                append(fq.substringBefore("("))
            }
        }
    }

    val declaringClassName by lazy {
        buildString {
            if (isExpansion) {
                append(qualifiedName)
            } else {
                append(qualifiedName.substringBeforeLast("."))
            }
        }
    }

    val declaringClassNameWithDollar by lazy { declaringClassName.withDollar() }

    val expansionClassName by lazy {
        if (isExpansion) {
            buildString { append(fq.substringAfter("@").substringBefore("(")) }
        } else null
    }

    val genericParams: Map<Int, KType> by lazy {
        fq.substringAfter("{").substringBefore("}").run {
            if (isNotEmpty())
                split(";").associate { param ->
                    val (index, type) = param.split("§")
                    index.toInt() to createGenericKType(param)
                }
            else mapOf()
        }
    }

    val param by lazy {
        paramOrNull.map { it ?: createKType(null, false, listOf()) }
    }

    /**
     * 当参数的泛型不在此完全名范围表示时才会获得 null
     */
    val paramOrNull by lazy {
        fq.substringAfter("(").substringBefore(")").run {
            if (isNotEmpty())
                split(";").map { param ->
                    val generic = param.substringAfter("<", "").substringBefore(">", "")
                    if (generic.isEmpty() && param.contains(":")) {
                        val (first, last) = param.split(":")
                        genericParams[last.toInt()]
                    } else {
                        val clazz = if (param.substringBefore("<").last() == '?') {
                            param.substringBefore("<").substringBeforeLast("?")
                        } else {
                            param.substringBefore("<")
                        }
                        createKType(
                            clazz,
                            param.last() == '?',
                            if (generic.isEmpty()) listOf() else generic.split(",").map { createGenericKType(it) })
                    }
                }
            else listOf()
        }
    }

    // <Type?> or <Type<Type2,Type3>>
    fun createGenericKType(typeSig: String): KType {
        val param = typeSig.substringAfter("<").substringBefore(">")
        val generic = param.substringAfter("<", "").substringBefore(">", "")
        fun create(): KType {
            val clazz = if (param.substringBefore("<").last() == '?') {
                param.substringBefore("<").substringBeforeLast("?")
            } else {
                param.substringBefore("<")
            }

            return createKType(
                clazz,
                param.last() == '?',
                if (generic.isEmpty()) listOf() else generic.split(",").map { createGenericKType(it) })
        }
        return if (generic.isEmpty() && param.contains(":")) {
            val (first, last) = param.split(":")
            genericParams[last.toInt()] ?: create()
        } else create()
    }
}

fun <R> Function<R>.asKFunction() = this as KFunction<R>

val KFunction<*>.fqName: String
    get() = ((this as kotlin.native.internal.KFunctionImpl).description as kotlin.native.internal.KFunctionDescription.Correct).fqName

val KFunction<*>.propertyName: String
    get() {
        val raw = this.name
        val params = params
        val isBoolean = (params.isEmpty() && this.returnType.classifier == Boolean::class) || (params.size == 1 && this.returnType.classifier == Unit::class && params.first()?.classifier == Boolean::class)

        fun format(type: String): String {
            val inner = raw.removePrefix("<$type-").removeSuffix(">")

            // Boolean getter
            if (type == "get" && isBoolean) {
                // 已经以 isXxx 命名的属性
                if (inner.startsWith("is") && inner.length > 2 && inner[2].isUpperCase()) {
                    return inner
                }
                // 否则构造 isXxx
                return "is" + inner.replaceFirstChar { it.uppercase() }
            }

            // Boolean setter
            if (type == "set" && isBoolean) {
                // 情况 1：属性原名是 isXxx，那么 setter 是 setXxx
                if (inner.startsWith("is") && inner.length > 2 && inner[2].isUpperCase()) {
                    val stripped = inner.removePrefix("is")
                    val cap = stripped.replaceFirstChar { it.uppercase() }
                    return "is$cap"
                }
                // 情况 2：普通 Boolean 属性，setter 与普通一致（但属性名是 isXxx）
                val cap = inner.replaceFirstChar { it.uppercase() }
                return "is$cap"
            }

            // 普通属性逻辑
            val prop = inner.split("-").joinToString("") {
                it.replaceFirstChar { c -> c.uppercase() }
            }
            return type + prop
        }

        return when {
            raw.startsWith("<set-") -> format("set")
            raw.startsWith("<get-") -> format("get")
            else -> raw
        }
    }

val KFunction<*>.qualifiedName: String
    get() = FqParser(fqName).qualifiedName

val KFunction<*>.isExpansion: Boolean
    get() = FqParser(fqName).isExpansion

val KFunction<*>.declaringClassName: String
    get() = FqParser(fqName).declaringClassName

val KFunction<*>.declaringClassNameWithDollar: String
    get() = FqParser(fqName).declaringClassNameWithDollar

val KFunction<*>.params: List<KType?>
    get() = FqParser(fqName).param

val KFunction<*>.genericParams: Map<Int, KType>
    get() = FqParser(fqName).genericParams