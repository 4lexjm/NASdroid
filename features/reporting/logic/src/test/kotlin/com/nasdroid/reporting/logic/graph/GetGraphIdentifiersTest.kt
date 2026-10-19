package com.nasdroid.reporting.logic.graph

import com.nasdroid.api.websocket.reporting.ReportingApi
import com.nasdroid.api.websocket.reporting.ReportingGraph
import com.nasdroid.reporting.data.metadata.CachedGraphMetadata
import com.nasdroid.reporting.data.metadata.GraphMetadataCache
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetGraphIdentifiersTest {

    private lateinit var graphMetadataCache: GraphMetadataCache
    private lateinit var reportingApi: ReportingApi

    private lateinit var getGraphIdentifiers: GetGraphIdentifiers

    @BeforeTest
    fun setUp() {
        graphMetadataCache = mockk()
        reportingApi = mockk()

        getGraphIdentifiers = GetGraphIdentifiers(reportingApi, graphMetadataCache)

        coEvery { graphMetadataCache.submitGraphMetadata(any()) } just Runs
    }

    @Test
    fun `given there is no data in cache, when invoked once, then one network request is made`() = runTest {
        every { graphMetadataCache.getGraphMetadata(any()) } returns MutableStateFlow(null)
        coEvery { reportingApi.graphs(any(), any()) } returns listOf(
            ReportingGraph(
                "name",
                "title",
                "verticalLabel",
                null
            )
        )

        getGraphIdentifiers("test").first()

        coVerify(exactly = 1) { reportingApi.graphs(null, null) }
        coVerify(exactly = 1) {
            graphMetadataCache.submitGraphMetadata(
                listOf(
                    CachedGraphMetadata(
                        name = "name",
                        title = "title",
                        verticalLabel = "verticalLabel",
                        identifiers = null
                    )
                )
            )
        }
    }

    @Test
    fun `given there is data in cache, when invoked once, then no network request is made`() = runTest {
        every { graphMetadataCache.getGraphMetadata(any()) } returns MutableStateFlow(
            CachedGraphMetadata(
                name = "name",
                title = "title",
                verticalLabel = "verticalLabel",
                identifiers = listOf("one", "two", "three")
            )
        )
        coEvery { reportingApi.graphs(any(), any()) } returns emptyList()

        getGraphIdentifiers("test").first()

        coVerify(inverse = true) { reportingApi.graphs(any(), any()) }
    }

    @Test
    fun `given there is no data in cache, when cache updated, then new data is received`() = runTest {
        val identifiers = listOf("one", "two", "three")
        val cacheState = MutableStateFlow<List<CachedGraphMetadata>?>(null)
        every { graphMetadataCache.getGraphMetadata(any()) } answers {
            cacheState.map {
                it?.firstOrNull { it.name == arg(0) }
            }
        }
        coEvery { graphMetadataCache.submitGraphMetadata(any()) } answers {
            cacheState.value = arg(0)
        }
        coEvery { reportingApi.graphs(any(), any()) } returns listOf(
            ReportingGraph(
                "name",
                "title",
                "verticalLabel",
                identifiers
            )
        )

        val result = getGraphIdentifiers("name").first()

        assertEquals(
            identifiers,
            result.getOrNull()
        )
    }
}
