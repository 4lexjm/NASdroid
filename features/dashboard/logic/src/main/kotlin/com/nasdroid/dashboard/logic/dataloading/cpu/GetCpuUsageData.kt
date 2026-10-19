package com.nasdroid.dashboard.logic.dataloading.cpu

import com.nasdroid.api.v2.exception.HttpNotOkException
import com.nasdroid.api.websocket.jsonrpc.MethodCallError
import com.nasdroid.api.websocket.reporting.ReportingApi
import com.nasdroid.api.websocket.reporting.RequestedGraph
import kotlinx.datetime.Clock
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/**
 * Retrieves CPU-related data from the server. See [invoke] for details.
 */
class GetCpuUsageData(
    private val reportingApi: ReportingApi,
) {

    /**
     * Returns a [Result] that contains either [CpuUsageData], or an exception if the request
     * failed.
     */
    suspend operator fun invoke(): Result<CpuUsageData> {
        return try {
            val now = Clock.System.now()
            val graphs = reportingApi.getData(
                graphs = listOf(
                    RequestedGraph(RequestedGraph.GraphName.CpuUsage, null),
                    RequestedGraph(RequestedGraph.GraphName.CpuTemp, null)
                ),
                start = now - 20.seconds,
                end = now,
            )
            val usageGraph = graphs.first { it.name == CPU_GRAPH_NAME }
            val temperatureGraph = graphs.firstOrNull { it.name == CPU_TEMP_GRAPH_NAME }

            // Usage data comes to us as a 2d array. We get the last set of values that aren't null,
            // i.e. the most recent recorded values, then take the "idle" percentage from that.
            // By subtracting the idle percentage from 100, we get the system CPU usage.
            val idleIndex = usageGraph.legend.indexOf("idle")
            val latestUsageRow = usageGraph.data.last { !it.contains(null) }.requireNoNulls()
            @Suppress("MagicNumber")
            val avgUsage = if (idleIndex >= 0) {
                (100 - latestUsageRow[idleIndex]) / 100.0
            } else {
                latestUsageRow.drop(1).sum() / 100.0
            }
            // Temperature data comes to us as a 2d array. We get the last set of values that aren't
            // null, i.e. the most recent recorded values, then we take the highest number from that.
            val nonNullTempRows = temperatureGraph?.data?.filter { !it.contains(null) }.orEmpty()
            val temp = if (temperatureGraph != null && nonNullTempRows.isNotEmpty()) {
                nonNullTempRows.last().requireNoNulls()
                    .reduceRightIndexed { index, d, acc ->
                        // If the item we are looking at isn't directly associated with a core
                        // (or thread), we ignore it.
                        if (temperatureGraph.legend[index].toIntOrNull() != null) {
                            // We "accumulate" the highest value to get the highest temp
                            if (acc > d) {
                                acc
                            } else {
                                d
                            }
                        } else {
                            acc
                        }
                    }
                    .roundToInt()
            } else {
                0
            }
            Result.success(
                CpuUsageData(
                    utilisation = avgUsage.toFloat(),
                    temp = temp
                )
            )
        } catch (e: MethodCallError) {
            Result.failure(e)
        } catch (e: HttpNotOkException) {
            Result.failure(e)
        }
    }

    /**
     * Constants used for CPU usage graph querying.
     */
    companion object {
        private const val CPU_GRAPH_NAME = "cpu"
        private const val CPU_TEMP_GRAPH_NAME = "cputemp"
    }
}

/**
 * Describes the CPU in the system.
 *
 * @property utilisation The current CPU utilisation. The value will always be between 0 and 1.
 * @property temp The CPU temperature in celsius. This is usually measured by the hottest
 * core.
 */
data class CpuUsageData(
    val utilisation: Float,
    val temp: Int,
)
