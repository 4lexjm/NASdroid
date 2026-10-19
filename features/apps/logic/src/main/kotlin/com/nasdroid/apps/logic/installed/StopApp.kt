package com.nasdroid.apps.logic.installed

import com.nasdroid.api.websocket.app.AppApi
import com.nasdroid.apps.data.installed.CachedInstalledApp
import com.nasdroid.apps.data.installed.InstalledAppCache

/**
 * Stops an application that is in a started state. See [invoke] for details.
 */
class StopApp(
    private val appApi: AppApi,
    private val installedAppCache: InstalledAppCache,
) {

    /**
     * Stops the application with the given name.
     */
    suspend operator fun invoke(appName: String) {
        installedAppCache.setState(appName, CachedInstalledApp.State.STOPPED)
        appApi.stop(appName = appName)
    }
}
