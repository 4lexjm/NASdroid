package com.nasdroid.api.websocket

import com.nasdroid.api.websocket.alert.AlertApi
import com.nasdroid.api.websocket.alert.JsonRpcAlertApi
import com.nasdroid.api.websocket.apiKey.ApiKeyApi
import com.nasdroid.api.websocket.apiKey.JsonRpcApiKeyApi
import com.nasdroid.api.websocket.app.AppApi
import com.nasdroid.api.websocket.app.JsonRpcAppApi
import com.nasdroid.api.websocket.auth.AuthApi
import com.nasdroid.api.websocket.auth.JsonRpcAuthApi
import com.nasdroid.api.websocket.catalog.CatalogApi
import com.nasdroid.api.websocket.catalog.JsonRpcCatalogApi
import com.nasdroid.api.websocket.core.CoreApi
import com.nasdroid.api.websocket.core.JsonRpcCoreApi
import com.nasdroid.api.websocket.ddp.DdpWebsocketClient
import com.nasdroid.api.websocket.jsonrpc.JsonRpcWebsocketClient
import com.nasdroid.api.websocket.pool.JsonRpcPoolApi
import com.nasdroid.api.websocket.pool.PoolApi
import com.nasdroid.api.websocket.reporting.JsonRpcReportingApi
import com.nasdroid.api.websocket.reporting.ReportingApi
import com.nasdroid.api.websocket.system.JsonRpcSystemApi
import com.nasdroid.api.websocket.system.SystemApi
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * A Koin module to inject the TrueNAS JSON-RPC 2.0 WebSocket API dependency graph.
 */
val ApiWebsocketModule = module {
    singleOf(::DdpWebsocketClient)
    singleOf(::JsonRpcWebsocketClient)

    singleOf(::JsonRpcAlertApi) bind AlertApi::class
    singleOf(::JsonRpcAuthApi) bind AuthApi::class
    singleOf(::JsonRpcApiKeyApi) bind ApiKeyApi::class
    singleOf(::JsonRpcAppApi) bind AppApi::class
    singleOf(::JsonRpcCatalogApi) bind CatalogApi::class
    singleOf(::JsonRpcCoreApi) bind CoreApi::class
    singleOf(::JsonRpcPoolApi) bind PoolApi::class
    singleOf(::JsonRpcReportingApi) bind ReportingApi::class
    singleOf(::JsonRpcSystemApi) bind SystemApi::class
}
