package org.sourceflow.gradient.common

import org.sourceflow.gradient.common.entities.*
import org.sourceflow.gradient.common.entities.UUID as GUUID



import java.io.File
import java.util.UUID

object CommonEntitySerde {
    fun fromUUID(e: UUID): GUUID {
        return GUUID.newBuilder()
                .setLeastSignificant(e.leastSignificantBits)
                .setMostSignificant(e.mostSignificantBits)
                .build()
    }

    fun toUUID(e: GUUID): UUID {
        return UUID(e.mostSignificant, e.leastSignificant)
    }

    fun fromBoolean(e: Boolean): Datum {
        return Datum.newBuilder()
                .setBooleanDatum(e)
                .build()
    }

    fun fromBooleans(e: Iterable<Boolean>, maxLength: Int = 100): Datum {
        return Datum.newBuilder()
                .setBooleansDatum(
                        Booleans.newBuilder()
                                .addAllValues(e.take(maxLength))
                )
                .build()
    }

    fun fromInt(e: Int): Datum {
        return Datum.newBuilder()
                .setIntegerDatum(e)
                .build()
    }

    fun fromInts(e: Iterable<Int>, maxLength: Int = 100): Datum {
        return Datum.newBuilder()
                .setIntegersDatum(
                        Integers.newBuilder()
                                .addAllValues(e.take(maxLength))
                )
                .build()
    }

    fun fromLong(e: Long): Datum {
        return Datum.newBuilder()
                .setLongDatum(e)
                .build()
    }

    fun fromLongs(e: Iterable<Long>, maxLength: Int = 100): Datum {
        return Datum.newBuilder()
                .setLongsDatum(
                        Longs.newBuilder()
                                .addAllValues(e.take(maxLength))
                )
                .build()
    }

    fun fromFloat(e: Float): Datum {
        return Datum.newBuilder()
                .setFloatDatum(e)
                .build()
    }

    fun fromFloats(e: Iterable<Float>, maxLength: Int = 100): Datum {
        return Datum.newBuilder()
                .setFloatsDatum(
                        Floats.newBuilder()
                                .addAllValues(e.take(maxLength))
                )
                .build()
    }

    fun fromDouble(e: Double): Datum {
        return Datum.newBuilder()
                .setDoubleDatum(e)
                .build()
    }

    fun fromDoubles(e: Iterable<Double>, maxLength: Int = 100): Datum {
        return Datum.newBuilder()
                .setDoublesDatum(
                        Doubles.newBuilder()
                                .addAllValues(e.take(maxLength))
                )
                .build()
    }

    fun fromString(e: String): Datum {
        return Datum.newBuilder()
                .setStringDatum(e)
                .build()
    }

    fun fromStrings(e: Iterable<String>, maxLength: Int = 100): Datum {
        return Datum.newBuilder()
                .setStringsDatum(
                        Strings.newBuilder()
                                .addAllValues(e.take(maxLength))
                )
                .build()
    }

    fun fromClass(e: Class<*>): Datum {
        return Datum.newBuilder()
                .setStringDatum(e.canonicalName)
                .build()
    }

    fun fromClasses(e: Iterable<Class<*>>, maxLength: Int = 100): Datum {
        return Datum.newBuilder()
                .setStringsDatum(
                        Strings.newBuilder()
                                .addAllValues(
                                        e.take(maxLength)
                                                .map { it.canonicalName }
                                )
                )
                .build()
    }

    fun fromFile(e: File): Datum {
        return Datum.newBuilder()
                .setStringDatum(e.absolutePath)
                .build()
    }

    fun fromFiles(e: Iterable<File>, maxLength: Int = 100): Datum {
        return Datum.newBuilder()
                .setStringsDatum(
                        Strings.newBuilder()
                                .addAllValues(e.take(maxLength).map { it.absolutePath })
                )
                .build()
    }

