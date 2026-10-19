package com.nasdroid.api.websocket.core

import com.nasdroid.api.websocket.ddp.EDateInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Describes the TrueNAS API "core" group.
 */
@Suppress("TooManyFunctions")
interface CoreApi {

    /**
     * Resolves IPv4/IPv6 Neighbor/ARP table entries.
     */
    suspend fun arp(ip: String? = null, networkInterface: String? = null): Map<String, String>

    /**
     * Setup middlewared for remote debugging.
     *
     * @param bindAddress local ip address to bind the remote debug session to.
     * @param bindPort local port to listen on.
     * @param threaded run debugger in a new thread instead of the main event loop.
     */
    suspend fun debug(bindAddress: String = "0.0.0.0", bindPort: Int = 3000, threaded: Boolean = false)

    /**
     * Returns whether middlewared is in debug mode.
     */
    suspend fun debugModeEnabled(): Boolean

    /**
     * Get information about long-running jobs. If authenticated session does not have the
     * FULL_ADMIN role, only jobs owned by the current authenticated session will be returned.
     */
    suspend fun getJobs(): List<Job>

    /**
     * Get information about a specific long-running job by its [jobId].
     */
    suspend fun getJob(jobId: Int): Job

    /**
     * Aborts the job with the given ID.
     */
    suspend fun jobAbort(jobId: Int)

    /**
     * Starts a new Job that waits for the specified job to complete.
     */
    suspend fun jobWait(jobId: Int): Int

    /**
     * Utility method which just returns "pong". Can be used to keep connection/authtoken alive
     * instead of using "ping" protocol message.
     */
    suspend fun ping(): String

    /**
     * Method that will send an ICMP echo request to "hostname" and will wait up to "timeout" for a
     * reply.
     */
    @Suppress("LongParameterList")
    suspend fun pingRemote(
        hostName: String,
        type: PingType = PingType.ICMP,
        timeout: Int = 4,
        count: Int? = null,
        networkInterface: String? = null,
        interval: Int? = null
    ): Boolean

    /**
     * Resize terminal session (/websocket/shell) to cols x rows.
     */
    suspend fun resizeShell(id: String?, cols: Int?, rows: Int?)

    /**
     * Get currently open websocket sessions.
     */
    suspend fun sessions(): List<Session>

    /**
     * Set debug_mode for middleware.
     */
    suspend fun setDebugMode(debugMode: Boolean)
}

/**
 * Describes an open WebSocket session on the TrueNAS server.
 *
 * @property id Unique session ID.
 * @property socketFamily Socket family string.
 * @property address Client address.
 * @property isAuthenticated Whether the session is authenticated.
 * @property callCount Number of RPC calls executed on this session.
 */
@Serializable
data class Session(
    @SerialName("id")
    val id: String,
    @SerialName("socket_family")
    val socketFamily: String,
    @SerialName("address")
    val address: String,
    @SerialName("authenticated")
    val isAuthenticated: Boolean,
    @SerialName("call_count")
    val callCount: Int
)

/**
 * ICMP ping protocol types.
 */
@Serializable
enum class PingType {
    @SerialName("ICMP")
    ICMP,
    @SerialName("ICMPV4")
    ICMPV4,
    @SerialName("ICMPV6")
    ICMPV6
}

/**
 * Contains information about a single job on the system.
 *
 * @property id The job ID.
 * @property method The method this job called.
 * @property arguments A list of arguments the method was called with.
 * @property isTransient Whether the job is transient.
 * @property description A human-readable description of the job.
 * @property isAbortable Whether the job can be aborted.
 * @property logsPath Filesystem path to the job logs, if any.
 * @property logsExcerpt Excerpt of the job logs, if any.
 * @property progress Information about the jobs current progress.
 * @property result The result of the job, or null if there is no result.
 * @property resultEncodingError Any encoding error encountered when serializing the result.
 * @property error A human-readable error message, if any.
 * @property exception The exception that occurred when the job failed.
 * @property excInfo Structured exception info if the job failed.
 * @property state The state of the job.
 * @property timeStarted The timestamp of when this job was started.
 * @property timeFinished The timestamp of when this job finished, if any.
 * @property credentials The credentials of the user that initiated the job.
 */
@Serializable
data class Job(
    @SerialName("id")
    val id: Int,
    @SerialName("method")
    val method: String,
    @SerialName("arguments")
    val arguments: List<JsonElement> = emptyList(),
    @SerialName("transient")
    val isTransient: Boolean = false,
    @SerialName("description")
    val description: String? = null,
    @SerialName("abortable")
    val isAbortable: Boolean = false,
    @SerialName("logs_path")
    val logsPath: String? = null,
    @SerialName("logs_excerpt")
    val logsExcerpt: String? = null,
    @SerialName("progress")
    val progress: Progress,
    @SerialName("result")
    val result: JsonElement? = null,
    @SerialName("result_encoding_error")
    val resultEncodingError: String? = null,
    @SerialName("error")
    val error: String? = null,
    @SerialName("exception")
    val exception: String? = null,
    @SerialName("exc_info")
    val excInfo: JobExcInfo? = null,
    @SerialName("state")
    val state: State,
    @Serializable(with = EDateInstantSerializer::class)
    @SerialName("time_started")
    val timeStarted: Instant,
    @Serializable(with = EDateInstantSerializer::class)
    @SerialName("time_finished")
    val timeFinished: Instant? = null,
    @SerialName("credentials")
    val credentials: Credentials? = null
) {

    /**
     * Credentials metadata for the user who started a job.
     *
     * @property type Credential type identifier.
     * @property data Additional credential metadata.
     */
    @Serializable
    data class Credentials(
        @SerialName("type")
        val type: String,
        @SerialName("data")
        val data: Map<String, JsonElement> = emptyMap()
    )

    /**
     * Describes the current progress of a running job.
     *
     * @property percent The percentage of work completed. Between 0 and 100, or null.
     * @property description A description for the job progress.
     * @property extra Extra progress payload.
     */
    @Serializable
    data class Progress(
        @SerialName("percent")
        val percent: Double? = null,
        @SerialName("description")
        val description: String? = null,
        @SerialName("extra")
        val extra: JsonElement? = null
    )

    /**
     * Encapsulates all possible states for a [Job].
     */
    @Serializable
    enum class State {
        @SerialName("WAITING")
        Waiting,
        @SerialName("RUNNING")
        Running,
        @SerialName("SUCCESS")
        Success,
        @SerialName("FAILED")
        Failed,
        @SerialName("ABORTED")
        Aborted,
        @SerialName("starting")
        Starting,
        @SerialName("running")
        LegacyRunning,
        @SerialName("exited")
        Exited,
        @SerialName("DEPLOYING")
        Deploying,
    }
}

/**
 * Structured exception information for a failed [Job].
 *
 * @property repr String representation of the exception.
 * @property type Exception class/type name.
 * @property extra Additional structured error details.
 */
@Serializable
data class JobExcInfo(
    @SerialName("repr")
    val repr: String? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("extra")
    val extra: JsonElement? = null
)
