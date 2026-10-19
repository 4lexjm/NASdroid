package com.nasdroid.reporting.logic.graph

import com.nasdroid.api.websocket.reporting.GraphUnit
import com.nasdroid.api.websocket.reporting.ReportingApi
import com.nasdroid.api.websocket.reporting.RequestedGraph
import com.nasdroid.core.strongresult.StrongResult
import com.nasdroid.reporting.logic.graph.DurationGraph.Companion.toDurationGraph
import com.nasdroid.reporting.logic.graph.FloatGraph.Companion.toFloatGraph
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Retrieves the data needed to display all system-related graphs. See [invoke] for details.
 */
class GetSystemGraphs(
    private val reportingApi: ReportingApi,
    private val calculationDispatcher: CoroutineDispatcher,
) {

    /**
     * Retrieves a list of [Graph] that describes all system-related graphs, or a
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
                    RequestedGraph(RequestedGraph.GraphName.ActiveProcesses, null),
                    RequestedGraph(RequestedGraph.GraphName.Uptime, null),
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
                    when (graph.name) {
                        "processes" -> graph.toFloatGraph("Processes")
                        "uptime" -> graph.toDurationGraph()
                        else -> graph.toFloatGraph(graph.name)
                    }
                }
            )
        } catch (_: IllegalArgumentException) {
            return@withContext StrongResult.failure(ReportingGraphError.InvalidGraphData)
        }
    }
}
