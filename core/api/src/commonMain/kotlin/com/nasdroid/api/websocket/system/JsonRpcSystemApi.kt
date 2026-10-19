package com.nasdroid.api.websocket.system

import com.nasdroid.api.websocket.ddp.EDateInstantSerializer
import com.nasdroid.api.websocket.jsonrpc.JsonRpcWebsocketClient
import com.nasdroid.api.websocket.jsonrpc.MethodCallError
import com.nasdroid.api.websocket.jsonrpc.callMethod
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.time.Duration

/**
 * An implementation of [SystemApi] backed by the TrueNAS JSON-RPC 2.0 WebSocket API.
 */
class JsonRpcSystemApi(
    private val client: JsonRpcWebsocketClient,
) : SystemApi {
    override suspend fun bootId(): String {
        return client.callMethod("system.boot_id")
    }

    override suspend fun buildTime(): Instant {
        return client.callMethod("system.build_time", EDateInstantSerializer())
    }

    override suspend fun debug(): Int {
        return client.callMethod("system.debug")
    }

    override suspend fun featureEnabled(feature: Feature): Boolean {
        return client.callMethod("system.feature_enabled", listOf(feature))
    }

    override suspend fun hostId(): String {
        return client.callMethod("system.host_id")
    }

    override suspend fun info(): SystemInfo {
        return client.callMethod("system.info")
    }

    override suspend fun isStable(): String {
        return client.callMethod("system.is_stable")
    }

    override suspend fun licenseUpdate(license: String) {
        return client.callMethod("system.license_update", listOf(license))
    }

    override suspend fun productType(): ProductType {
        return client.callMethod("system.product_type")
    }

    override suspend fun ready(): Boolean {
        return client.callMethod("system.ready")
    }

    override suspend fun reboot(delay: Duration?, reason: String): Int {
        val options = buildJsonObject {
            put("delay", delay?.let { JsonPrimitive(it.inWholeSeconds) } ?: JsonNull)
        }
        return try {
            client.callMethod<Int, JsonElement>(
                method = "system.reboot",
                params = listOf(JsonPrimitive(reason), options)
            )
        } catch (_: MethodCallError) {
            client.callMethod<Int, JsonElement>(
                method = "system.reboot",
                params = listOf(options)
            )
        }
    }

    override suspend fun releaseNotesUrl(version: String?): String? {
        return if (version == null) {
            client.callMethod("system.release_notes_url")
        } else {
            client.callMethod("system.release_notes_url", listOf(version))
        }
    }

    override suspend fun shutdown(delay: Duration?, reason: String): Int {
        val options = buildJsonObject {
            put("delay", delay?.let { JsonPrimitive(it.inWholeSeconds) } ?: JsonNull)
        }
        return try {
            client.callMethod<Int, JsonElement>(
                method = "system.shutdown",
                params = listOf(JsonPrimitive(reason), options)
            )
        } catch (_: MethodCallError) {
            client.callMethod<Int, JsonElement>(
                method = "system.shutdown",
                params = listOf(options)
            )
        }
    }

    override suspend fun state(): State {
        return client.callMethod("system.state")
    }

    override suspend fun version(): String {
        return client.callMethod("system.version")
    }

    override suspend fun versionShort(): String {
        return client.callMethod("system.version_short")
    }
}
