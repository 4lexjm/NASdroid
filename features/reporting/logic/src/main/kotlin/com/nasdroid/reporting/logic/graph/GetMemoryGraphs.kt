package com.nasdroid.reporting.logic.graph

import com.nasdroid.api.websocket.reporting.GraphUnit
import com.nasdroid.api.websocket.reporting.ReportingApi
import com.nasdroid.api.websocket.reporting.RequestedGraph
import com.nasdroid.core.strongresult.StrongResult
import com.nasdroid.reporting.logic.graph.CapacityGraph.Companion.toCapacityGraph
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Retrieves the data needed to display all memory-related graphs. See [invoke] for details.
 */
class GetMemoryGraphs(
    private val reportingApi: ReportingApi,
    private val calculationDispatcher: CoroutineDispatcher,
) {

    /**
     * Retrieves a list of [Graph] that describes all memory-related graphs, or a
     * [ReportingGraphError] if something went wrong. The retrieved data represents the last hour of
     * reporting data.
     *
     * @param timeframe The frame of time for which the graph data is returned for.
     */
    suspend operator fun invoke(
        timeframe: GraphTimeframe = GraphTimeframe.Hour
    ): StrongResult<List<Graph<*>>, ReportingGraphError> = withContext(calculationDispatcher) {
        try {
            val reportingData = reportingApi.getData(
                graphs = listOf(
                    RequestedGraph(RequestedGraph.GraphName.MemoryUsage, null),
                    RequestedGraph(RequestedGraph.GraphName.Swap, null),
                ),
                unit = when (timeframe) {
                    GraphTimeframe.Hour -> GraphUnit.Hour
                    GraphTimeframe.Day -> GraphUnit.Day
                    GraphTimeframe.Week -> GraphUnit.Week
                    GraphTimeframe.Month -> GraphUnit.Month
                    GraphTimeframe.Year -> GraphUnit.Year
                },
                page = 1
            )

            return@withContext StrongResult.success(
                reportingData.map { graph ->
                    graph.toCapacityGraph()
                }
            )
        } catch (_: IllegalArgumentException) {
            return@withContext StrongResult.failure(ReportingGraphError.InvalidGraphData)
        }
    }
}
