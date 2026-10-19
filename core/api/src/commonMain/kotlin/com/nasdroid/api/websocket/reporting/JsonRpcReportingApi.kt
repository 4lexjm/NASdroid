package com.nasdroid.api.websocket.reporting

import com.nasdroid.api.websocket.jsonrpc.JsonRpcSerializer
import com.nasdroid.api.websocket.jsonrpc.JsonRpcWebsocketClient
import com.nasdroid.api.websocket.jsonrpc.MethodCallError
import com.nasdroid.api.websocket.jsonrpc.callMethod
import com.nasdroid.api.websocket.jsonrpc.subscribe
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement

/**
 * An implementation of [ReportingApi] backed by the TrueNAS JSON-RPC 2.0 WebSocket API.
 */
class JsonRpcReportingApi(
    private val client: JsonRpcWebsocketClient
) : ReportingApi {
    override suspend fun config(): ReportingConfig {
        return client.callMethod("reporting.config")
    }

    override suspend fun getData(
        graphs: List<RequestedGraph>,
        aggregate: Boolean
    ): List<GraphData> {
        return executeGetData(
            graphs = graphs,
            query = ReportingQueryOptions(aggregate = aggregate)
        )
    }

    override suspend fun getData(
        graphs: List<RequestedGraph>,
        unit: GraphUnit,
        page: Int,
        aggregate: Boolean
    ): List<GraphData> {
        return executeGetData(
            graphs = graphs,
            query = ReportingQueryOptions(
                unit = unit,
                page = page,
                aggregate = aggregate
            )
        )
    }

    override suspend fun getData(
        graphs: List<RequestedGraph>,
        start: Instant,
        end: Instant,
        aggregate: Boolean
    ): List<GraphData> {
        return executeGetData(
            graphs = graphs,
            query = ReportingQueryOptions(
                start = start.epochSeconds,
                end = end.epochSeconds,
                aggregate = aggregate
            )
        )
    }

    private suspend fun executeGetData(
        graphs: List<RequestedGraph>,
        query: ReportingQueryOptions
    ): List<GraphData> {
        val params = listOf(
            JsonRpcSerializer.encodeToJsonElement(graphs),
            JsonRpcSerializer.encodeToJsonElement(query)
        )
        return try {
            client.callMethod("reporting.netdata_get_data", params)
        } catch (error: MethodCallError) {
            val filtered = graphs.filterNot { it.name in LEGACY_ONLY_GRAPHS }
            if (filtered.isNotEmpty() && filtered.size < graphs.size) {
                val retryParams = listOf(
                    JsonRpcSerializer.encodeToJsonElement(filtered),
                    JsonRpcSerializer.encodeToJsonElement(query)
                )
                runCatching {
                    client.callMethod<List<GraphData>, JsonElement>("reporting.netdata_get_data", retryParams)
                }.getOrElse {
                    client.callMethod("reporting.get_data", retryParams)
                }
            } else {
                try {
                    client.callMethod("reporting.get_data", params)
                } catch (_: MethodCallError) {
                    throw error
                }
            }
        }
    }

    override suspend fun graph(
        name: String,
        aggregate: Boolean
    ): List<GraphData> {
        return executeGraph(name, ReportingQueryOptions(aggregate = aggregate))
    }

    override suspend fun graph(
        name: String,
        unit: GraphUnit,
        page: Int,
        aggregate: Boolean
    ): List<GraphData> {
        return executeGraph(
            name = name,
            query = ReportingQueryOptions(unit = unit, page = page, aggregate = aggregate)
        )
    }

    override suspend fun graph(
        name: String,
        start: Instant,
        end: Instant,
        aggregate: Boolean
    ): List<GraphData> {
        return executeGraph(
            name = name,
            query = ReportingQueryOptions(
                start = start.epochSeconds,
                end = end.epochSeconds,
                aggregate = aggregate
            )
        )
    }

    private suspend fun executeGraph(
        name: String,
        query: ReportingQueryOptions
    ): List<GraphData> {
        val params = listOf(
            JsonPrimitive(name),
            JsonRpcSerializer.encodeToJsonElement(query)
        )
        return try {
            client.callMethod("reporting.netdata_graph", params)
        } catch (_: MethodCallError) {
            client.callMethod("reporting.graph", params)
        }
    }

    override suspend fun graphs(
        limit: Int?,
        offset: Int?
    ): List<Graph> {
        val params = if (limit == null && offset == null) {
            emptyList()
        } else {
            listOf<JsonElement>(
                JsonArray(emptyList()),
                buildJsonObject {
                    limit?.let { put("limit", JsonPrimitive(it)) }
                    offset?.let { put("offset", JsonPrimitive(it)) }
                }
            )
        }
        return try {
            if (params.isEmpty()) {
                client.callMethod("reporting.netdata_graphs")
            } else {
                client.callMethod("reporting.netdata_graphs", params)
            }
        } catch (_: MethodCallError) {
            if (params.isEmpty()) {
                client.callMethod("reporting.graphs")
            } else {
                client.callMethod("reporting.graphs", params)
            }
        }
    }

    override suspend fun netDataWebGeneratePassword(): String {
        return client.callMethod("reporting.netdataweb_generate_password")
    }

    override suspend fun update(tier1Days: Int): ReportingConfig {
        return client.callMethod("reporting.update", listOf(mapOf("tier1_days" to tier1Days)))
    }

    override fun processes(): Flow<RealtimeProcesses> {
        return client.subscribe("reporting.processes")
    }

    override fun realtime(interval: Int): Flow<RealtimeUtilisation> {
        return client.subscribe("reporting.realtime")
    }

    companion object {
        private val LEGACY_ONLY_GRAPHS = setOf(
            RequestedGraph.GraphName.ArcActualRate,
            RequestedGraph.GraphName.ArcRate,
            RequestedGraph.GraphName.ArcResult,
        )
    }
}

@Serializable
internal data class ReportingQueryOptions(
    @SerialName("unit")
    val unit: GraphUnit? = null,
    @SerialName("page")
    val page: Int? = null,
    @SerialName("start")
    val start: Long? = null,
    @SerialName("end")
    val end: Long? = null,
    @SerialName("aggregate")
    val aggregate: Boolean = true,
)
