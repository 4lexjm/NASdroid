package com.nasdroid.dashboard.logic.dataloading.memory

import com.boswelja.capacity.Capacity
import com.boswelja.capacity.Capacity.Companion.bytes
import com.nasdroid.api.v2.exception.HttpNotOkException
import com.nasdroid.api.websocket.jsonrpc.MethodCallError
import com.nasdroid.api.websocket.system.SystemApi

/**
 * Retrieves specifications for physical memory installed in the system. See [invoke] for details.
 */
class GetMemorySpecs(
    private val systemApi: SystemApi,
) {

    /**
     * Returns a [Result] that contains either [MemorySpecs], or an exception if the request
     * failed.
     */
    suspend operator fun invoke(): Result<MemorySpecs> {
        return try {
            val systemInformation = systemApi.info()
            Result.success(
                MemorySpecs(
                    isEcc = systemInformation.eccMemory,
                    totalCapacity = systemInformation.physicalMemory.bytes
                )
            )
        } catch (e: MethodCallError) {
            Result.failure(e)
        } catch (e: HttpNotOkException) {
            Result.failure(e)
        }
    }
}

/**
 * Physical memory specifications.
 *
 * @property isEcc Whether the memory is ECC (has error correcting code).
 * @property totalCapacity The total capacity of installed memory.
 */
data class MemorySpecs(
    val isEcc: Boolean,
    val totalCapacity: Capacity
)
