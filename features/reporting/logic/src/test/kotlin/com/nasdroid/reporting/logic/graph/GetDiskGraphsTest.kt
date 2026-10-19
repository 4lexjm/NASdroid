package com.nasdroid.reporting.logic.graph

import com.boswelja.capacity.Capacity.Companion.kibibytes
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

class GetDiskGraphsTest {

    private lateinit var reportingApi: ReportingApi

    private lateinit var getDiskGraphs: GetDiskGraphs

    @BeforeTest
    fun setUp() {
        reportingApi = mockk()

        getDiskGraphs = GetDiskGraphs(reportingApi, Dispatchers.Default)
    }

    @Test
    fun `given data is valid, when zero disks are requested, then result is empty success`() = runTest {
        mockValidGetGraphData(reportingApi)

        val result = getDiskGraphs(emptyList())

        assertEquals(StrongResult.success(emptyList()), result)
    }

    @Test
    fun `given data is valid, when one disk is requested, then result is two item success`() = runTest {
        mockValidGetGraphData(reportingApi)

        val result = getDiskGraphs(listOf("sda"))

        assertEquals(
            StrongResult.success(
                listOf(
                    CapacityGraph(
                        dataSlices = DEFAULT_VALID_DATA.map {
                            Graph.DataSlice(
                                timestamp = Instant.fromEpochSeconds(it.first().toLong()),
                                data = it.drop(1).map { it.kibibytes }
                            )
                        },
                        legend = emptyList(),
                        name = "disk",
                        identifier = "sda",
                        start = Instant.fromEpochSeconds(DEFAULT_START_SECONDS),
                        end = Instant.fromEpochSeconds(DEFAULT_END_SECONDS)
                    ),
                    TemperatureGraph(
                        dataSlices = DEFAULT_VALID_DATA.map {
                            Graph.DataSlice(
                                timestamp = Instant.fromEpochSeconds(it.first().toLong()),
                                data = it.drop(1).map { it.celsius }
                            )
                        },
                        legend = emptyList(),
                        name = "disktemp",
                        identifier = "sda",
                        start = Instant.fromEpochSeconds(DEFAULT_START_SECONDS),
                        end = Instant.fromEpochSeconds(DEFAULT_END_SECONDS)
                    )
                )
            ),
            result
        )
    }

    @Test
    fun `given invalid data, when graphs requested, then error invalid data returned`() = runTest {
        mockInvalidGetGraphData(reportingApi)

        val result = getDiskGraphs(listOf("sda"))

        assertEquals(
            StrongResult.failure<List<Graph<*>>, ReportingGraphError>(ReportingGraphError.InvalidGraphData),
            result
        )
    }
}
