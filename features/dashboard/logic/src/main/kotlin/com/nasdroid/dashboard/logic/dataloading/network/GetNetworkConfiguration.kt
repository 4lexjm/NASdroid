package com.nasdroid.dashboard.logic.dataloading.network

import com.nasdroid.api.v2.exception.HttpNotOkException
import com.nasdroid.api.websocket.jsonrpc.MethodCallError
import com.nasdroid.api.websocket.reporting.ReportingApi

/**
 * Get a simple representation of the system network configuration. See [invoke] for details.
 */
class GetNetworkConfiguration(
    private val reportingApi: ReportingApi,
) {

    /**
     * Returns a [Result] that contains either [NetworkConfiguration], or an exception if the
     * request failed.
     */
    suspend operator fun invoke(): Result<NetworkConfiguration> {
        return try {
            val adapterNames = reportingApi.graphs(
                limit = null,
                offset = null,
            ).first { it.name == INTERFACE_GRAPH_NAME }.identifiers
            val adapters = adapterNames.orEmpty().map {
                NetworkConfiguration.NetworkAdapter(
                    name = it,
                    address = null // TODO Get the address for an adapter
                )
            }
            Result.success(NetworkConfiguration(adapters = adapters))
        } catch (e: MethodCallError) {
            Result.failure(e)
        } catch (e: HttpNotOkException) {
            Result.failure(e)
        }
    }

    /**
     * Constants used for network configuration graph querying.
     */
    companion object {
        private const val INTERFACE_GRAPH_NAME = "interface"
    }
}

/**
 * Describes the network configuration of the system.
 *
 * @property adapters A list of [NetworkAdapter]s the system has.
 */
data class NetworkConfiguration(
    val adapters: List<NetworkAdapter>
) {

    /**
     * Describes a single network adapter in the system.
     *
     * @property name The unique adapter name.
     * @property address The adapter IP address, if any.
     */
    data class NetworkAdapter(
        val name: String,
        val address: String?
    )
}
