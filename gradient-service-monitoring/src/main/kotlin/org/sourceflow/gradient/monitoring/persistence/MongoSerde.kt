package org.sourceflow.gradient.monitoring.persistence

import org.bson.Document
import org.sourceflow.gradient.common.CommonEntity
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.monitoring.MonitoringEntity
import org.sourceflow.gradient.monitoring.entity.LinkedFrame

object MongoSerde {
    fun to(e: CommonEntity.Datum): Any? {
        return with(e) {
            when (datumSelectionCase) {
                CommonEntity.Datum.DatumSelectionCase.STRING_DATUM -> stringDatum
                CommonEntity.Datum.DatumSelectionCase.BOOLEAN_DATUM -> booleanDatum
                CommonEntity.Datum.DatumSelectionCase.INTEGER_DATUM -> integerDatum
                CommonEntity.Datum.DatumSelectionCase.FLOAT_DATUM -> floatDatum
                CommonEntity.Datum.DatumSelectionCase.DOUBLE_DATUM -> doubleDatum
                CommonEntity.Datum.DatumSelectionCase.LONG_DATUM -> longDatum
                CommonEntity.Datum.DatumSelectionCase.STRINGS_DATUM -> stringsDatum.valuesList.toList()
                CommonEntity.Datum.DatumSelectionCase.BOOLEANS_DATUM -> booleansDatum.valuesList.toList()
                CommonEntity.Datum.DatumSelectionCase.INTEGERS_DATUM -> integersDatum.valuesList.toList()
                CommonEntity.Datum.DatumSelectionCase.FLOATS_DATUM -> floatsDatum.valuesList.toList()
                CommonEntity.Datum.DatumSelectionCase.DOUBLES_DATUM -> doublesDatum.valuesList.toList()
                CommonEntity.Datum.DatumSelectionCase.LONGS_DATUM -> longsDatum.valuesList.toList()
                CommonEntity.Datum.DatumSelectionCase.DATUMSELECTION_NOT_SET -> null
                else -> {
                    error("All cases should be covered ${datumSelectionCase.name}")
                }
            }
        }
    }

    fun to(e: MonitoringEntity.MonitoringEvent): Document {
        return Document()
                .append("_dtype", "MonitoringEvent")
                .append("type", e.type.name)
                .append("frameId", e.frameId)
                .append("source", e.source)
                .append("target", e.target)
                .append("datumType", e.datum.datumSelectionCase.name)
                .append("datum", to(e.datum))
    }

    fun to(e: LinkedFrame): Document {
        return Document()
                .append("frameId", e.id)
                .append("frameCodeElementId", e.frameElement)
                .append("events", e.events.map { to(it) })
                .append("childFrames", e.childFrames.map { it.id })
    }

    fun to(e: CommonEntity.ProjectContext): Document {
        return Document()
                .append("projectId", CommonEntitySerde.to(e.projectId))
                .append("sessionId", CommonEntitySerde.to(e.sessionId))
    }
}