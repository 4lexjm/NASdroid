package com.nasdroid.reporting.logic.graph

import com.boswelja.capacity.Capacity.Companion.kibibytes
import com.nasdroid.api.websocket.reporting.GraphUnit
import com.nasdroid.api.websocket.reporting.ReportingApi
import com.nasdroid.api.websocket.reporting.RequestedGraph
import com.nasdroid.core.strongresult.StrongResult
import com.nasdroid.reporting.logic.graph.CapacityGraph.Companion.toCapacityGraph
import com.nasdroid.reporting.logic.graph.TemperatureGraph.Companion.toTemperatureGraph
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Retrieves the data needed to display all disk-related graphs. See [invoke] for details.
 */
class GetDiskGraphs(
    private val reportingApi: ReportingApi,
    private val calculationDispatcher: CoroutineDispatcher,
) {

    /**
     * Retrieves a list of [Graph] that describes all disk-related graphs, or a [ReportingGraphError]
     * if something went wrong. The retrieved data represents the last hour of reporting data.
     *
     * @param disks A list of disk identifiers whose utilisation graphs should be retrieved.
     * @param timeframe The frame of time for which the graph data is returned for.
     */
    suspend operator fun invoke(
        disks: List<String>,
        timeframe: GraphTimeframe = GraphTimeframe.Hour
    ): StrongResult<List<Graph<*>>, ReportingGraphError> = withContext(calculationDispatcher) {
        if (disks.isEmpty()) {
            return@withContext StrongResult.success(emptyList())
        }
        val reportingData = reportingApi.getData(
            graphs = disks.flatMap {
                listOf(
                    RequestedGraph(RequestedGraph.GraphName.DiskIo, it),
                    RequestedGraph(RequestedGraph.GraphName.DiskTemp, it)
                )
            },
            unit = when (timeframe) {
                GraphTimeframe.Hour -> GraphUnit.Hour
                GraphTimeframe.Day -> GraphUnit.Day
                GraphTimeframe.Week -> GraphUnit.Week
                GraphTimeframe.Month -> GraphUnit.Month
                GraphTimeframe.Year -> GraphUnit.Year
            },
            page = 1
        )

        try {
            val utilisationGraphs = emptyList<CapacityGraph>().toMutableList()
            val temperatureGraphs = emptyList<TemperatureGraph>().toMutableList()
            reportingData.forEach { graph ->
                if (graph.name == "disk") {
                    utilisationGraphs += graph.toCapacityGraph { it.kibibytes }
                } else {
                    temperatureGraphs += graph.toTemperatureGraph()
                }
            }

            return@withContext StrongResult.success(
                utilisationGraphs + temperatureGraphs
            )
        } catch (_: IllegalArgumentException) {
            return@withContext StrongResult.failure(ReportingGraphError.InvalidGraphData)
        }
    }
}
