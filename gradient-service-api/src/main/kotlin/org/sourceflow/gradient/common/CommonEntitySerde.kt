package org.sourceflow.gradient.common

import java.io.File
import java.util.*

object CommonEntitySerde {
    fun from(e: UUID): CommonEntity.UUID {
        return CommonEntity.UUID.newBuilder()
                .setLeastSignificant(e.leastSignificantBits)
                .setMostSignificant(e.mostSignificantBits)
                .build()
    }

    fun to(e: CommonEntity.UUID): UUID {
        return UUID(e.mostSignificant, e.leastSignificant)
    }

    fun fromBoolean(e: Boolean): CommonEntity.Datum {
        return CommonEntity.Datum.newBuilder()
                .setBooleanDatum(e)
                .build()
    }

    fun fromBooleans(e: Iterable<Boolean>, maxLength: Int = 100): CommonEntity.Datum {
        return CommonEntity.Datum.newBuilder()
                .setBooleansDatum(
                        CommonEntity.Booleans.newBuilder()
                                .addAllValues(e.take(maxLength))
                )
                .build()
    }

    fun fromInt(e: Int): CommonEntity.Datum {
        return CommonEntity.Datum.newBuilder()
                .setIntegerDatum(e)
                .build()
    }

    fun fromInts(e: Iterable<Int>, maxLength: Int = 100): CommonEntity.Datum {
        return CommonEntity.Datum.newBuilder()
                .setIntegersDatum(
                        CommonEntity.Integers.newBuilder()
                                .addAllValues(e.take(maxLength))
                )
                .build()
    }

    fun fromLong(e: Long): CommonEntity.Datum {
        return CommonEntity.Datum.newBuilder()
                .setLongDatum(e)
                .build()
    }

    fun fromLongs(e: Iterable<Long>, maxLength: Int = 100): CommonEntity.Datum {
        return CommonEntity.Datum.newBuilder()
                .setLongsDatum(
                        CommonEntity.Longs.newBuilder()
                                .addAllValues(e.take(maxLength))
                )
                .build()
    }

    fun fromFloat(e: Float): CommonEntity.Datum {
        return CommonEntity.Datum.newBuilder()
                .setFloatDatum(e)
                .build()
    }

    fun fromFloats(e: Iterable<Float>, maxLength: Int = 100): CommonEntity.Datum {
        return CommonEntity.Datum.newBuilder()
                .setFloatsDatum(
                        CommonEntity.Floats.newBuilder()
                                .addAllValues(e.take(maxLength))
                )
                .build()
    }

    fun fromDouble(e: Double): CommonEntity.Datum {
        return CommonEntity.Datum.newBuilder()
                .setDoubleDatum(e)
                .build()
    }

    fun fromDoubles(e: Iterable<Double>, maxLength: Int = 100): CommonEntity.Datum {
        return CommonEntity.Datum.newBuilder()
                .setDoublesDatum(
                        CommonEntity.Doubles.newBuilder()
                                .addAllValues(e.take(maxLength))
                )
                .build()
    }

    fun fromString(e: String): CommonEntity.Datum {
        return CommonEntity.Datum.newBuilder()
                .setStringDatum(e)
                .build()
    }

    fun fromStrings(e: Iterable<String>, maxLength: Int = 100): CommonEntity.Datum {
        return CommonEntity.Datum.newBuilder()
                .setStringsDatum(
                        CommonEntity.Strings.newBuilder()
                                .addAllValues(e.take(maxLength))
                )
                .build()
    }

    fun fromClass(e: Class<*>): CommonEntity.Datum {
        return CommonEntity.Datum.newBuilder()
                .setStringDatum(e.canonicalName)
                .build()
    }

    fun fromClasses(e: Iterable<Class<*>>, maxLength: Int = 100): CommonEntity.Datum {
        return CommonEntity.Datum.newBuilder()
                .setStringsDatum(
                        CommonEntity.Strings.newBuilder()
                                .addAllValues(
                                        e.take(maxLength)
                                                .map { it.canonicalName }
                                )
                )
                .build()
    }

    fun fromFile(e: File): CommonEntity.Datum {
        return CommonEntity.Datum.newBuilder()
                .setStringDatum(e.absolutePath)
                .build()
    }

    fun fromFiles(e: Iterable<File>, maxLength: Int = 100): CommonEntity.Datum {
        return CommonEntity.Datum.newBuilder()
                .setStringsDatum(
                        CommonEntity.Strings.newBuilder()
                                .addAllValues(e.take(maxLength).map { it.absolutePath })
                )
                .build()
    }