    fun fromReference(e: Any?): Datum? {
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
    fun fromReferences(e: Iterable<*>, maxLength: Int = 100): Datum? {
        val elements = e.take(maxLength)
        val result: Datum? = null

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

    fun toAny(e: Datum): Any {
        return when (e.datumSelectionCase) {
            Datum.DatumSelectionCase.BOOLEAN_DATUM -> toBoolean(e)
            Datum.DatumSelectionCase.INTEGER_DATUM -> toInt(e)
            Datum.DatumSelectionCase.LONG_DATUM -> toLong(e)
            Datum.DatumSelectionCase.FLOAT_DATUM -> toFloat(e)
            Datum.DatumSelectionCase.DOUBLE_DATUM -> toDouble(e)
            Datum.DatumSelectionCase.STRING_DATUM -> toString(e)
            else -> error("Only primitives and strings allowed")
        }
    }

    fun toAnyList(e: Datum): List<Any> {
        return when (e.datumSelectionCase) {
            Datum.DatumSelectionCase.BOOLEANS_DATUM -> toBooleans(e)
            Datum.DatumSelectionCase.INTEGERS_DATUM -> toInts(e)
            Datum.DatumSelectionCase.LONGS_DATUM -> toLongs(e)
            Datum.DatumSelectionCase.FLOATS_DATUM -> toFloats(e)
            Datum.DatumSelectionCase.DOUBLES_DATUM -> toDoubles(e)
            Datum.DatumSelectionCase.STRINGS_DATUM -> toStrings(e)
            else -> error("Only primitive or string list")
        }
    }


    fun toBoolean(e: Datum): Boolean {
        assert(e.datumSelectionCase == Datum.DatumSelectionCase.BOOLEAN_DATUM)
        return e.booleanDatum
    }

    fun toInt(e: Datum): Int {
        assert(e.datumSelectionCase == Datum.DatumSelectionCase.INTEGER_DATUM)
        return e.integerDatum
    }

    fun toLong(e: Datum): Long {
        assert(e.datumSelectionCase == Datum.DatumSelectionCase.LONG_DATUM)
        return e.longDatum
    }

    fun toFloat(e: Datum): Float {
        assert(e.datumSelectionCase == Datum.DatumSelectionCase.FLOAT_DATUM)
        return e.floatDatum
    }

    fun toDouble(e: Datum): Double {
        assert(e.datumSelectionCase == Datum.DatumSelectionCase.DOUBLE_DATUM)
        return e.doubleDatum
    }

    fun toString(e: Datum): String {
        assert(e.datumSelectionCase == Datum.DatumSelectionCase.STRING_DATUM)
        return e.stringDatum
    }

    fun toBooleans(e: Datum): List<Boolean> {
        assert(e.datumSelectionCase == Datum.DatumSelectionCase.BOOLEANS_DATUM)
        return e.booleansDatum.valuesList
    }

    fun toInts(e: Datum): List<Int> {
        assert(e.datumSelectionCase == Datum.DatumSelectionCase.INTEGERS_DATUM)
        return e.integersDatum.valuesList
    }

    fun toLongs(e: Datum): List<Long> {
        assert(e.datumSelectionCase == Datum.DatumSelectionCase.LONGS_DATUM)
        return e.longsDatum.valuesList
    }

    fun toFloats(e: Datum): List<Float> {
        assert(e.datumSelectionCase == Datum.DatumSelectionCase.FLOATS_DATUM)
        return e.floatsDatum.valuesList
    }

    fun toDoubles(e: Datum): List<Double> {
        assert(e.datumSelectionCase == Datum.DatumSelectionCase.DOUBLES_DATUM)
        return e.doublesDatum.valuesList
    }

    private fun toStrings(e: Datum): List<String> {
        assert(e.datumSelectionCase == Datum.DatumSelectionCase.STRINGS_DATUM)
        return e.stringsDatum.valuesList
    }
}
