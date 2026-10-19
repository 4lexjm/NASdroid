package com.nasdroid.api.websocket.core

import com.nasdroid.api.websocket.jsonrpc.JsonRpcWebsocketClient
import com.nasdroid.api.websocket.jsonrpc.callMethod
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

/**
 * An implementation of [CoreApi] backed by the TrueNAS JSON-RPC 2.0 WebSocket API.
 */
@Suppress("TooManyFunctions")
class JsonRpcCoreApi(
    private val client: JsonRpcWebsocketClient
) : CoreApi {
    override suspend fun arp(
        ip: String?,
        networkInterface: String?
    ): Map<String, String> {
        return client.callMethod("core.arp", listOf(ArpArgs(ip, networkInterface)))
    }

    override suspend fun debug(
        bindAddress: String,
        bindPort: Int,
        threaded: Boolean
    ) {
        return client.callMethod("core.debug", listOf(DebugArgs(bindAddress, bindPort, threaded)))
    }

    override suspend fun debugModeEnabled(): Boolean {
        return client.callMethod("core.debug_mode_enabled")
    }

    override suspend fun getJobs(): List<Job> {
        return client.callMethod("core.get_jobs")
    }

    override suspend fun getJob(jobId: Int): Job {
        val filterParams = listOf<JsonElement>(
            buildJsonArray {
                add(
                    JsonArray(
                        listOf(
                            JsonPrimitive("id"),
                            JsonPrimitive("="),
                            JsonPrimitive(jobId)
                        )
                    )
                )
            }
        )
        val jobs: List<Job> = client.callMethod("core.get_jobs", filterParams)
        return jobs.first { it.id == jobId }
    }

    override suspend fun jobAbort(jobId: Int) {
        return client.callMethod("core.job_abort", listOf(jobId))
    }

    override suspend fun jobWait(jobId: Int): Int {
        return client.callMethod("core.job_wait", listOf(jobId))
    }

    override suspend fun ping(): String {
        return client.callMethod("core.ping")
    }

    override suspend fun pingRemote(
        hostName: String,
        type: PingType,
        timeout: Int,
        count: Int?,
        networkInterface: String?,
        interval: Int?
    ): Boolean {
        return client.callMethod(
            method = "core.ping_remote",
            params = listOf(PingRemoteArgs(type, hostName, timeout, count, networkInterface, interval))
        )
    }

    override suspend fun resizeShell(id: String?, cols: Int?, rows: Int?) {
        val params = listOf<JsonElement>(
            id?.let { JsonPrimitive(it) } ?: JsonNull,
            cols?.let { JsonPrimitive(it) } ?: JsonNull,
            rows?.let { JsonPrimitive(it) } ?: JsonNull,
        )
        return client.callMethod(
            method = "core.resize_shell",
            params = params
        )
    }

    override suspend fun sessions(): List<Session> {
        return client.callMethod("core.sessions")
    }

    override suspend fun setDebugMode(debugMode: Boolean) {
        return client.callMethod("core.set_debug_mode", listOf(debugMode))
    }
}
