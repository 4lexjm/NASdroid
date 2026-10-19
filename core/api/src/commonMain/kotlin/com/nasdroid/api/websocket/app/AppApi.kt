package com.nasdroid.api.websocket.app

import com.nasdroid.api.websocket.ddp.EDateMillisSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Describes the TrueNAS WebSocket API for managing and discovering applications ("app" namespace).
 *
 * In TrueNAS 24.10+ (Electric Eel, Fangtooth 25.04, Goldeye 25.10, 26.04+), this Docker Compose-based
 * `app.*` namespace replaces the removed Kubernetes `chart.release.*` namespace.
 */
interface AppApi {

    /**
     * Retrieve a list of all available applications across configured catalog trains.
     */
    suspend fun available(): List<AvailableApp>

    /**
     * Retrieve a list of all app categories.
     */
    suspend fun categories(): List<String>

    /**
     * Retrieve a list of the latest available applications.
     */
    suspend fun latest(): List<AvailableApp>

    /**
     * Retrieve a list of applications similar to [appName] in [train].
     */
    suspend fun similar(appName: String, train: String): List<AvailableApp>

    /**
     * Retrieve a list of all installed applications.
     */
    suspend fun query(): List<InstalledApp>

    /**
     * Retrieve a single installed application by its [name].
     */
    suspend fun getInstance(name: String): InstalledApp

    /**
     * Start the application with [appName]. Returns a job ID.
     */
    suspend fun start(appName: String): Int

    /**
     * Stop the application with [appName]. Returns a job ID.
     */
    suspend fun stop(appName: String): Int

    /**
     * Delete the application with [appName]. Returns a job ID.
     */
    suspend fun delete(
        appName: String,
        removeImages: Boolean = true,
        removeIxVolumes: Boolean = false,
        forceRemoveCustomApp: Boolean = false,
    ): Int

    /**
     * Retrieve the list of versions that [appName] can be rolled back to.
     */
    suspend fun rollbackVersions(appName: String): List<String>

    /**
     * Roll back [appName] to [appVersion]. Returns a job ID.
     */
    suspend fun rollback(
        appName: String,
        appVersion: String,
        rollbackSnapshot: Boolean = false,
    ): Int

    /**
     * Retrieve container details keyed by container ID for [appName].
     */
    suspend fun containerIds(appName: String): Map<String, ContainerDetails>
}

/**
 * Details of an available application from the TrueNAS catalog.
 *
 * @property appReadme A README string describing the app.
 * @property categories The list of categories this app belongs to.
 * @property description A short description of the app.
 * @property healthy Whether the catalog definition is healthy.
 * @property healthyError An error message if the app definition is not healthy.
 * @property home The upstream homepage URL of the app.
 * @property location The filesystem path of the app definition on the server.
 * @property latestVersion The latest chart/definition version of the app.
 * @property latestAppVersion The latest upstream application version.
 * @property latestHumanVersion A human-readable representation of the latest version.
 * @property lastUpdate Timestamp or date string of the last update.
 * @property name The identifier name of the app.
 * @property recommended Whether the app is recommended.
 * @property title The display title of the app.
 * @property maintainers The list of maintainers of the app definition.
 * @property tags Tags associated with the app.
 * @property screenshots URLs to screenshots of the app.
 * @property sources URLs to sources for the app.
 * @property iconUrl URL to the app icon.
 * @property catalog The catalog name (defaults to `"TRUENAS"` in TrueNAS 25.04+ where third-party catalogs were removed).
 * @property installed Whether the app is currently installed.
 * @property train The catalog train this app belongs to.
 */
