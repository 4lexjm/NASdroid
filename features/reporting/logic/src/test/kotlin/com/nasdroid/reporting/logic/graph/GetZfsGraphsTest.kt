package com.nasdroid.reporting.logic.graph

import com.boswelja.capacity.Capacity.Companion.mebibytes
import com.boswelja.percentage.Percentage.Companion.percent
import com.nasdroid.api.websocket.reporting.ReportingApi
import com.nasdroid.core.strongresult.StrongResult
import com.nasdroid.reporting.logic.DEFAULT_VALID_DATA
import com.nasdroid.reporting.logic.mockInvalidGetGraphData
import com.nasdroid.reporting.logic.mockValidGetGraphData
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetZfsGraphsTest {

    private lateinit var reportingApi: ReportingApi

    private lateinit var getZfsGraphs: GetZfsGraphs

    @BeforeTest
    fun setUp() {
        reportingApi = mockk()

        getZfsGraphs = GetZfsGraphs(reportingApi, Dispatchers.Default)
    }

    @Test
    fun `given data is valid, when graphs requested, then result is five item success`() = runTest {
        mockValidGetGraphData(reportingApi)

        val result = getZfsGraphs()

        assertEquals(StrongResult.success(createExpectedZfsGraphs()), result)
    }

    private fun createExpectedZfsGraphs(): List<Graph<*>> {
        val defaultInstant = Instant.fromEpochSeconds(1714556421)
        val floatSlices = DEFAULT_VALID_DATA.map {
            Graph.DataSlice(Instant.fromEpochSeconds(it.first().toLong()), it.drop(1).map { it.toFloat() })
        }
        return listOf(
            FloatGraph(floatSlices, emptyList(), "arcactualrate", null, defaultInstant, defaultInstant, "Events/s"),
            FloatGraph(floatSlices, emptyList(), "arcrate", null, defaultInstant, defaultInstant, "Events/s"),
            CapacityGraph(
                DEFAULT_VALID_DATA.map {
                    Graph.DataSlice(Instant.fromEpochSeconds(it.first().toLong()), it.drop(1).map { it.mebibytes })
                },
                emptyList(),
                "arcsize",
                null,
                defaultInstant,
                defaultInstant,
            ),
            PercentageGraph(
                DEFAULT_VALID_DATA.map {
                    Graph.DataSlice(Instant.fromEpochSeconds(it.first().toLong()), it.drop(1).map { it.percent })
                },
                emptyList(),
                "arcresult",
                "demand_data",
                defaultInstant,
                defaultInstant,
            ),
            PercentageGraph(
                DEFAULT_VALID_DATA.map {
                    Graph.DataSlice(Instant.fromEpochSeconds(it.first().toLong()), it.drop(1).map { it.percent })
                },
                emptyList(),
                "arcresult",
                "prefetch_data",
                defaultInstant,
                defaultInstant,
            )
        )
    }

    @Test
    fun `given invalid data, when graphs requested, then error invalid data returned`() = runTest {
        mockInvalidGetGraphData(reportingApi)

        val result = getZfsGraphs()

        assertEquals(
            StrongResult.failure<List<Graph<*>>, ReportingGraphError>(ReportingGraphError.InvalidGraphData),
            result
        )
    }
}
