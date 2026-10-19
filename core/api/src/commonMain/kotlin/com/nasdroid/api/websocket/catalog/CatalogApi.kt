package com.nasdroid.api.websocket.catalog

import com.nasdroid.api.websocket.app.AvailableApp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Describes the TrueNAS WebSocket API for querying the application catalog ("catalog" namespace).
 *
 * In TrueNAS 24.10+, third-party catalog CRUD was removed in favor of the official TrueNAS catalog
 * and its trains (`catalog.trains`, `catalog.config`, `catalog.get_app_details`).
 */
interface CatalogApi {

    /**
     * Retrieve the list of available catalog trains (e.g. `["stable", "community", "enterprise"]`).
     */
    suspend fun trains(): List<String>

    /**
     * Retrieve the current catalog configuration.
     */
    suspend fun config(): CatalogConfig

    /**
     * Retrieve detailed information for [appName] in the specified catalog [train].
     */
    suspend fun getAppDetails(appName: String, train: String = "stable"): AvailableApp
}

/**
 * Configuration of the TrueNAS application catalog.
 *
 * @property label The label of the catalog (typically `"TRUENAS"`).
 * @property preferredTrains The list of preferred catalog trains configured on the system.
 * @property trains All available catalog trains.
 */
@Serializable
data class CatalogConfig(
    @SerialName("label")
    val label: String = "TRUENAS",
    @SerialName("preferred_trains")
    val preferredTrains: List<String> = emptyList(),
    @SerialName("trains")
    val trains: List<String> = emptyList(),
)
