package com.nasdroid.apps.logic.installed

import com.nasdroid.api.websocket.app.AppApi
import com.nasdroid.apps.data.installed.CachedInstalledApp
import com.nasdroid.apps.data.installed.InstalledAppCache

/**
 * Starts an application that is in a stopped state. See [invoke] for details.
 */
class StartApp(
    private val appApi: AppApi,
    private val installedAppCache: InstalledAppCache,
) {

    /**
     * Starts the application with the given name.
     */
    suspend operator fun invoke(appName: String) {
        installedAppCache.setState(appName, CachedInstalledApp.State.DEPLOYING)
        appApi.start(appName = appName)
    }
}
