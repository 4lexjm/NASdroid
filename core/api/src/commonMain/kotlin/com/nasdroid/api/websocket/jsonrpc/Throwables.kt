package com.nasdroid.api.websocket.jsonrpc

import kotlinx.serialization.json.JsonElement

/**
 * A JSON-RPC websocket server responded with an error after attempting to perform a method call.
 *
 * @property errorCode The code describing the error type.
 * @property message A short description of the error.
 * @property data Optional extra details provided by the server (e.g., TrueNAS error name or trace).
 */
data class MethodCallError(
    val errorCode: Int,
    override val message: String? = null,
    val data: JsonElement? = null,
) : Exception(message)

/**
 * Something went wrong while trying to construct the RPC call to the server.
 *
 * @property cause The underlying serialization failure.
 */
data class SerializeError(override val cause: Throwable) : Exception(cause)

/**
 * Something went wrong while trying to deserialize the response received from the server.
 *
 * @property cause The underlying deserialization failure.
 * @property resultMessage The raw JSON payload received from the server.
 */
data class DeserializeError(override val cause: Throwable, val resultMessage: String) : Exception(cause)
