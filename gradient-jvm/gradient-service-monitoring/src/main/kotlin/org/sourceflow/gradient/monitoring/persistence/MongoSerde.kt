package org.sourceflow.gradient.monitoring.persistence

import org.bson.Document
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.common.entities.CommonEntities
import org.sourceflow.gradient.monitoring.entities.MonitoringEntities
import org.sourceflow.gradient.monitoring.entity.LinkedFrame

object MongoSerde {
    fun to(e: CommonEntities.Datum): Any? {
        return with(e) {
            when (datumSelectionCase) {
                CommonEntities.Datum.DatumSelectionCase.STRING_DATUM -> stringDatum
                CommonEntities.Datum.DatumSelectionCase.BOOLEAN_DATUM -> booleanDatum
                CommonEntities.Datum.DatumSelectionCase.INTEGER_DATUM -> integerDatum
                CommonEntities.Datum.DatumSelectionCase.FLOAT_DATUM -> floatDatum
                CommonEntities.Datum.DatumSelectionCase.DOUBLE_DATUM -> doubleDatum
                CommonEntities.Datum.DatumSelectionCase.LONG_DATUM -> longDatum
                CommonEntities.Datum.DatumSelectionCase.STRINGS_DATUM -> stringsDatum.valuesList.toList()
                CommonEntities.Datum.DatumSelectionCase.BOOLEANS_DATUM -> booleansDatum.valuesList.toList()
                CommonEntities.Datum.DatumSelectionCase.INTEGERS_DATUM -> integersDatum.valuesList.toList()
                CommonEntities.Datum.DatumSelectionCase.FLOATS_DATUM -> floatsDatum.valuesList.toList()
                CommonEntities.Datum.DatumSelectionCase.DOUBLES_DATUM -> doublesDatum.valuesList.toList()
                CommonEntities.Datum.DatumSelectionCase.LONGS_DATUM -> longsDatum.valuesList.toList()
                CommonEntities.Datum.DatumSelectionCase.DATUMSELECTION_NOT_SET -> null
                else -> {
                    error("All cases should be covered ${datumSelectionCase.name}")
                }
            }
        }
    }

    fun to(e: MonitoringEntities.MonitoringEvent): Document {
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

    fun to(e: CommonEntities.ProjectContext): Document {
        return Document()
            .append("projectId", CommonEntitySerde.toUUID(e.projectId))
            .append("sessionId", CommonEntitySerde.toUUID(e.sessionId))
    }
}