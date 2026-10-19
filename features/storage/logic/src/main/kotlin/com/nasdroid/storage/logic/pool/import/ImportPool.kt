package com.nasdroid.storage.logic.pool.import

import com.nasdroid.api.websocket.core.CoreApi
import com.nasdroid.api.websocket.core.Job
import com.nasdroid.api.websocket.pool.ImportPoolParams
import com.nasdroid.api.websocket.pool.PoolApi
import com.nasdroid.core.strongresult.StrongResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/**
 * Imports a pool, discovered by [GetImportablePools]. See [invoke] for details.
 */
class ImportPool(
    private val coreApi: CoreApi,
    private val poolApi: PoolApi
) {

    /**
     * Imports a pool by its GUID. See [ImportPoolError] for possible failure modes.
     *
     * @param guid The GUID of the pool to import.
     * @param enableAttachments Retained for caller compatibility (ignored in TrueNAS 25.10+ where
     * `enable_attachments` was removed from `pool.import_pool`).
     * @param name Optional name for the imported pool. If not specified, the pool's original name is
     * used.
     */
    @Suppress("UnusedParameter")
    suspend operator fun invoke(
        guid: String,
        enableAttachments: Boolean = true,
        name: String? = null,
    ): StrongResult<Unit, ImportPoolError> {
        val jobId = poolApi.importPool(
            ImportPoolParams(
                guid = guid,
                name = name,
            )
        )
        var job = coreApi.getJob(jobId)
        withTimeout(Timeout) {
            while (job.state == Job.State.Waiting || job.state == Job.State.Running) {
                delay(1.seconds)
                job = coreApi.getJob(jobId)
            }
        }
        return when (job.state) {
            Job.State.Success -> {
                StrongResult.success(Unit)
            }
            else -> {
                if (job.error == "[ENOENT] Pool  not found." || job.error?.contains("ENOENT") == true) {
                    StrongResult.failure(ImportPoolError.PoolNotFound)
                } else {
                    StrongResult.failure(ImportPoolError.PoolImportFailed)
                }
            }
        }
    }

    /**
     * Constants for [ImportPool].
     */
    companion object {
        private val Timeout = 30.seconds
    }
}

/**
 * Encapsulates the failure modes for importing a pool.
 */
enum class ImportPoolError {
    /**
     * The requested pool was not found on the system.
     */
    PoolNotFound,

    /**
     * The pool import job failed or encountered an error.
     */
    PoolImportFailed
}

