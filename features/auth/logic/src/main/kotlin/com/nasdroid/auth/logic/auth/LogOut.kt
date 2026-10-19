package com.nasdroid.auth.logic.auth

import com.nasdroid.api.websocket.jsonrpc.JsonRpcWebsocketClient
import com.nasdroid.auth.data.currentserver.CurrentServerSource

/**
 * Signs out the current user. See [invoke] for details.
 */
class LogOut(
    private val client: JsonRpcWebsocketClient,
    private val currentServerSource: CurrentServerSource,
) {

    /**
     * Signs out the current user. If no user is signed in, nothing happens. This is guaranteed to
     * result in a "signed out" state.
     */
    suspend operator fun invoke() {
        client.disconnect()
        currentServerSource.setCurrentServer(null)
    }
}
