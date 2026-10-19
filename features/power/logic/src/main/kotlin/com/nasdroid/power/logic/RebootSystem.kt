package com.nasdroid.power.logic

import com.nasdroid.api.v2.exception.ClientUnauthorizedException
import com.nasdroid.api.v2.exception.HttpNotOkException
import com.nasdroid.api.websocket.jsonrpc.MethodCallError
import com.nasdroid.api.websocket.system.SystemApi
import com.nasdroid.core.strongresult.StrongResult

/**
 * Reboots the system. See [invoke] for details.
 */
class RebootSystem(
    private val systemApi: SystemApi
) {
    /**
     * Reboots the system, and suspends until the task completes. This task can fail, see
     * [RebootError] for detailed failure modes.
     */
    suspend operator fun invoke(reason: String = "Requested from NASdroid"): StrongResult<Unit, RebootError> {
        return try {
            systemApi.reboot(reason = reason)
            StrongResult.success(Unit)
        } catch (e: MethodCallError) {
            if (e.message?.contains("Not authenticated", ignoreCase = true) == true) {
                StrongResult.failure(RebootError.Unauthorized)
            } else {
                StrongResult.failure(RebootError.GenericNetworkError(e.errorCode, e.message.orEmpty()))
            }
        } catch (_: ClientUnauthorizedException) {
            StrongResult.failure(RebootError.Unauthorized)
        } catch (e: HttpNotOkException) {
            StrongResult.failure(RebootError.GenericNetworkError(e.code, e.description))
        }
    }
}

/**
 * Encapsulates all possible failure modes for [RebootSystem].
 */
sealed interface RebootError {

    /**
     * The currently authenticated user is not authorized to reboot the system.
     */
    data object Unauthorized : RebootError

    /**
     * The server returned a status that was not OK, but also not a known failure type.
     *
     * @property httpResponseCode [HTTP response status code](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status).
     * @property httpResponseDescription A description for this failure. This might be raw data sent
     * from the server.
     */
    data class GenericNetworkError(
        val httpResponseCode: Int,
        val httpResponseDescription: String,
    ) : RebootError
}
