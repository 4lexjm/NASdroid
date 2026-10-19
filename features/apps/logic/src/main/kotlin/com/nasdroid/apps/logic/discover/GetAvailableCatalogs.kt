package com.nasdroid.apps.logic.discover

import com.nasdroid.api.v2.exception.HttpNotOkException
import com.nasdroid.api.websocket.catalog.CatalogApi
import com.nasdroid.api.websocket.jsonrpc.MethodCallError

/**
 * Retrieves a list of catalogs that are providing apps to the user. See [invoke] for details.
 */
class GetAvailableCatalogs(
    private val catalogApi: CatalogApi
) {

    /**
     * Get a list of Strings representing user-defined catalog labels.
     */
    suspend operator fun invoke(): Result<List<String>> {
        return try {
            val config = catalogApi.config()
            Result.success(listOf(config.label))
        } catch (e: MethodCallError) {
            Result.failure(e)
        } catch (e: HttpNotOkException) {
            Result.failure(e)
        }
    }
}
