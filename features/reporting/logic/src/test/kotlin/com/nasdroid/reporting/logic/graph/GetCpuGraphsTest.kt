package com.nasdroid.reporting.logic.graph

import com.boswelja.percentage.Percentage.Companion.percent
import com.boswelja.temperature.Temperature.Companion.celsius
import com.nasdroid.api.websocket.reporting.ReportingApi
import com.nasdroid.core.strongresult.StrongResult
import com.nasdroid.reporting.logic.DEFAULT_END_SECONDS
import com.nasdroid.reporting.logic.DEFAULT_START_SECONDS
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

class GetCpuGraphsTest {

    private lateinit var reportingApi: ReportingApi

    private lateinit var getCpuGraphs: GetCpuGraphs

    @BeforeTest
    fun setUp() {
        reportingApi = mockk()

        getCpuGraphs = GetCpuGraphs(reportingApi, Dispatchers.Default)
    }

    @Test
    fun `given data is valid, when graphs are requested, then result is four item success`() = runTest {
        mockValidGetGraphData(reportingApi)

        val result = getCpuGraphs()

        assertEquals(StrongResult.success(createExpectedCpuGraphs()), result)
    }

    private fun createExpectedCpuGraphs(): List<Graph<*>> {
        val startInstant = Instant.fromEpochSeconds(DEFAULT_START_SECONDS)
        val endInstant = Instant.fromEpochSeconds(DEFAULT_END_SECONDS)
        val floatSlices = DEFAULT_VALID_DATA.map {
            Graph.DataSlice(Instant.fromEpochSeconds(it.first().toLong()), it.drop(1).map { it.toFloat() })
        }
        return listOf(
            PercentageGraph(
                DEFAULT_VALID_DATA.map {
                    Graph.DataSlice(Instant.fromEpochSeconds(it.first().toLong()), it.drop(1).map { it.percent })
                },
                emptyList(),
                "cpu",
                null,
                startInstant,
                endInstant,
            ),
            TemperatureGraph(
                DEFAULT_VALID_DATA.map {
                    Graph.DataSlice(Instant.fromEpochSeconds(it.first().toLong()), it.drop(1).map { it.celsius })
                },
                emptyList(),
                "cputemp",
                null,
                startInstant,
                Instant.fromEpochSeconds(DEFAULT_START_SECONDS),
            ),
            FloatGraph(floatSlices, emptyList(), "load", null, startInstant, endInstant, "Load"),
            FloatGraph(floatSlices, emptyList(), "processes", null, startInstant, endInstant, "Processes"),
        )
    }

    @Test
    fun `given invalid data, when graphs requested, then error invalid data returned`() = runTest {
        mockInvalidGetGraphData(reportingApi)

        val result = getCpuGraphs()

        assertEquals(
            StrongResult.failure<List<Graph<*>>, ReportingGraphError>(ReportingGraphError.InvalidGraphData),
            result
        )
    }
}
