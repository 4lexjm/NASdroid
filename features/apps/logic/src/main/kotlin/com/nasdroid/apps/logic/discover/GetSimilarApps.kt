package com.nasdroid.apps.logic.discover

import com.nasdroid.api.v2.exception.HttpNotOkException
import com.nasdroid.api.websocket.app.AppApi
import com.nasdroid.api.websocket.jsonrpc.MethodCallError
import com.nasdroid.apps.logic.discover.AvailableApp.Companion.toSanitizedModel

/**
 * Retrieves a list of [AvailableApp]s similar to another specified app. See [invoke] for details.
 */
class GetSimilarApps(
    private val appApi: AppApi
) {
    /**
     * Retrieves a list of [AvailableApp]s similar to the app specified by parameters.
     *
     * @param appId The [AvailableApp.id] of the app whose similar apps we are looking for.
     * @param catalogName The [AvailableApp.catalogName] of the app whose similar apps we are
     * looking for.
     * @param catalogTrain The [AvailableApp.catalogTrain] of the app whose similar apps we are
     * looking for.
     */
    @Suppress("UnusedParameter")
    suspend operator fun invoke(appId: String, catalogName: String, catalogTrain: String): Result<List<AvailableApp>> {
        return try {
            val result = appApi.similar(appId, catalogTrain)
                .map { it.toSanitizedModel() }
            Result.success(result)
        } catch (e: MethodCallError) {
            Result.failure(e)
        } catch (e: HttpNotOkException) {
            Result.failure(e)
        }
    }
}
