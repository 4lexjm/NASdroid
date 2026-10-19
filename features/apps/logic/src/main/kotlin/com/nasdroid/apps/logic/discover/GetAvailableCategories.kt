package com.nasdroid.apps.logic.discover

import com.nasdroid.api.v2.exception.HttpNotOkException
import com.nasdroid.api.websocket.app.AppApi
import com.nasdroid.api.websocket.jsonrpc.MethodCallError

/**
 * Get a list of all available categories provided by all catalogs. See [invoke] for details.
 */
class GetAvailableCategories(
    private val appApi: AppApi
) {

    /**
     * Retrieves a list of Strings representing every available app category. Categories are
     * provided by catalogs, and as such have no guarantees on their human-friendliness.
     */
    suspend operator fun invoke(): Result<List<String>> {
        return try {
            val categories = appApi.categories()
            Result.success(categories)
        } catch (e: MethodCallError) {
            Result.failure(e)
        } catch (e: HttpNotOkException) {
            Result.failure(e)
        }
    }
}
