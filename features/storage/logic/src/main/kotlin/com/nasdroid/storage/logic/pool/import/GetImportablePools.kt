package com.nasdroid.storage.logic.pool.import

import com.nasdroid.api.websocket.core.CoreApi
import com.nasdroid.api.websocket.core.Job
import com.nasdroid.api.websocket.jsonrpc.JsonRpcSerializer
import com.nasdroid.api.websocket.pool.ImportablePool
import com.nasdroid.api.websocket.pool.PoolApi
import com.nasdroid.core.strongresult.StrongResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.builtins.ListSerializer
import kotlin.time.Duration.Companion.seconds

/**
 * Retrieves a list of pools that can be imported. See [invoke] for details.
 */
class GetImportablePools(
    private val coreApi: CoreApi,
    private val poolApi: PoolApi
) {

    /**
     * Retrieves a list of pools that can be imported, or [GetImportablePoolsError] if an error
     * occurs. Note this is a potentially long-running action.
     */
    suspend operator fun invoke(): StrongResult<List<ImportablePool>, GetImportablePoolsError> {
        val jobId = poolApi.findPoolsToImport()
        var job = coreApi.getJob(jobId)
        withTimeout(Timeout) {
            while (job.state == Job.State.Waiting || job.state == Job.State.Running) {
                delay(1.seconds)
                job = coreApi.getJob(jobId)
            }
        }
        val result = job.result
        return if (job.state != Job.State.Success || result == null) {
            StrongResult.failure(GetImportablePoolsError.Timeout)
        } else {
            val pools = JsonRpcSerializer.decodeFromJsonElement(
                ListSerializer(ImportablePool.serializer()),
                result
            )
            StrongResult.success(pools)
        }
    }

    /**
     * Constants for [GetImportablePools].
     */
    companion object {
        private val Timeout = 30.seconds
    }
}

/**
 * Encapsulates all possible failures when retrieving importable pools.
 */
enum class GetImportablePoolsError {
    /**
     * The operation timed out waiting for the job to complete.
     */
    Timeout
}

