package com.nasdroid.api.websocket.jsonrpc

import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.time.Duration.Companion.minutes

/**
 * Shared [Json] instance configured for TrueNAS JSON-RPC 2.0 payloads.
 */
@OptIn(ExperimentalSerializationApi::class)
val JsonRpcSerializer: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * A websocket client following [JSON-RPC 2.0](https://www.jsonrpc.org/specification).
 *
 * Before executing any calls, you'll need to [connect] successfully.
 */
@OptIn(ExperimentalAtomicApi::class)
class JsonRpcWebsocketClient {
    private val idCounter = AtomicInt(1)

    private val bootstrapLock = Mutex()
    private val pendingRequestsLock = Mutex()
    private val pendingRequests = mutableMapOf<Int, CompletableDeferred<JsonObject>>()
    private val notifications = MutableSharedFlow<Notification>(extraBufferCapacity = 64)
    private var readerJob: Job? = null

    private val _state = MutableStateFlow<State>(State.Disconnected)

    /**
     * The current state of the client. See [State] for all states.
     */
    val state: StateFlow<State> = _state

    /**
     * Attempts to open a connection to a JSON-RPC server at [url].
     *
     * When the connection succeeds, [state] is updated to [State.Connected], and this function
     * returns. However, if the connection fails, an [IllegalStateException] is thrown and the state
     * is reset.
     *
     * @throws IllegalStateException when the server rejects the connection.
     * @throws java.net.UnknownHostException when the server at [url] was not reachable.
     */
    suspend fun connect(url: String) {
        bootstrapLock.withLock {
            when (val current = state.value) {
                is State.Connected -> {
                    readerJob?.cancel()
                    readerJob = null
                    current.webSocketSession.close()
                    _state.value = State.Disconnected
                }
                is State.Connecting -> {
                    error("Cannot connect when already connecting.")
                }
                State.Disconnected -> { /* Proceed */ }
            }
            _state.value = State.Connecting
            try {
                val webSocket = openWebSocketWithFallback(url)
                readerJob = webSocket.launch {
                    listenForMessages(webSocket)
                }
                _state.value = State.Connected(webSocket)
            } finally {
                // Ensure state is never connecting when we finish
                if (_state.value is State.Connecting) {
                    _state.value = State.Disconnected
                }
            }
        }
    }

    private suspend fun openWebSocketWithFallback(url: String): DefaultClientWebSocketSession {
        return try {
            WebsocketKtorClient.webSocketSession(urlString = url)
        } catch (primaryException: Exception) {
            if (primaryException is CancellationException) {
                throw primaryException
            }
            val fallbackUrl = when {
                url.endsWith("/websocket") -> url.removeSuffix("/websocket") + "/api/current"
                url.endsWith("/api/current") -> url.removeSuffix("/api/current") + "/websocket"
                else -> null
            }
            if (fallbackUrl != null) {
                try {
                    WebsocketKtorClient.webSocketSession(urlString = fallbackUrl)
                } catch (_: Exception) {
                    throw primaryException
                }
            } else {
                throw primaryException
            }
        }
    }

    private suspend fun listenForMessages(webSocket: DefaultClientWebSocketSession) {
        try {
            for (frame in webSocket.incoming) {
                if (frame is Frame.Text) {
                    handleIncomingTextFrame(frame.readText())
                }
            }
        } catch (_: Exception) {
            // Connection closed or failed
        } finally {
            failAllPendingRequests(IllegalStateException("WebSocket session closed."))
        }
    }

    private suspend fun handleIncomingTextFrame(rawText: String) {
        val jsonObject = try {
            JsonRpcSerializer.parseToJsonElement(rawText).jsonObject
        } catch (_: Exception) {
            return
        }
        val idElement = jsonObject["id"]
        if (idElement != null) {
            val requestId = idElement.jsonPrimitive.intOrNull
            if (requestId != null) {
                val deferred = pendingRequestsLock.withLock {
                    pendingRequests.remove(requestId)
                }
                deferred?.complete(jsonObject)
            }
        } else if (jsonObject.containsKey("method")) {
            val notification = try {
                JsonRpcSerializer.decodeFromJsonElement(Notification.serializer(), jsonObject)
            } catch (_: SerializationException) {
                null
            }
            if (notification != null) {
                notifications.tryEmit(notification)
            }
        }
    }

    private suspend fun failAllPendingRequests(cause: Throwable) {
        val toFail = pendingRequestsLock.withLock {
            val values = pendingRequests.values.toList()
            pendingRequests.clear()
            values
        }
        for (deferred in toFail) {
            deferred.completeExceptionally(cause)
        }
    }

    /**
     * Disconnects from the connected session, or does nothing if not already connected.
     *
     * In case the client is connecting when this is called, this will suspend until connection
     * completes.
     */
    suspend fun disconnect() {
        bootstrapLock.withLock {
            when (val currentState = state.value) {
                is State.Connected -> {
                    readerJob?.cancel()
                    readerJob = null
                    currentState.webSocketSession.close()
                    failAllPendingRequests(IllegalStateException("Client disconnected."))
                    _state.value = State.Disconnected
                }
                is State.Connecting -> {
                    error("Attempted to disconnect while connecting. This should be impossible!")
                }
                State.Disconnected -> { /* Already disconnected, no-op */ }
            }
        }
    }

    /**
     * Performs an RPC call on the connected session.
     *
     * @param T The type of data to be returned from the method call.
     * @param P The type of parameters to be sent alongside the method call.
     *
     * @param method The method to call. Check your server documentation for available methods.
     * @param serializer The [KSerializer] used to deserialize the result.
     * @param params A list of optional parameters to send along with the method call.
     * @param paramSerializer The serializer for parameter items.
     *
     * @throws IllegalStateException if the client wasn't connected with [connect] before making the
     * call.
     * @throws MethodCallError if the server returned an error in response to the method call.
     * @throws SerializeError if the client failed to construct the method call. In this
     * situation, nothing was sent to the server.
     * @throws DeserializeError if the client failed to deserialize the method result.
     */
    suspend fun <T, P> callMethod(
        method: String,
        serializer: KSerializer<T>,
        params: List<P>,
        paramSerializer: KSerializer<P>
    ): T {
        val currentState = state.value
        check(currentState is State.Connected) { "Client must be connected before making any method calls!" }

        val requestId = idCounter.fetchAndIncrement()
        val message = RequestMessage(requestId, method, params)
        val serializedString = try {
            JsonRpcSerializer.encodeToString(RequestMessage.serializer(paramSerializer), message)
        } catch (e: SerializationException) {
            throw SerializeError(e)
        }

        val responseDeferred = CompletableDeferred<JsonObject>()
        pendingRequestsLock.withLock {
            pendingRequests[requestId] = responseDeferred
        }

        val responseObject = try {
            currentState.webSocketSession.send(Frame.Text(serializedString))
            responseDeferred.await()
        } catch (e: Throwable) {
            pendingRequestsLock.withLock {
                pendingRequests.remove(requestId)
            }
            throw e
        }

        val result = try {
            JsonRpcSerializer.decodeFromJsonElement(RpcResponseSerializer(serializer), responseObject)
        } catch (e: SerializationException) {
            throw DeserializeError(e, responseObject.toString())
        }
        when (result) {
            is ResponseError -> throw MethodCallError(
                errorCode = result.error.code,
                message = result.error.message,
                data = result.error.data,
            )
            is ResponseMessage<*> -> {
                @Suppress("UNCHECKED_CAST")
                return result.result as T
            }
        }
    }

    /**
     * Performs an RPC call on the connected session.
     *
     * @param T The type of data to be returned from the method call.
     *
     * @param method The method to call. Check your server documentation for available methods.
     * @param serializer The [KSerializer] used to deserialize the result.
     *
     * @throws IllegalStateException if the client wasn't connected with [connect] before making the
     * call.
     * @throws MethodCallError if the server returned an error in response to the method call.
     */
    suspend fun <T> callMethod(
        method: String,
        serializer: KSerializer<T>,
    ): T {
        return callMethod(
            method = method,
            serializer = serializer,
            params = emptyList(),
            paramSerializer = String.serializer()
        )
    }

    /**
     * Subscribes to a collection on the TrueNAS JSON-RPC server via `core.subscribe`.
     *
     * @param T The type of event payload expected in `fields`.
     * @param collection The collection name to subscribe to (e.g., `reporting.realtime`).
     * @param serializer The [KSerializer] used to decode the `fields` object of each event.
     */
    fun <T> subscribe(
        collection: String,
        serializer: KSerializer<T>,
    ): Flow<T> {
        var subscriptionId: String? = null
        return notifications
            .filter { it.method == "collection_update" }
            .mapNotNull { notification ->
                val update = try {
                    JsonRpcSerializer.decodeFromJsonElement(
                        CollectionUpdateParams.serializer(),
                        notification.params
                    )
                } catch (_: SerializationException) {
                    null
                }
                if (update != null && update.collection == collection && update.fields != null) {
                    try {
                        JsonRpcSerializer.decodeFromJsonElement(serializer, update.fields)
                    } catch (_: SerializationException) {
                        null
                    }
                } else {
                    null
                }
            }
            .onStart {
                subscriptionId = callMethod(
                    method = "core.subscribe",
                    serializer = String.serializer(),
                    params = listOf(collection),
                    paramSerializer = String.serializer()
                )
            }
            .onCompletion {
                val idToUnsubscribe = subscriptionId
                if (idToUnsubscribe != null && state.value is State.Connected) {
                    try {
                        callMethod(
                            method = "core.unsubscribe",
                            serializer = JsonElement.serializer(),
                            params = listOf(idToUnsubscribe),
                            paramSerializer = String.serializer()
                        )
                    } catch (_: Exception) {
                        // Ignore unsubscribe errors on completion
                    }
                }
            }
    }

    /**
     * Encapsulates possible states that [JsonRpcWebsocketClient] might be in.
     */
    sealed interface State {

        /**
         * The client is trying to connect to a server.
         */
        data object Connecting : State

        /**
         * The client successfully connected to the server.
         *
         * @property webSocketSession The internal websocket session powering the client.
         */
        data class Connected(
            internal val webSocketSession: DefaultClientWebSocketSession,
        ) : State

        /**
         * The client is not connected to any session. This is the default state. Call [connect] to
         * get set up.
         */
        data object Disconnected : State
    }
}

private val WebsocketKtorClient = HttpClient {
    install(Logging) {
        level = LogLevel.ALL
        logger = object : Logger {
            override fun log(message: String) {
                println(message)
            }
        }
    }

    install(WebSockets) {
        pingInterval = 1.minutes
    }
}

/**
 * Performs an RPC call on the connected session.
 *
 * @param T The type of data to be returned from the method call.
 *
 * @param method The method to call. Check your server documentation for available methods.
 *
 * @throws IllegalStateException if the client wasn't connected with
 * [JsonRpcWebsocketClient.connect] before making the call.
 * @throws MethodCallError if the server returned an error in response to the method call.
 */
suspend inline fun <reified T> JsonRpcWebsocketClient.callMethod(method: String): T {
    return callMethod(method, serializer())
}

/**
 * Performs an RPC call on the connected session.
 *
 * @param T The type of data to be returned from the method call.
 * @param P The type of parameters to be sent alongside the method call.
 *
 * @param method The method to call. Check your server documentation for available methods.
 * @param params A list of optional parameters to send along with the method call.
 *
 * @throws IllegalStateException if the client wasn't connected with
 * [JsonRpcWebsocketClient.connect] before making the call.
 * @throws MethodCallError if the server returned an error in response to the method call.
 */
suspend inline fun <reified T, reified P> JsonRpcWebsocketClient.callMethod(
    method: String,
    params: List<P>
): T {
    return callMethod(
        method = method,
        serializer = serializer(),
        params = params,
        paramSerializer = serializer()
    )
}

/**
 * Subscribes to a collection on the TrueNAS JSON-RPC server via `core.subscribe`.
 *
 * @param T The type of data to be received from the subscription.
 * @param collection The collection name to subscribe to.
 */
inline fun <reified T> JsonRpcWebsocketClient.subscribe(
    collection: String,
): Flow<T> {
    return subscribe(collection, serializer())
}
