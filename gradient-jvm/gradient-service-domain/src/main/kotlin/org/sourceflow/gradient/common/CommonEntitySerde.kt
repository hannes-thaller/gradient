package org.sourceflow.gradient.common

import org.sourceflow.gradient.common.entities.CommonEntities
import java.io.File
import java.util.UUID

object CommonEntitySerde {
    fun fromUUID(e: UUID): CommonEntities.UUID {
        return CommonEntities.UUID.newBuilder()
            .setLeastSignificant(e.leastSignificantBits)
            .setMostSignificant(e.mostSignificantBits)
            .build()
    }

    fun toUUID(e: CommonEntities.UUID): UUID {
        return UUID(e.mostSignificant, e.leastSignificant)
    }

    fun fromBoolean(e: Boolean): CommonEntities.Datum {
        return CommonEntities.Datum.newBuilder()
            .setBooleanDatum(e)
            .build()
    }

    fun fromBooleans(e: Iterable<Boolean>, maxLength: Int = 100): CommonEntities.Datum {
        return CommonEntities.Datum.newBuilder()
            .setBooleansDatum(
                CommonEntities.Booleans.newBuilder()
                    .addAllValues(e.take(maxLength))
            )
            .build()
    }

    fun fromInt(e: Int): CommonEntities.Datum {
        return CommonEntities.Datum.newBuilder()
            .setIntegerDatum(e)
            .build()
    }

    fun fromInts(e: Iterable<Int>, maxLength: Int = 100): CommonEntities.Datum {
        return CommonEntities.Datum.newBuilder()
            .setIntegersDatum(
                CommonEntities.Integers.newBuilder()
                    .addAllValues(e.take(maxLength))
            )
            .build()
    }

    fun fromLong(e: Long): CommonEntities.Datum {
        return CommonEntities.Datum.newBuilder()
            .setLongDatum(e)
            .build()
    }

    fun fromLongs(e: Iterable<Long>, maxLength: Int = 100): CommonEntities.Datum {
        return CommonEntities.Datum.newBuilder()
            .setLongsDatum(
                CommonEntities.Longs.newBuilder()
                    .addAllValues(e.take(maxLength))
            )
            .build()
    }

    fun fromFloat(e: Float): CommonEntities.Datum {
        return CommonEntities.Datum.newBuilder()
            .setFloatDatum(e)
            .build()
    }

    fun fromFloats(e: Iterable<Float>, maxLength: Int = 100): CommonEntities.Datum {
        return CommonEntities.Datum.newBuilder()
            .setFloatsDatum(
                CommonEntities.Floats.newBuilder()
                    .addAllValues(e.take(maxLength))
            )
            .build()
    }

    fun fromDouble(e: Double): CommonEntities.Datum {
        return CommonEntities.Datum.newBuilder()
            .setDoubleDatum(e)
            .build()
    }

    fun fromDoubles(e: Iterable<Double>, maxLength: Int = 100): CommonEntities.Datum {
        return CommonEntities.Datum.newBuilder()
            .setDoublesDatum(
                CommonEntities.Doubles.newBuilder()
                    .addAllValues(e.take(maxLength))
            )
            .build()
    }

    fun fromString(e: String): CommonEntities.Datum {
        return CommonEntities.Datum.newBuilder()
            .setStringDatum(e)
            .build()
    }

    fun fromStrings(e: Iterable<String>, maxLength: Int = 100): CommonEntities.Datum {
        return CommonEntities.Datum.newBuilder()
            .setStringsDatum(
                CommonEntities.Strings.newBuilder()
                    .addAllValues(e.take(maxLength))
            )
            .build()
    }

    fun fromClass(e: Class<*>): CommonEntities.Datum {
        return CommonEntities.Datum.newBuilder()
            .setStringDatum(e.canonicalName)
            .build()
    }

    fun fromClasses(e: Iterable<Class<*>>, maxLength: Int = 100): CommonEntities.Datum {
        return CommonEntities.Datum.newBuilder()
            .setStringsDatum(
                CommonEntities.Strings.newBuilder()
                    .addAllValues(
                        e.take(maxLength)
                            .map { it.canonicalName }
                    )
            )
            .build()
    }

    fun fromFile(e: File): CommonEntities.Datum {
        return CommonEntities.Datum.newBuilder()
            .setStringDatum(e.absolutePath)
            .build()
    }

    fun fromFiles(e: Iterable<File>, maxLength: Int = 100): CommonEntities.Datum {
        return CommonEntities.Datum.newBuilder()
            .setStringsDatum(
                CommonEntities.Strings.newBuilder()
                    .addAllValues(e.take(maxLength).map { it.absolutePath })
            )
            .build()
    }

