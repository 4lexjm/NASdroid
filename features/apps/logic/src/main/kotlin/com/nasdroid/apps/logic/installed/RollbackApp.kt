package com.nasdroid.apps.logic.installed

import com.nasdroid.api.v2.exception.HttpNotOkException
import com.nasdroid.api.websocket.app.AppApi
import com.nasdroid.api.websocket.jsonrpc.MethodCallError
import com.nasdroid.apps.data.installed.InstalledAppCache

/**
 * Reverts an app to a previous version. See [invoke] for details.
 */
class RollbackApp(
    private val appApi: AppApi,
    private val installedAppCache: InstalledAppCache,
) {

    /**
     * Rolls back the app to the specified [targetVersion]. If [rollbackSnapshots] is true, then
     * associated storage volumes with snapshots will also be rolled back.
     */
    suspend operator fun invoke(
        appName: String,
        targetVersion: String,
        rollbackSnapshots: Boolean
    ): Result<Unit> {
        return try {
            appApi.rollback(
                appName = appName,
                appVersion = targetVersion,
                rollbackSnapshot = rollbackSnapshots
            )
            installedAppCache.setAppVersion(
                appName = appName,
                version = targetVersion,
                updateAvailable = true // We assume there will be an update available after downgrading
            )
            Result.success(Unit)
        } catch (e: MethodCallError) {
            Result.failure(e)
        } catch (e: HttpNotOkException) {
            Result.failure(e)
        }
    }
}
