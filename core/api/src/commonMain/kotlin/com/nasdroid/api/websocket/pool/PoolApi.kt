package com.nasdroid.api.websocket.pool

import com.nasdroid.api.websocket.ddp.EDateMillisSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Describes the TrueNAS WebSocket API "pool" namespace for managing ZFS storage pools.
 */
interface PoolApi {

    /**
     * Retrieve a list of all [Pool]s on the system (`pool.query`).
     */
    suspend fun getPools(): List<Pool>

    /**
     * Retrieve details about a single [Pool] by its [id] (`pool.get_instance`).
     */
    suspend fun getPool(id: Int): Pool

    /**
     * Start a job to discover pools available for import (`pool.import_find`).
     *
     * @return The job ID whose result will deserialize to `List<ImportablePool>`.
     */
    suspend fun findPoolsToImport(): Int

    /**
     * Start a job to import a pool (`pool.import_pool`).
     *
     * @return The job ID for the import operation.
     */
    suspend fun importPool(params: ImportPoolParams): Int

    /**
     * Start, stop, or pause a scrub on the pool with [id] (`pool.scrub`).
     *
     * @return The job ID for the scrub action.
     */
    suspend fun scrubPool(id: Int, action: ScrubAction): Int
}

/**
 * Represents the parameters for importing a ZFS pool (`pool.import_pool`).
 *
 * Note: `enable_attachments` was removed from `pool.import_pool` in TrueNAS 25.10+.
 *
 * @property guid The [ImportablePool.guid] of the pool to import.
 * @property name Optional custom name for the imported pool.
 */
@Serializable
data class ImportPoolParams(
    @SerialName("guid")
    val guid: String,
    @SerialName("name")
    val name: String? = null,
)

/**
 * Describes a ZFS pool that is available to be imported.
 *
 * @property name The name of the pool to be imported.
 * @property guid The pool's unique identifier.
 * @property status The current status of the pool.
 * @property hostname The hostname associated with the pool.
 */
@Serializable
data class ImportablePool(
    @SerialName("name")
    val name: String,
    @SerialName("guid")
    val guid: String,
    @SerialName("status")
    val status: String = "",
    @SerialName("hostname")
    val hostname: String = "",
)

/**
 * Possible pool scrub actions that can be executed via [PoolApi.scrubPool].
 */
@Serializable
enum class ScrubAction {
    @SerialName("START")
    Start,

    @SerialName("STOP")
    Stop,

    @SerialName("PAUSE")
    Pause,
}

/**
 * Describes a ZFS pool on the system.
 *
 * @property id The unique ID of the pool on this system.
 * @property name The name of the pool.
 * @property guid A globally unique identifier for this pool.
 * @property status The overall status of the pool.
 * @property path The mount path of the pool on the filesystem.
 * @property scan The most recent [Scan] that occurred, or null if none.
 * @property isUpgraded Whether the pool is on the latest ZFS version with all feature flags enabled.
 * @property healthy Whether the pool is healthy.
 * @property warning Whether this pool has an active warning.
 * @property statusCode The code for the current pool status, if issues are present.
 * @property statusDetail Reasoning for the current status.
 * @property size The total size of the pool in bytes.
 * @property allocated The number of bytes in this pool that have data allocated.
 * @property free The number of bytes in this pool that do not have data allocated.
 * @property freeing The number of bytes currently being freed.
 * @property fragmentation Pool fragmentation percentage string.
 * @property sizeStr Human-readable [size].
 * @property allocatedStr Human-readable [allocated].
 * @property freeStr Human-readable [free].
 * @property freeingStr Human-readable [freeing].
 * @property autotrim [Autotrim] configuration of the pool.
 * @property topology [Topology] of the pool's VDEVs.
 */
