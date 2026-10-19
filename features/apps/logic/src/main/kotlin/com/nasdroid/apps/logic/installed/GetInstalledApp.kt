package com.nasdroid.apps.logic.installed

import com.nasdroid.api.v2.exception.HttpNotOkException
import com.nasdroid.api.websocket.app.AppApi
import com.nasdroid.api.websocket.app.InstalledApp
import com.nasdroid.api.websocket.jsonrpc.MethodCallError
import kotlinx.datetime.Instant

/**
 * Retrieves details about a single app installed on the system. See [invoke] for details.
 */
class GetInstalledApp(
    private val appApi: AppApi
) {

    /**
     * Get a single [InstalledAppDetails] for the app whose name matches [appName].
     */
    suspend operator fun invoke(appName: String): Result<InstalledAppDetails> {
        return try {
            val installedApp = appApi.getInstance(appName)
            Result.success(
                InstalledAppDetails(
                    name = installedApp.id,
                    iconUrl = installedApp.metadata.icon.orEmpty(),
                    notes = installedApp.notes,
                    appVersion = installedApp.metadata.appVersion.ifEmpty { installedApp.humanVersion },
                    chartVersion = installedApp.metadata.version.ifEmpty { installedApp.version },
                    lastUpdated = null, // TODO
                    sources = installedApp.metadata.sources,
                    developer = installedApp.metadata.maintainers.firstOrNull()?.name,
                    catalog = "TRUENAS",
                    train = installedApp.metadata.train,
                    state = when (installedApp.state) {
                        InstalledApp.State.DEPLOYING,
                        InstalledApp.State.STOPPING -> InstalledAppDetails.State.DEPLOYING
                        InstalledApp.State.RUNNING -> InstalledAppDetails.State.ACTIVE
                        InstalledApp.State.STOPPED,
                        InstalledApp.State.CRASHED,
                        InstalledApp.State.ERROR -> InstalledAppDetails.State.STOPPED
                    },
                    hasUpdateAvailable = installedApp.upgradeAvailable || installedApp.imageUpdatesAvailable,
                    webPortalUrl = installedApp.portals.values.firstOrNull()
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
 * Describes basic information about an installed application.
 *
 * @property name The user-specified name given to the application.
 * @property iconUrl The URL of the application icon.
 * @property notes A Markdown-formatted block of text used as short documentation for the app.
 * @property appVersion The version of the app that is installed in the container.
 * @property chartVersion The version of the container the app is installed in.
 * @property lastUpdated Indicates when the last update was installed, if possible.
 * @property sources A list of URLs linking to various sources used in the app.
 * @property developer The details of the app maintainer, if available.
 * @property catalog The catalog this application was installed from.
 * @property train The catalog train this application was installed from.
 * @property state The current state of the application.
 * @property hasUpdateAvailable Whether the application has an update available.
 * @property webPortalUrl The URL to the applications web interface, if any.
 */
data class InstalledAppDetails(
    val name: String,
    val iconUrl: String,
    val notes: String?,
    val appVersion: String,
    val chartVersion: String,
    val lastUpdated: Instant?,
    val sources: List<String>,
    val developer: String?,
    val catalog: String,
    val train: String,
    val state: State,
    val hasUpdateAvailable: Boolean,
    val webPortalUrl: String?
) {
    /**
     * Possible states an application may be in.
     */
    enum class State {
        /**
         * The app is deploying or updating.
         */
        DEPLOYING,

        /**
         * The app is running.
         */
        ACTIVE,

        /**
         * The app is stopped.
         */
        STOPPED
    }
}
