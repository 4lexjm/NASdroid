package com.nasdroid.apps.logic.installed

import com.nasdroid.api.websocket.app.AppApi

/**
 * Retrieve all pods and containers that logs can be exported from. See [invoke] for details.
 */
class GetLogOptions(
    private val appApi: AppApi
) {

    /**
     * Gets all available log options for the given release name.
     */
    suspend operator fun invoke(releaseName: String): LogOptions {
        val containers = appApi.containerIds(releaseName)
        val containerNames = containers.values
            .map { it.serviceName.ifEmpty { it.id } }
            .ifEmpty { listOf(releaseName) }
        return LogOptions(
            podsAndCharts = mapOf(releaseName to containerNames)
        )
    }
}

/**
 * Options available for log retrieval.
 *
 * @property podsAndCharts A Map of pod names to a list of containers inside the pod.
 */
data class LogOptions(
    val podsAndCharts: Map<String, List<String>>
)