@Suppress("BooleanPropertyNaming")
@Serializable
data class AvailableApp(
    @SerialName("app_readme")
    val appReadme: String? = null,
    @SerialName("categories")
    val categories: List<String> = emptyList(),
    @SerialName("description")
    val description: String = "",
    @SerialName("healthy")
    val healthy: Boolean = true,
    @SerialName("healthy_error")
    val healthyError: String? = null,
    @SerialName("home")
    val home: String = "",
    @SerialName("location")
    val location: String = "",
    @SerialName("latest_version")
    val latestVersion: String = "",
    @SerialName("latest_app_version")
    val latestAppVersion: String = "",
    @SerialName("latest_human_version")
    val latestHumanVersion: String = "",
    @Serializable(with = EDateMillisSerializer::class)
    @SerialName("last_update")
    val lastUpdate: Long = 0,
    @SerialName("name")
    val name: String,
    @SerialName("recommended")
    val recommended: Boolean = false,
    @SerialName("title")
    val title: String = "",
    @SerialName("maintainers")
    val maintainers: List<Maintainer> = emptyList(),
    @SerialName("tags")
    val tags: List<String> = emptyList(),
    @SerialName("screenshots")
    val screenshots: List<String> = emptyList(),
    @SerialName("sources")
    val sources: List<String> = emptyList(),
    @SerialName("icon_url")
    val iconUrl: String? = null,
    @SerialName("catalog")
    val catalog: String = "TRUENAS",
    @SerialName("installed")
    val installed: Boolean = false,
    @SerialName("train")
    val train: String = "stable",
) {
    /**
     * Information about an application maintainer.
     *
     * @property email The maintainer's email address.
     * @property name The maintainer's name.
     * @property url The maintainer's website URL.
     */
    @Serializable
    data class Maintainer(
        @SerialName("email")
        val email: String = "",
        @SerialName("name")
        val name: String = "",
        @SerialName("url")
        val url: String? = null,
    )
}

/**
 * Details of an installed application in TrueNAS 24.10+ (`app.query` / `app.get_instance`).
 *
 * @property name The unique instance name of the installed application.
 * @property id The identifier of the installed application.
 * @property state The current lifecycle state of the application.
 * @property upgradeAvailable Whether an upgrade is available for this application.
 * @property latestVersion The latest available version if an upgrade is available.
 * @property imageUpdatesAvailable Whether container image updates are available.
 * @property customApp Whether this is a custom Docker Compose application.
 * @property migrated Whether this application was migrated from Kubernetes.
 * @property humanVersion Human-readable version string of the installed application.
 * @property version The installed chart/app definition version.
 * @property metadata Metadata describing the application from the catalog.
 * @property activeWorkloads Information about running containers, ports, and images.
 * @property notes Optional notes or markdown documentation for the installed application.
 * @property portals Map of portal names to their web URLs.
 */
@Suppress("BooleanPropertyNaming")
@Serializable
data class InstalledApp(
    @SerialName("name")
    val name: String,
    @SerialName("id")
    val id: String = name,
    @SerialName("state")
    val state: State = State.STOPPED,
    @SerialName("upgrade_available")
    val upgradeAvailable: Boolean = false,
    @SerialName("latest_version")
    val latestVersion: String? = null,
    @SerialName("image_updates_available")
    val imageUpdatesAvailable: Boolean = false,
    @SerialName("custom_app")
    val customApp: Boolean = false,
    @SerialName("migrated")
    val migrated: Boolean = false,
    @SerialName("human_version")
    val humanVersion: String = "",
    @SerialName("version")
    val version: String = "",
    @SerialName("metadata")
    val metadata: AppMetadata = AppMetadata(),
    @SerialName("active_workloads")
    val activeWorkloads: ActiveWorkloads = ActiveWorkloads(),
    @SerialName("notes")
    val notes: String? = null,
    @SerialName("portals")
    val portals: Map<String, String> = emptyMap(),
) {
    /**
     * Possible lifecycle states of an installed application in TrueNAS 24.10+.
     */
    @Serializable
    enum class State {
        @SerialName("CRASHED")
        CRASHED,

        @SerialName("DEPLOYING")
        DEPLOYING,

        @SerialName("RUNNING")
        RUNNING,

        @SerialName("STOPPED")
        STOPPED,

        @SerialName("STOPPING")
        STOPPING,

        @SerialName("ERROR")
        ERROR,
    }
}

