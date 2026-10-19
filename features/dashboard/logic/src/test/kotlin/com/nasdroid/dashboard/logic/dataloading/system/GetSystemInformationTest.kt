package com.nasdroid.dashboard.logic.dataloading.system

import com.boswelja.capacity.Capacity.Companion.gigabytes
import com.boswelja.capacity.CapacityUnit
import com.nasdroid.api.v2.exception.HttpNotOkException
import com.nasdroid.api.websocket.system.SystemApi
import com.nasdroid.api.websocket.system.SystemInfo
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetSystemInformationTest {
    private lateinit var mockSystemApi: SystemApi

    private lateinit var getSystemInformation: GetSystemInformation

    @BeforeTest
    fun setUp() {
        mockSystemApi = mockk()

        getSystemInformation = GetSystemInformation(mockSystemApi)
    }

    @Test
    fun `when api throws, error is returned`() = runTest {
        coEvery { mockSystemApi.info() } throws HttpNotOkException(418, "I'm a teapot")

        val result = getSystemInformation()

        assertTrue(result.isFailure)
        assertEquals(
            HttpNotOkException(418, "I'm a teapot"),
            result.exceptionOrNull()
        )
    }

    @Test
    fun `when api returns data, data is returned`() = runTest {
        val bootTime = LocalDateTime(2023, 10, 8, 22, 18, 33)
        val timeZone = TimeZone.currentSystemDefault()
        coEvery { mockSystemApi.info() } returns SystemInfo(
            version = "2023.1.2",
            buildTime = Instant.fromEpochMilliseconds(0),
            hostName = "truenas",
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
            bootTime = bootTime.toInstant(timeZone),
            dateTime = Instant.fromEpochMilliseconds(0),
            timeZone = timeZone,
            systemManufacturer = "Manufacturer",
            eccMemory = true,
        )

        val result = getSystemInformation()

        assertTrue(result.isSuccess)
        assertEquals(
            SystemInformation(
                version = "2023.1.2",
                hostname = "truenas",
                lastBootTime = bootTime
            ),
            result.getOrNull()
        )
    }
}