    fun fromReference(e: Any?): CommonEntity.Datum? {
        return when (e) {
            is Iterable<*> -> fromReferences(e)
            is String -> fromString(e)
            is Boolean -> fromBoolean(e)
            is Char -> fromString(e.toString())
            is Byte -> fromInt(e.toInt())
            is Short -> fromInt(e.toInt())
            is Int -> fromInt(e)
            is Long -> fromLong(e)
            is Float -> fromFloat(e)
            is Double -> fromDouble(e)
            is Class<*> -> fromClass(e)
            is File -> fromFile(e)
            is BooleanArray -> fromBooleans(e.asIterable())
            is IntArray -> fromInts(e.asIterable())
            is LongArray -> fromLongs(e.asIterable())
            is FloatArray -> fromFloats(e.asIterable())
            is DoubleArray -> fromDoubles(e.asIterable())
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun fromReferences(e: Iterable<*>, maxLength: Int = 100): CommonEntity.Datum? {
        val elements = e.take(maxLength)
        val result: CommonEntity.Datum? = null

        elements.getOrNull(0)?.let { firstElement ->
            when (firstElement) {
                is String -> fromStrings(elements.filterIsInstance<String>(), maxLength)
                is Boolean -> fromBooleans(elements.filterIsInstance<Boolean>(), maxLength)
                is Char -> fromStrings(e.map { it.toString() }, maxLength)
                is Byte -> fromInts(
                        e.filterIsInstance<Byte>()
                                .map { it.toInt() }
                        , maxLength
                )
                is Short -> fromInts(
                        e.filterIsInstance<Short>()
                                .map { it.toInt() },
                        maxLength
                )
                is Int -> fromInts(e.filterIsInstance<Int>(), maxLength)
                is Long -> fromLongs(e.filterIsInstance<Long>(), maxLength)
                is Float -> fromFloats(e.filterIsInstance<Float>(), maxLength)
                is Double -> fromDoubles(e.filterIsInstance<Double>(), maxLength)
                is Class<*> -> fromClasses(e.filterIsInstance<Class<*>>(), maxLength)
                is File -> fromFiles(e.filterIsInstance<File>(), maxLength)
                else -> null
            }
        }
        return result
    }

    fun toAny(e: CommonEntity.Datum): Any {
        return when (e.datumSelectionCase) {
            CommonEntity.Datum.DatumSelectionCase.BOOLEAN_DATUM -> toBoolean(e)
            CommonEntity.Datum.DatumSelectionCase.INTEGER_DATUM -> toInt(e)
            CommonEntity.Datum.DatumSelectionCase.LONG_DATUM -> toLong(e)
            CommonEntity.Datum.DatumSelectionCase.FLOAT_DATUM -> toFloat(e)
            CommonEntity.Datum.DatumSelectionCase.DOUBLE_DATUM -> toDouble(e)
            CommonEntity.Datum.DatumSelectionCase.STRING_DATUM -> toString(e)
            else -> error("Only primitives and strings allowed")
        }
    }

    fun toAnyList(e: CommonEntity.Datum): List<Any> {
        return when (e.datumSelectionCase) {
            CommonEntity.Datum.DatumSelectionCase.BOOLEANS_DATUM -> toBooleans(e)
            CommonEntity.Datum.DatumSelectionCase.INTEGERS_DATUM -> toInts(e)
            CommonEntity.Datum.DatumSelectionCase.LONGS_DATUM -> toLongs(e)
            CommonEntity.Datum.DatumSelectionCase.FLOATS_DATUM -> toFloats(e)
            CommonEntity.Datum.DatumSelectionCase.DOUBLES_DATUM -> toDoubles(e)
            CommonEntity.Datum.DatumSelectionCase.STRINGS_DATUM -> toStrings(e)
            else -> error("Only primitive or string list")
        }
    }


    fun toBoolean(e: CommonEntity.Datum): Boolean {
        assert(e.datumSelectionCase == CommonEntity.Datum.DatumSelectionCase.BOOLEAN_DATUM)
        return e.booleanDatum
    }

    fun toInt(e: CommonEntity.Datum): Int {
        assert(e.datumSelectionCase == CommonEntity.Datum.DatumSelectionCase.INTEGER_DATUM)
        return e.integerDatum
    }

    fun toLong(e: CommonEntity.Datum): Long {
        assert(e.datumSelectionCase == CommonEntity.Datum.DatumSelectionCase.LONG_DATUM)
        return e.longDatum
    }

    fun toFloat(e: CommonEntity.Datum): Float {
        assert(e.datumSelectionCase == CommonEntity.Datum.DatumSelectionCase.FLOAT_DATUM)
        return e.floatDatum
    }

    fun toDouble(e: CommonEntity.Datum): Double {
        assert(e.datumSelectionCase == CommonEntity.Datum.DatumSelectionCase.DOUBLE_DATUM)
        return e.doubleDatum
    }

    fun toString(e: CommonEntity.Datum): String {
        assert(e.datumSelectionCase == CommonEntity.Datum.DatumSelectionCase.STRING_DATUM)
        return e.stringDatum
    }

    fun toBooleans(e: CommonEntity.Datum): List<Boolean> {
        assert(e.datumSelectionCase == CommonEntity.Datum.DatumSelectionCase.BOOLEANS_DATUM)
        return e.booleansDatum.valuesList
    }

    fun toInts(e: CommonEntity.Datum): List<Int> {
        assert(e.datumSelectionCase == CommonEntity.Datum.DatumSelectionCase.INTEGERS_DATUM)
        return e.integersDatum.valuesList
    }

    fun toLongs(e: CommonEntity.Datum): List<Long> {
        assert(e.datumSelectionCase == CommonEntity.Datum.DatumSelectionCase.LONGS_DATUM)
        return e.longsDatum.valuesList
    }

    fun toFloats(e: CommonEntity.Datum): List<Float> {
        assert(e.datumSelectionCase == CommonEntity.Datum.DatumSelectionCase.FLOATS_DATUM)
        return e.floatsDatum.valuesList
    }

    fun toDoubles(e: CommonEntity.Datum): List<Double> {
        assert(e.datumSelectionCase == CommonEntity.Datum.DatumSelectionCase.DOUBLES_DATUM)
        return e.doublesDatum.valuesList
    }

    private fun toStrings(e: CommonEntity.Datum): List<String> {
        assert(e.datumSelectionCase == CommonEntity.Datum.DatumSelectionCase.STRINGS_DATUM)
        return e.stringsDatum.valuesList
    }
}
