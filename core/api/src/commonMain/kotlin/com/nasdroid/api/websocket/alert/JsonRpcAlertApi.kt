package com.nasdroid.api.websocket.alert

import com.nasdroid.api.websocket.jsonrpc.JsonRpcWebsocketClient
import com.nasdroid.api.websocket.jsonrpc.callMethod

/**
 * An implementation of [AlertApi] backed by the TrueNAS JSON-RPC 2.0 WebSocket API.
 */
class JsonRpcAlertApi(
    private val client: JsonRpcWebsocketClient
) : AlertApi {
    override suspend fun dismiss(uuid: String) {
        return client.callMethod("alert.dismiss", listOf(uuid))
    }

    override suspend fun list(): List<Alert> {
        return client.callMethod("alert.list")
    }

    override suspend fun listCategories(): List<AlertCategory> {
        return client.callMethod("alert.list_categories")
    }

    override suspend fun listPolicies(): List<AlertPolicy> {
        return client.callMethod("alert.list_policies")
    }

    override suspend fun restore(uuid: String) {
        return client.callMethod("alert.restore", listOf(uuid))
    }
}
