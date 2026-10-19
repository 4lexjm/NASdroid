package com.nasdroid.apps.logic.installed

import com.nasdroid.api.websocket.app.AppApi
import com.nasdroid.apps.data.installed.InstalledAppCache

/**
 * Deletes an application and all its data (where possible). See [invoke] for details.
 */
class DeleteApp(
    private val appApi: AppApi,
    private val installedAppCache: InstalledAppCache,
) {

    /**
     * Deletes the specified application, and all its volumes.
     */
    suspend operator fun invoke(appName: String, deleteUnusedImages: Boolean) {
        installedAppCache.deleteInstalledApp(appName)
        appApi.delete(appName = appName, removeImages = deleteUnusedImages)
    }
}
