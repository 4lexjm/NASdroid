package com.nasdroid.reporting.logic

import com.nasdroid.api.v2.exception.ClientRequestException
import com.nasdroid.api.websocket.reporting.GraphData
import com.nasdroid.api.websocket.reporting.GraphUnit
import com.nasdroid.api.websocket.reporting.ReportingApi
import com.nasdroid.api.websocket.reporting.RequestedGraph
import io.mockk.coEvery

const val DEFAULT_START_SECONDS = 1714556421L
const val DEFAULT_END_SECONDS = 1714556421L
val DEFAULT_VALID_DATA = listOf(
    listOf(DEFAULT_START_SECONDS.toDouble(), 1.0, 2.0, 3.0),
    listOf(DEFAULT_END_SECONDS.toDouble(), 2.0, 3.0, 4.0),
    listOf(DEFAULT_END_SECONDS.toDouble(), 3.0, 4.0, 5.0)
)

/**
 * Mocks [ReportingApi.getData] to function with valid, reasonable graph data.
 */
fun mockValidGetGraphData(
    target: ReportingApi
) {
    mockGetGraphData(target) {
        GraphData(
            name = it.name.serialName,
            identifier = it.identifier,
            data = DEFAULT_VALID_DATA,
            start = DEFAULT_START_SECONDS,
            end = DEFAULT_END_SECONDS,
            step = null,
            legend = emptyList(),
            aggregations = null
        )
    }
}

/**
 * Mocks [ReportingApi.getData] to function with invalid, unexpected graph data.
 */
fun mockInvalidGetGraphData(
    target: ReportingApi
) {
    mockGetGraphData(target) {
        GraphData(
            name = it.name.serialName,
            identifier = it.identifier,
            data = listOf(
                listOf(DEFAULT_START_SECONDS.toDouble(), 1.0, null, 3.0),
                listOf(DEFAULT_END_SECONDS.toDouble(), null, 3.0, 4.0),
                emptyList()
            ),
            start = DEFAULT_START_SECONDS,
            end = DEFAULT_END_SECONDS,
            step = null,
            legend = emptyList(),
            aggregations = null
        )
    }
}

/**
 * Mocks [ReportingApi.getData] to function as expected. By default, the graphs returned by
 * the mock will have no data.
 */
fun mockGetGraphData(
    target: ReportingApi,
    dataProducer: (RequestedGraph) -> GraphData = {
        GraphData(
            name = it.name.serialName,
            identifier = it.identifier,
            data = emptyList(),
            start = DEFAULT_START_SECONDS,
            end = DEFAULT_END_SECONDS,
            step = null,
            legend = emptyList(),
            aggregations = null
        )
    }
) {
    coEvery {
        target.getData(emptyList(), GraphUnit.Hour, 1)
    } throws ClientRequestException(422, "Parameter graphs is required")
    coEvery {
        target.getData(not(emptyList()), GraphUnit.Hour, 1)
    } answers {
        arg<List<RequestedGraph>>(0).map(dataProducer)
    }
}
