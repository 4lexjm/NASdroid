package com.nasdroid.api.websocket.pool

import com.nasdroid.api.websocket.jsonrpc.JsonRpcSerializer
import com.nasdroid.api.websocket.jsonrpc.JsonRpcWebsocketClient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * An implementation of [PoolApi] backed by a JSON-RPC 2.0 WebSocket connection.
 */
class JsonRpcPoolApi(
    private val client: JsonRpcWebsocketClient
) : PoolApi {

    override suspend fun getPools(): List<Pool> {
        return client.callMethod(
            method = "pool.query",
            serializer = ListSerializer(Pool.serializer())
        )
    }

    override suspend fun getPool(id: Int): Pool {
        return client.callMethod(
            method = "pool.get_instance",
            serializer = Pool.serializer(),
            params = listOf(id),
            paramSerializer = Int.serializer(),
        )
    }

    override suspend fun findPoolsToImport(): Int {
        return client.callMethod(
            method = "pool.import_find",
            serializer = Int.serializer()
        )
    }

    override suspend fun importPool(params: ImportPoolParams): Int {
        return client.callMethod(
            method = "pool.import_pool",
            serializer = Int.serializer(),
            params = listOf(params),
            paramSerializer = ImportPoolParams.serializer(),
        )
    }

    override suspend fun scrubPool(id: Int, action: ScrubAction): Int {
        val actionJson = JsonRpcSerializer.encodeToJsonElement(ScrubAction.serializer(), action)
        return client.callMethod(
            method = "pool.scrub",
            serializer = Int.serializer(),
            params = listOf(JsonPrimitive(id), actionJson),
            paramSerializer = JsonElement.serializer(),
        )
    }
}
