package com.nasdroid.dashboard.logic.dataloading.memory

import com.boswelja.capacity.Capacity.Companion.gigabytes
import com.boswelja.capacity.CapacityUnit
import com.nasdroid.api.v2.exception.ServerResponseException
import com.nasdroid.api.websocket.system.SystemApi
import com.nasdroid.api.websocket.system.SystemInfo
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetMemorySpecsTest {

    private lateinit var mockSystemApi: SystemApi

    private lateinit var getMemorySpecs: GetMemorySpecs

    @BeforeTest
    fun setUp() {
        mockSystemApi = mockk()

        getMemorySpecs = GetMemorySpecs(mockSystemApi)
    }

    @Test
    fun `when api throws, error is returned`() = runTest {
        coEvery { mockSystemApi.info() } throws ServerResponseException(500, "Internal server error")

        val result = getMemorySpecs()

        assertTrue(result.isFailure)
        assertEquals(
            ServerResponseException(500, "Internal server error"),
            result.exceptionOrNull()
        )
    }

    @Test
    fun `when api returns data, data is returned`() = runTest {
        coEvery { mockSystemApi.info() } returns SystemInfo(
            version = "2023.1.2",
            buildTime = Instant.fromEpochMilliseconds(0),
            hostName = "TrueNAS System",
            physicalMemory = 12.gigabytes.toLong(CapacityUnit.BYTE),
            cpuModel = "Ampere",
            cores = 128,
            physicalCores = 80,
            loadAvg = emptyList(),
            uptime = "12345",
            uptimeSeconds = 12345.0,
            systemSerial = "serial",
            systemProduct = "TrueNAS SCALE",
            systemProductVersion = "2023.1.2",
            license = null,
            bootTime = Instant.fromEpochMilliseconds(0),
            dateTime = Instant.fromEpochMilliseconds(0),
            timeZone = TimeZone.UTC,
            systemManufacturer = "Manufacturer",
            eccMemory = true,
        )

        val result = getMemorySpecs()

        assertTrue(result.isSuccess)
        assertEquals(
            MemorySpecs(
                isEcc = true,
                totalCapacity = 12.gigabytes
            ),
            result.getOrNull()
        )
    }
}
