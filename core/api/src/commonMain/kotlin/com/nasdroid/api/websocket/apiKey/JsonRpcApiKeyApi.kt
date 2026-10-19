package com.nasdroid.api.websocket.apiKey

import com.nasdroid.api.websocket.auth.AuthenticatedUser
import com.nasdroid.api.websocket.jsonrpc.JsonRpcSerializer
import com.nasdroid.api.websocket.jsonrpc.JsonRpcWebsocketClient
import com.nasdroid.api.websocket.jsonrpc.MethodCallError
import com.nasdroid.api.websocket.jsonrpc.callMethod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

/**
 * An implementation of [ApiKeyApi] that is backed by the TrueNAS JSON-RPC 2.0 WebSocket API.
 */
class JsonRpcApiKeyApi(
    private val client: JsonRpcWebsocketClient
) : ApiKeyApi {
    override suspend fun create(newKey: NewApiKey): ApiKey {
        val resolvedUsername = newKey.username ?: runCatching {
            client.callMethod<AuthenticatedUser>("auth.me").name
        }.getOrNull()

        if (!resolvedUsername.isNullOrBlank()) {
            try {
                return client.callMethod(
                    method = "api_key.create",
                    params = listOf(
                        CreateApiKeyV25Params(
                            name = newKey.name,
                            username = resolvedUsername,
                        )
                    )
                )
            } catch (error: MethodCallError) {
                val errorText = error.message.orEmpty() + error.data?.toString().orEmpty()
                if (!errorText.contains("username") && !errorText.contains("allowlist")) {
                    throw error
                }
            }
        }

        val fallbackKey = NewApiKey(
            name = newKey.name,
            allowList = newKey.allowList ?: listOf(AllowedMethod(AllowedMethod.Method.All, "*")),
            username = null,
        )
        return client.callMethod("api_key.create", listOf(fallbackKey))
    }

    override suspend fun delete(id: Int): Boolean {
        return client.callMethod("api_key.delete", listOf(id))
    }

    override suspend fun getInstance(id: Int): ApiKey {
        return client.callMethod("api_key.get_instance", listOf(id))
    }

    override suspend fun update(
        id: Int,
        updatedKey: UpdateApiKey
    ): ApiKey {
        val params = listOf<JsonElement>(
            JsonPrimitive(id),
            JsonRpcSerializer.encodeToJsonElement(updatedKey)
        )
        return client.callMethod(
            method = "api_key.update",
            params = params
        )
    }
}

@Serializable
internal data class CreateApiKeyV25Params(
    @SerialName("name")
    val name: String,
    @SerialName("username")
    val username: String,
)
