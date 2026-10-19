package com.nasdroid.api.websocket.catalog

import com.nasdroid.api.websocket.app.AvailableApp
import com.nasdroid.api.websocket.jsonrpc.JsonRpcSerializer
import com.nasdroid.api.websocket.jsonrpc.JsonRpcWebsocketClient
import com.nasdroid.api.websocket.jsonrpc.MethodCallError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * An implementation of [CatalogApi] backed by a JSON-RPC 2.0 WebSocket connection.
 */
class JsonRpcCatalogApi(
    private val client: JsonRpcWebsocketClient
) : CatalogApi {

    override suspend fun trains(): List<String> {
        return try {
            client.callMethod(
                method = "catalog.trains",
                serializer = ListSerializer(String.serializer())
            )
        } catch (_: MethodCallError) {
            config().trains
        }
    }

    override suspend fun config(): CatalogConfig {
        return client.callMethod(
            method = "catalog.config",
            serializer = CatalogConfig.serializer()
        )
    }

    override suspend fun getAppDetails(appName: String, train: String): AvailableApp {
        return try {
            val detailsJson = JsonRpcSerializer.encodeToJsonElement(
                AppVersionDetails.serializer(),
                AppVersionDetails(train = train)
            )
            client.callMethod(
                method = "catalog.get_app_details",
                serializer = AvailableApp.serializer(),
                params = listOf(JsonPrimitive(appName), detailsJson),
                paramSerializer = JsonElement.serializer(),
            )
        } catch (error: MethodCallError) {
            // Fallback: look up the app from app.available (e.g. when train was not specified or differs)
            val availableApps = client.callMethod(
                method = "app.available",
                serializer = ListSerializer(AvailableApp.serializer())
            )
            availableApps.firstOrNull { app ->
                app.name == appName && (app.train.equals(train, ignoreCase = true) || app.catalog.equals(train, ignoreCase = true))
            } ?: availableApps.firstOrNull { app -> app.name == appName } ?: throw error
        }
    }

    @Serializable
    internal data class AppVersionDetails(
        @SerialName("train")
        val train: String,
    )
}
