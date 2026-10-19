package com.nasdroid.apps.logic.installed

import com.nasdroid.api.websocket.app.AppApi

/**
 * Gets the available rollback configuration for an application installed on the system. See
 * [invoke] for details.
 */
class GetRollbackOptions(
    private val appApi: AppApi
) {

    /**
     * Gets a [RollbackOptions] for the installed application with the given name.
     */
    suspend operator fun invoke(appName: String): RollbackOptions {
        val availableVersions = appApi.rollbackVersions(appName)
        return RollbackOptions(
            canRollBack = availableVersions.isNotEmpty(),
            availableVersions = availableVersions.sortedDescending()
        )
    }
}

/**
 * Describes the configuration available to the user when attempting to roll back an app to an
 * earlier version.
 *
 * @property canRollBack Whether rolling back the app is possible.
 * @property availableVersions A list of available versions that the app can be rolled back to.
 * Versions are sorted such that the highest (latest) version is at the top, and the lowest
 * (earliest) version is at the bottom.
 */
data class RollbackOptions(
    val canRollBack: Boolean,
    val availableVersions: List<String>
)