@Suppress("BooleanPropertyNaming")
@Serializable
data class Pool(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("guid")
    val guid: String = "",
    @SerialName("status")
    val status: PoolStatus = PoolStatus.Online,
    @SerialName("path")
    val path: String = "",
    @SerialName("scan")
    val scan: Scan? = null,
    @SerialName("is_upgraded")
    val isUpgraded: Boolean? = null,
    @SerialName("healthy")
    val healthy: Boolean = true,
    @SerialName("warning")
    val warning: Boolean = false,
    @SerialName("status_code")
    val statusCode: String? = null,
    @SerialName("status_detail")
    val statusDetail: String? = null,
    @SerialName("size")
    val size: Long? = null,
    @SerialName("allocated")
    val allocated: Long? = null,
    @SerialName("free")
    val free: Long? = null,
    @SerialName("freeing")
    val freeing: Long? = null,
    @SerialName("fragmentation")
    val fragmentation: String? = null,
    @SerialName("size_str")
    val sizeStr: String? = null,
    @SerialName("allocated_str")
    val allocatedStr: String? = null,
    @SerialName("free_str")
    val freeStr: String? = null,
    @SerialName("freeing_str")
    val freeingStr: String? = null,
    @SerialName("autotrim")
    val autotrim: Autotrim = Autotrim(),
    @SerialName("topology")
    val topology: Topology = Topology(),
) {
    /**
     * Describes a pool's Autotrim configuration.
     *
     * @property parsed The parsed autotrim value.
     * @property rawValue The raw ZFS property value.
     * @property source The property source.
     * @property value The display value.
     */
    @Serializable
    data class Autotrim(
        @SerialName("parsed")
        val parsed: String = "",
        @SerialName("rawvalue")
        val rawValue: String = "off",
        @SerialName("source")
        val source: String = "",
        @SerialName("value")
        val value: String = "OFF",
    )

    /**
     * Describes a scan (scrub/resilver) that occurred on the ZFS pool.
     *
     * @property bytesIssued The number of bytes issued.
     * @property bytesProcessed The number of bytes processed in the scan.
     * @property bytesToProcess Total number of bytes to process for this scan.
     * @property endTime Epoch milliseconds when this scan finished.
     * @property errors The number of errors found in the scan.
     * @property function The scan function (e.g. `SCRUB`), or null if never run.
     * @property pause Epoch milliseconds when this scan was paused, if paused.
     * @property percentage Completion percentage of the scan.
     * @property startTime Epoch milliseconds when this scan started.
     * @property state The state of the scan.
     * @property totalSecsLeft Estimated seconds remaining for this scan.
     */
    @Serializable
    data class Scan(
        @SerialName("bytes_issued")
        val bytesIssued: Long? = 0,
        @SerialName("bytes_processed")
        val bytesProcessed: Long? = 0,
        @SerialName("bytes_to_process")
        val bytesToProcess: Long? = 0,
        @Serializable(with = EDateMillisSerializer::class)
        @SerialName("end_time")
        val endTime: Long? = null,
        @SerialName("errors")
        val errors: Int? = 0,
        @SerialName("function")
        val function: String? = null,
        @Serializable(with = EDateMillisSerializer::class)
        @SerialName("pause")
        val pause: Long? = null,
        @SerialName("percentage")
        val percentage: Double? = 0.0,
        @Serializable(with = EDateMillisSerializer::class)
        @SerialName("start_time")
        val startTime: Long? = null,
        @SerialName("state")
        val state: ScanState? = null,
        @SerialName("total_secs_left")
        val totalSecsLeft: Long? = null,
    ) {

        /**
         * Encapsulates all possible states for a scan on a pool.
         */
        @Serializable
        enum class ScanState {
            @SerialName("FINISHED")
            Finished,

            @SerialName("SCANNING")
            Scanning,

            @SerialName("CANCELED")
            Cancelled,

            @SerialName("NONE")
            None,
        }
    }

    /**
     * Describes a pool's VDEV topology.
     *
     * @property cache A list of [VDev]s that are used for L2ARC caching.
     * @property data A list of [VDev]s that are used for primary data storage.
     * @property dedup A list of [VDev]s that are used for deduplication tables.
     * @property log A list of [VDev]s that are used for ZIL/SLOG.
     * @property spare A list of [VDev]s that are used as hot spares.
     * @property special A list of special allocation class [VDev]s.
     */
    @Serializable
    data class Topology(
        @SerialName("cache")
        val cache: List<VDev> = emptyList(),
        @SerialName("data")
        val data: List<VDev> = emptyList(),
        @SerialName("dedup")
        val dedup: List<VDev> = emptyList(),
        @SerialName("log")
        val log: List<VDev> = emptyList(),
        @SerialName("spare")
        val spare: List<VDev> = emptyList(),
        @SerialName("special")
        val special: List<VDev> = emptyList(),
    )

    /**
     * Describes a VDEV in a pool.
     *
     * @property children A list of child [VDev]s.
     * @property device The partition/device name (e.g. `sda1`).
     * @property disk The disk name (e.g. `sda`).
     * @property guid A globally unique identifier for this VDEV.
     * @property name The name of this VDEV.
     * @property path The device path of this VDEV.
     * @property stats [Stats] for this VDEV.
     * @property status The status of the VDEV.
     * @property type The VDEV layout type.
     * @property unavailDisk Details if the underlying disk is unavailable.
     */
    @Serializable
    data class VDev(
        @SerialName("children")
        val children: List<VDev> = emptyList(),
        @SerialName("device")
        val device: String? = null,
        @SerialName("disk")
        val disk: String? = null,
        @SerialName("guid")
        val guid: String = "",
        @SerialName("name")
        val name: String = "",
        @SerialName("path")
        val path: String? = null,
        @SerialName("stats")
        val stats: Stats = Stats(),
        @SerialName("status")
        val status: PoolStatus = PoolStatus.Online,
        @SerialName("type")
        val type: Type = Type.Disk,
        @SerialName("unavail_disk")
        val unavailDisk: String? = null,
    ) {

        /**
         * Encapsulates all possible VDEV types or layouts.
         */
        @Serializable
        enum class Type {
            @SerialName("DRAID1")
            DRaid1,

            @SerialName("DRAID2")
            DRaid2,

            @SerialName("DRAID3")
            DRaid3,

            @SerialName("RAIDZ1")
            RaidZ1,

            @SerialName("RAIDZ2")
            RaidZ2,

            @SerialName("RAIDZ3")
            RaidZ3,

            @SerialName("MIRROR")
            Mirror,

            @SerialName("STRIPE")
            Stripe,

            @SerialName("DISK")
            Disk,

            @SerialName("FILE")
            File,

            @SerialName("SPARE")
            Spare,

            @SerialName("LOG")
            Log,

            @SerialName("CACHE")
            Cache,

            @SerialName("SPECIAL")
            Special,

            @SerialName("DEDUP")
            Dedup,
        }
    }

    /**
     * Contains statistics for a VDEV.
     *
     * @property allocated The number of bytes allocated.
     * @property bytes Byte counters per ZIO type.
     * @property checksumErrors The number of checksum errors.
     * @property configuredAshift Configured sector size power-of-two.
     * @property fragmentation Fragmentation percentage.
     * @property logicalAshift Logical sector size power-of-two.
     * @property ops Operation counters per ZIO type.
     * @property physicalAshift Physical sector size power-of-two.
     * @property readErrors The number of read errors that have occurred.
     * @property selfHealed The number of self-healed bytes/sectors.
     * @property size Total size of the VDEV in bytes.
     * @property timestamp Timestamp of the stats snapshot.
     * @property writeErrors The number of write errors that have occurred.
     */
    @Serializable
    data class Stats(
        @SerialName("allocated")
        val allocated: Long = 0,
        @SerialName("bytes")
        val bytes: List<Long> = emptyList(),
        @SerialName("checksum_errors")
        val checksumErrors: Int = 0,
        @SerialName("configured_ashift")
        val configuredAshift: Int = 0,
        @SerialName("fragmentation")
        val fragmentation: Int = 0,
        @SerialName("logical_ashift")
        val logicalAshift: Int = 0,
        @SerialName("ops")
        val ops: List<Long> = emptyList(),
        @SerialName("physical_ashift")
        val physicalAshift: Int = 0,
        @SerialName("read_errors")
        val readErrors: Int = 0,
        @SerialName("self_healed")
        val selfHealed: Int = 0,
        @SerialName("size")
        val size: Long = 0,
        @SerialName("timestamp")
        val timestamp: Long = 0,
        @SerialName("write_errors")
        val writeErrors: Int = 0,
    )
}

/**
 * Encapsulates all possible statuses of a pool or VDEV.
 */
@Serializable
enum class PoolStatus {
    @SerialName("ONLINE")
    Online,

    @SerialName("DEGRADED")
    Degraded,

    @SerialName("OFFLINE")
    Offline,

    @SerialName("FAULTED")
    Faulted,

    @SerialName("REMOVED")
    Removed,

    @SerialName("UNAVAIL")
    Unavail,

    @SerialName("UNKNOWN")
    Unknown,
}
