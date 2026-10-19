package com.nasdroid.api.websocket.app

import com.nasdroid.api.websocket.jsonrpc.JsonRpcSerializer
import com.nasdroid.api.websocket.jsonrpc.JsonRpcWebsocketClient
import com.nasdroid.api.websocket.jsonrpc.MethodCallError
import com.nasdroid.api.websocket.jsonrpc.callMethod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * An implementation of [AppApi] backed by a JSON-RPC 2.0 WebSocket connection.
 */
class JsonRpcAppApi(
    private val client: JsonRpcWebsocketClient
) : AppApi {

    override suspend fun available(): List<AvailableApp> {
        return client.callMethod(
            method = "app.available",
            serializer = ListSerializer(AvailableApp.serializer())
        )
    }

    override suspend fun categories(): List<String> {
        return client.callMethod(
            method = "app.categories",
            serializer = ListSerializer(String.serializer())
        )
    }

    override suspend fun latest(): List<AvailableApp> {
        return client.callMethod(
            method = "app.latest",
            serializer = ListSerializer(AvailableApp.serializer())
        )
    }

    override suspend fun similar(appName: String, train: String): List<AvailableApp> {
        return client.callMethod(
            method = "app.similar",
            serializer = ListSerializer(AvailableApp.serializer()),
            params = listOf(appName, train),
            paramSerializer = String.serializer(),
        )
    }

    override suspend fun query(): List<InstalledApp> {
        return client.callMethod(
            method = "app.query",
            serializer = ListSerializer(InstalledApp.serializer())
        )
    }

    override suspend fun getInstance(name: String): InstalledApp {
        return client.callMethod(
            method = "app.get_instance",
            serializer = InstalledApp.serializer(),
            params = listOf(name),
            paramSerializer = String.serializer(),
        )
    }

    override suspend fun start(appName: String): Int {
        return client.callMethod(
            method = "app.start",
            serializer = Int.serializer(),
            params = listOf(appName),
            paramSerializer = String.serializer(),
        )
    }

    override suspend fun stop(appName: String): Int {
        return client.callMethod(
            method = "app.stop",
            serializer = Int.serializer(),
            params = listOf(appName),
            paramSerializer = String.serializer(),
        )
    }

    override suspend fun delete(
        appName: String,
        removeImages: Boolean,
        removeIxVolumes: Boolean,
        forceRemoveCustomApp: Boolean,
    ): Int {
        val optionsJson = JsonRpcSerializer.encodeToJsonElement(
            DeleteOptions.serializer(),
            DeleteOptions(
                removeImages = removeImages,
                removeIxVolumes = removeIxVolumes,
                forceRemoveCustomApp = forceRemoveCustomApp,
            )
        )
        return client.callMethod(
            method = "app.delete",
            serializer = Int.serializer(),
            params = listOf(JsonPrimitive(appName), optionsJson),
            paramSerializer = JsonElement.serializer(),
        )
    }

    override suspend fun rollbackVersions(appName: String): List<String> {
        return client.callMethod(
            method = "app.rollback_versions",
            serializer = ListSerializer(String.serializer()),
            params = listOf(appName),
            paramSerializer = String.serializer(),
        )
    }

    override suspend fun rollback(
        appName: String,
        appVersion: String,
        rollbackSnapshot: Boolean,
    ): Int {
        val optionsJson = JsonRpcSerializer.encodeToJsonElement(
            RollbackOptions.serializer(),
            RollbackOptions(
                appVersion = appVersion,
                rollbackSnapshot = rollbackSnapshot,
            )
        )
        return client.callMethod(
            method = "app.rollback",
            serializer = Int.serializer(),
            params = listOf(JsonPrimitive(appName), optionsJson),
            paramSerializer = JsonElement.serializer(),
        )
    }

    override suspend fun containerIds(appName: String): Map<String, ContainerDetails> {
        return try {
            client.callMethod(
                method = "app.container_ids",
                serializer = MapSerializer(String.serializer(), ContainerDetails.serializer()),
                params = listOf(appName),
                paramSerializer = String.serializer(),
            )
        } catch (_: MethodCallError) {
            getInstance(appName).activeWorkloads.containerDetails.associateBy { it.id }
        }
    }

    @Suppress("BooleanPropertyNaming")
    @Serializable
    internal data class DeleteOptions(
        @SerialName("remove_images")
        val removeImages: Boolean = true,
        @SerialName("remove_ix_volumes")
        val removeIxVolumes: Boolean = false,
        @SerialName("force_remove_custom_app")
        val forceRemoveCustomApp: Boolean = false,
    )

    @Suppress("BooleanPropertyNaming")
    @Serializable
    internal data class RollbackOptions(
        @SerialName("app_version")
        val appVersion: String,
        @SerialName("rollback_snapshot")
        val rollbackSnapshot: Boolean = false,
    )
}
