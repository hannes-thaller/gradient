package org.sourceflow.gradient.dataset.service

import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.dataset.entity.*
import org.sourceflow.gradient.monitoring.entities.MonitoringEntities.*
import java.util.*
import kotlin.random.Random


class DatapointFactory(
    private val sessionId: UUID,
    featureDescriptions: List<FeatureDescription>
) {
    companion object {
        private const val CONDITIONAL_ID = -1
        private val DEFAULT_VALUES = mapOf(
            DataTypeDescriptor.BOOLEAN to false,
            DataTypeDescriptor.BOOLEANS to false,
            DataTypeDescriptor.INTEGER to 0,
            DataTypeDescriptor.INTEGERS to 0,
            DataTypeDescriptor.LONG to 0L,
            DataTypeDescriptor.LONGS to 0L,
            DataTypeDescriptor.FLOAT to 0.0f,
            DataTypeDescriptor.FLOATS to 0.0f,
            DataTypeDescriptor.DOUBLE to 0.0,
            DataTypeDescriptor.DOUBLES to 0.0,
            DataTypeDescriptor.STRING to "",
            DataTypeDescriptor.STRINGS to ""
        )
    }

    private val featureDescriptions = featureDescriptions.associateBy { it.elementId }
    var createdDatapoints = 0L

    fun createDatapoints(frames: List<Frame>): List<Datapoint> {
        val datapoints = frames
            .map { frame ->
                assert(frame.eventsList.all { it.frameId == frame.id })
                assert(frame.eventsList.any { it.type == MonitoringEventType.FRAME })

                createDatapoints(frame)
            }
            .flatten()
        createdDatapoints += datapoints.size
        return datapoints
    }

    private fun createDatapoints(frame: Frame): List<Datapoint> {
        val result = mutableListOf<Datapoint>()
        featureDescriptions[frame.frameCodeElementId]?.let { featureDescription ->
            val eventsById = structureFrame(frame)

            val data = featureDescription.inputFeatures.map { feature ->
                val filter = eventFilter(feature, frame.id)
                prepareData(feature, eventsById, filter)
            }

            val maxExtend = data.map { it.size }.maxOrNull() ?: 0
            if (maxExtend > 0) {
                data.map { series ->
                    repeat(maxExtend - series.size) {
                        series.add(series[Random.nextInt(series.size)])
                    }
                    series.shuffle()
                }
                assert(data.all { maxExtend == it.size }) { "Dataset has different feature length after resampling." }

                for (rowIndex in 0 until maxExtend) {
                    val rowData = data.map { it[rowIndex] }
                    result.add(Datapoint(sessionId, featureDescription.id, rowData))
                }
            }
        }

        return result
    }

    private fun structureFrame(frame: Frame): Map<Int, MutableList<MonitoringEvent>> {
        val eventsById = mutableMapOf<Int, MutableList<MonitoringEvent>>()
        addEventsWithId(frame.relatedChildEventsList, eventsById)
        return eventsById
    }

    private fun addEventsWithId(
        events: Iterable<MonitoringEvent>,
        eventsById: MutableMap<Int, MutableList<MonitoringEvent>>
    ) {
        events.forEach {
            when (it.type) {
                MonitoringEventType.FRAME -> eventsById.getOrPut(CONDITIONAL_ID) { mutableListOf() }.add(it)
                MonitoringEventType.RETURN,
                MonitoringEventType.EXCEPT -> eventsById.getOrPut(it.source) { mutableListOf() }.add(it)
                MonitoringEventType.RECEIVE,
                MonitoringEventType.READ,
                MonitoringEventType.WRITE -> eventsById.getOrPut(it.target) { mutableListOf() }.add(it)
                else -> {
                }
            }
        }
    }

    private fun eventFilter(feature: Feature, frameId: Long): (MonitoringEvent) -> Boolean {
        return when (feature.featureType) {
            FeatureType.CONDITIONAL -> {
                { true }
            }
            FeatureType.INPUT_PROPERTY -> {
                {
                    it.type == MonitoringEventType.READ
                }
            }
            FeatureType.INPUT_PARAMETER -> {
                {
                    it.type == MonitoringEventType.RECEIVE &&
                            it.frameId == frameId
                }
            }
            FeatureType.INPUT_RESULT -> {
                {
                    it.type == MonitoringEventType.RETURN &&
                            frameId != it.frameId
                }
            }
            FeatureType.OUTPUT_PROPERTY -> {
                {
                    it.type == MonitoringEventType.WRITE
                }
            }
            FeatureType.OUTPUT_PARAMETER -> {
                {
                    it.type == MonitoringEventType.RECEIVE &&
                            it.frameId != frameId
                }
            }
            FeatureType.OUTPUT_RESULT -> {
                {
                    it.type == MonitoringEventType.RETURN &&
                            it.frameId == frameId
                }
            }
        }
    }

    private fun prepareData(
        feature: Feature,
        eventsById: Map<Int, List<MonitoringEvent>>,
        eventFilter: (MonitoringEvent) -> Boolean
    ): MutableList<Any> {
        val result: MutableList<Any> = mutableListOf()
        eventsById[feature.elementId]
            ?.filter(eventFilter)
            ?.let { events ->
                if (feature.dataType.isIterable()) {
                    events.flatMapTo(result) { CommonEntitySerde.toAnyList(it.datum) }
                } else {
                    if (feature.featureType == FeatureType.CONDITIONAL) {
                        events.find { it.type == MonitoringEventType.FRAME }
                            ?.let { result.add(it.source) }
                    } else {
                        events.mapTo(result) { CommonEntitySerde.toAny(it.datum) }
                    }
                }

                result
            }

        if (result.isEmpty()) {
            result.add(DEFAULT_VALUES.getValue(feature.dataType.descriptor))
        }

        return result
    }
}