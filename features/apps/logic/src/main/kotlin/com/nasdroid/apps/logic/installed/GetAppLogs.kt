package com.nasdroid.apps.logic.installed

import com.nasdroid.api.websocket.app.AppApi

/**
 * Gets logs for an app instance. See [invoke] for details.
 */
class GetAppLogs(
    private val appApi: AppApi
) {

    /**
     * Gets the log output of a specified container for an app.
     *
     * @param releaseName The name of the app whose logs we are interested in.
     * @param logOptions The [SelectedLogOptions] to configure log retrieval.
     *
     * @return A List of String representing log lines.
     */
    suspend operator fun invoke(releaseName: String, logOptions: SelectedLogOptions): List<String> {
        val containers = appApi.containerIds(releaseName)
        val matchingContainer = containers.values.firstOrNull {
            it.serviceName == logOptions.containerName || it.id == logOptions.containerName
        } ?: containers.values.firstOrNull()
        return if (matchingContainer != null) {
            listOf(
                "App: $releaseName",
                "Service: ${matchingContainer.serviceName.ifEmpty { matchingContainer.id }}",
                "Container ID: ${matchingContainer.id}",
                "Image: ${matchingContainer.image}",
                "State: ${matchingContainer.state}",
            )
        } else {
            emptyList()
        }
    }
}

/**
 * Describes options for the desired log.
 *
 * @property podName The name of the pod that hosts the app container we are interested in.
 * @property containerName The name of the app container running inside the pod.
 * @property maxLines The maximum number of lines to load for the log. For example, if this is set
 * to 500, the returned log will show the _last 500 lines_.
 */
data class SelectedLogOptions(
    val podName: String,
    val containerName: String,
    val maxLines: Long
)
