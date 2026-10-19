package com.nasdroid.dashboard.logic.dataloading.cpu

import com.boswelja.capacity.Capacity.Companion.gigabytes
import com.boswelja.capacity.CapacityUnit
import com.nasdroid.api.v2.exception.HttpNotOkException
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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GetCpuSpecsTest {

    private lateinit var mockSystemApi: SystemApi

    private lateinit var getCpuSpecs: GetCpuSpecs

    @BeforeTest
    fun setUp() {
        mockSystemApi = mockk()

        getCpuSpecs = GetCpuSpecs(mockSystemApi)
    }

    @Test
    fun `when api throws http error, failure is returned`() = runTest {
        val expectedException = HttpNotOkException(418, "I'm a teapot")
        coEvery { mockSystemApi.info() } throws expectedException

        val result = getCpuSpecs()
        assertTrue(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
    }

    @Test
    fun `when unknown exception thrown, it is not swallowed`() = runTest {
        val expectedException = IllegalStateException("This shouldn't happen!")
        coEvery { mockSystemApi.info() } throws expectedException

        assertFailsWith<IllegalStateException> {
            getCpuSpecs()
        }
    }

    @Test
    fun `when api returns expected, specs are returned`() = runTest {
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

        val result = getCpuSpecs()

        assertTrue(result.isSuccess)
        assertEquals(
            CpuSpecs(
                model = "Ampere",
                totalCores = 128,
                physicalCores = 80
            ),
            result.getOrNull()
        )
    }
}