    fun fromReference(e: Any?): CommonEntities.Datum? {
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
    fun fromReferences(e: Iterable<*>, maxLength: Int = 100): CommonEntities.Datum? {
        val elements = e.take(maxLength)
        val result: CommonEntities.Datum? = null

        elements.getOrNull(0)?.let { firstElement ->
            when (firstElement) {
                is String -> fromStrings(elements.filterIsInstance<String>(), maxLength)
                is Boolean -> fromBooleans(elements.filterIsInstance<Boolean>(), maxLength)
                is Char -> fromStrings(e.map { it.toString() }, maxLength)
                is Byte -> fromInts(
                    e.filterIsInstance<Byte>()
                        .map { it.toInt() }, maxLength
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

    fun toAny(e: CommonEntities.Datum): Any {
        return when (e.datumSelectionCase) {
            CommonEntities.Datum.DatumSelectionCase.BOOLEAN_DATUM -> toBoolean(e)
            CommonEntities.Datum.DatumSelectionCase.INTEGER_DATUM -> toInt(e)
            CommonEntities.Datum.DatumSelectionCase.LONG_DATUM -> toLong(e)
            CommonEntities.Datum.DatumSelectionCase.FLOAT_DATUM -> toFloat(e)
            CommonEntities.Datum.DatumSelectionCase.DOUBLE_DATUM -> toDouble(e)
            CommonEntities.Datum.DatumSelectionCase.STRING_DATUM -> toString(e)
            else -> error("Only primitives and strings allowed")
        }
    }

    fun toAnyList(e: CommonEntities.Datum): List<Any> {
        return when (e.datumSelectionCase) {
            CommonEntities.Datum.DatumSelectionCase.BOOLEANS_DATUM -> toBooleans(e)
            CommonEntities.Datum.DatumSelectionCase.INTEGERS_DATUM -> toInts(e)
            CommonEntities.Datum.DatumSelectionCase.LONGS_DATUM -> toLongs(e)
            CommonEntities.Datum.DatumSelectionCase.FLOATS_DATUM -> toFloats(e)
            CommonEntities.Datum.DatumSelectionCase.DOUBLES_DATUM -> toDoubles(e)
            CommonEntities.Datum.DatumSelectionCase.STRINGS_DATUM -> toStrings(e)
            else -> error("Only primitive or string list")
        }
    }


    fun toBoolean(e: CommonEntities.Datum): Boolean {
        assert(e.datumSelectionCase == CommonEntities.Datum.DatumSelectionCase.BOOLEAN_DATUM)
        return e.booleanDatum
    }

    fun toInt(e: CommonEntities.Datum): Int {
        assert(e.datumSelectionCase == CommonEntities.Datum.DatumSelectionCase.INTEGER_DATUM)
        return e.integerDatum
    }

    fun toLong(e: CommonEntities.Datum): Long {
        assert(e.datumSelectionCase == CommonEntities.Datum.DatumSelectionCase.LONG_DATUM)
        return e.longDatum
    }

    fun toFloat(e: CommonEntities.Datum): Float {
        assert(e.datumSelectionCase == CommonEntities.Datum.DatumSelectionCase.FLOAT_DATUM)
        return e.floatDatum
    }

    fun toDouble(e: CommonEntities.Datum): Double {
        assert(e.datumSelectionCase == CommonEntities.Datum.DatumSelectionCase.DOUBLE_DATUM)
        return e.doubleDatum
    }

    fun toString(e: CommonEntities.Datum): String {
        assert(e.datumSelectionCase == CommonEntities.Datum.DatumSelectionCase.STRING_DATUM)
        return e.stringDatum
    }

    fun toBooleans(e: CommonEntities.Datum): List<Boolean> {
        assert(e.datumSelectionCase == CommonEntities.Datum.DatumSelectionCase.BOOLEANS_DATUM)
        return e.booleansDatum.valuesList
    }

    fun toInts(e: CommonEntities.Datum): List<Int> {
        assert(e.datumSelectionCase == CommonEntities.Datum.DatumSelectionCase.INTEGERS_DATUM)
        return e.integersDatum.valuesList
    }

    fun toLongs(e: CommonEntities.Datum): List<Long> {
        assert(e.datumSelectionCase == CommonEntities.Datum.DatumSelectionCase.LONGS_DATUM)
        return e.longsDatum.valuesList
    }

    fun toFloats(e: CommonEntities.Datum): List<Float> {
        assert(e.datumSelectionCase == CommonEntities.Datum.DatumSelectionCase.FLOATS_DATUM)
        return e.floatsDatum.valuesList
    }

    fun toDoubles(e: CommonEntities.Datum): List<Double> {
        assert(e.datumSelectionCase == CommonEntities.Datum.DatumSelectionCase.DOUBLES_DATUM)
        return e.doublesDatum.valuesList
    }

    private fun toStrings(e: CommonEntities.Datum): List<String> {
        assert(e.datumSelectionCase == CommonEntities.Datum.DatumSelectionCase.STRINGS_DATUM)
        return e.stringsDatum.valuesList
    }
}