/**
 * Catalog metadata associated with an installed application.
 *
 * @property appVersion The upstream application version.
 * @property categories Categories the application belongs to.
 * @property description Short description of the application.
 * @property home Upstream homepage URL.
 * @property icon URL to the application icon.
 * @property name Catalog application name.
 * @property title Display title of the application.
 * @property train Catalog train name.
 * @property version Catalog definition version.
 * @property sources Upstream source URLs.
 * @property maintainers List of maintainers.
 */
@Serializable
data class AppMetadata(
    @SerialName("app_version")
    val appVersion: String = "",
    @SerialName("categories")
    val categories: List<String> = emptyList(),
    @SerialName("description")
    val description: String = "",
    @SerialName("home")
    val home: String = "",
    @SerialName("icon")
    val icon: String? = null,
    @SerialName("name")
    val name: String = "",
    @SerialName("title")
    val title: String = "",
    @SerialName("train")
    val train: String = "stable",
    @SerialName("version")
    val version: String = "",
    @SerialName("sources")
    val sources: List<String> = emptyList(),
    @SerialName("maintainers")
    val maintainers: List<AvailableApp.Maintainer> = emptyList(),
)

/**
 * Information about the active Docker Compose workloads of an installed application.
 *
 * @property containers Total number of containers in the application.
 * @property usedPorts List of ports exposed by the application.
 * @property containerDetails Details of individual containers belonging to the application.
 * @property images List of container image tags used by the application.
 * @property volumes List of volumes mounted by the application.
 */
@Serializable
data class ActiveWorkloads(
    @SerialName("containers")
    val containers: Int = 0,
    @SerialName("used_ports")
    val usedPorts: List<UsedPort> = emptyList(),
    @SerialName("container_details")
    val containerDetails: List<ContainerDetails> = emptyList(),
    @SerialName("images")
    val images: List<String> = emptyList(),
    @SerialName("volumes")
    val volumes: List<Volume> = emptyList(),
)

/**
 * Details of a network port exposed by an application container.
 *
 * @property containerPort Port number inside the container.
 * @property protocol Network protocol (e.g. `tcp`, `udp`).
 * @property hostPorts Host port bindings for this container port.
 */
@Serializable
data class UsedPort(
    @SerialName("container_port")
    val containerPort: Int = 0,
    @SerialName("protocol")
    val protocol: String = "tcp",
    @SerialName("host_ports")
    val hostPorts: List<HostPort> = emptyList(),
)

/**
 * Host IP and port binding for an exposed container port.
 *
 * @property hostPort The port number on the TrueNAS host.
 * @property hostIp The IP address bound on the TrueNAS host.
 */
@Serializable
data class HostPort(
    @SerialName("host_port")
    val hostPort: Int = 0,
    @SerialName("host_ip")
    val hostIp: String = "0.0.0.0",
)

/**
 * Details of an individual Docker container within an installed application.
 *
 * @property id The Docker container ID.
 * @property serviceName The Docker Compose service name.
 * @property image The container image name.
 * @property state The current state of the container.
 */
@Serializable
data class ContainerDetails(
    @SerialName("id")
    val id: String,
    @SerialName("service_name")
    val serviceName: String = "",
    @SerialName("image")
    val image: String = "",
    @SerialName("state")
    val state: String = "",
)

/**
 * Details of a volume mounted into an application container.
 *
 * @property source The host path or volume source.
 * @property destination The mount path inside the container.
 * @property mode Optional mount mode.
 * @property type Optional volume type.
 */
@Serializable
data class Volume(
    @SerialName("source")
    val source: String = "",
    @SerialName("destination")
    val destination: String = "",
    @SerialName("mode")
    val mode: String? = null,
    @SerialName("type")
    val type: String? = null,
)
