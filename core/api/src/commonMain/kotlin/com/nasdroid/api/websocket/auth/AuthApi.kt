package com.nasdroid.api.websocket.auth

import com.nasdroid.api.websocket.ddp.EDateInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Describes the TrueNAS API "Auth" group. Note these mappings may not be 1:1, as we will
 * rearrange data to be more accessible in Kotlin.
 */
@Suppress("TooManyFunctions")
interface AuthApi {

    /**
     * Verifies the given username/password combination is valid.
     */
    suspend fun checkPassword(username: String, password: String): Boolean

    /**
     * Verifies the given username/password combination is valid.
     */
    suspend fun checkUser(username: String, password: String): Boolean

    /**
     * Generate a token to be used for authentication.
     *
     * @param timeToLive The token will be invalidated if the connection has been inactive for a time
     * greater than this.
     * @param attrs A general purpose object/dictionary to hold information about the token.
     * @param matchOrigin Whether the token will only allow using this token from the same IP
     * address or with the same user UID.
     */
    suspend fun generateToken(
        timeToLive: Duration = 600.seconds,
        attrs: Map<String, Any> = emptyMap(),
        matchOrigin: Boolean = false
    ): String

    /**
     * Authenticate session using username and password. [otpToken] must be specified if two-factor authentication is
     * enabled.
     */
    suspend fun logIn(username: String, password: String, otpToken: String? = null): Boolean

    /**
     * Authenticate session using API key.
     *
     * @param apiKey The API key string.
     * @param username Optional username associated with the API key for `auth.login_ex` (`API_KEY_PLAIN`).
     */
    suspend fun logInWithApiKey(apiKey: String, username: String? = null): Boolean

    /**
     * Authenticate session using token generated with [generateToken].
     */
    suspend fun logInWithToken(token: String): Boolean

    /**
     * Deauthenticate the session. If a token exists, it is removed from the session.
     */
    suspend fun logOut(): Boolean

    /**
     * Gets the currently logged-in user.
     */
    suspend fun me(): AuthenticatedUser

    /**
     * Get a list of active auth sessions.
     *
     * This method is accessible to the users granted with the following roles:
     * AUTH_SESSIONS_READ, AUTH_SESSIONS_WRITE, READONLY_ADMIN, SHARING_ADMIN
     */
    suspend fun sessions(): List<Session>

    /**
     * Set current users `attributes` dictionary [key] to [value].
     */
    suspend fun setAttribute(key: String, value: String)

    /**
     * Terminates all other sessions (except the current one).
     *
     * This method is accessible to the users granted with the following roles: AUTH_SESSIONS_WRITE
     */
    suspend fun terminateOtherSessions()

    /**
     * Terminates the session with the given ID.
     */
    suspend fun terminateSession(sessionId: String): Boolean

    /**
     * Returns true if two-factor authorization is required for authorizing user's login.
     */
    suspend fun twoFactorAuth(username: String, password: String): Boolean
}

/**
 * Details of the currently authenticated TrueNAS user returned by `auth.me`.
 *
 * @property name The username (`pw_name`).
 * @property gecos The user full name or comment (`pw_gecos`).
 * @property dir The user home directory (`pw_dir`).
 * @property shell The user login shell (`pw_shell`).
 * @property uid The numeric user ID (`pw_uid`).
 * @property gid The numeric primary group ID (`pw_gid`).
 * @property groupList List of supplementary group IDs (`grouplist`).
 * @property sid Optional security identifier (`sid`).
 * @property source The directory source of the user account.
 * @property isLocal Whether the user is a local account (`local`).
 * @property attributes Arbitrary user attributes map (`attributes`).
 * @property twoFactorConfig Two-factor authentication configuration (`two_factor_config`).
 */
@Serializable
data class AuthenticatedUser(
    @SerialName("pw_name")
    val name: String,
    @SerialName("pw_gecos")
    val gecos: String = "",
    @SerialName("pw_dir")
    val dir: String = "",
    @SerialName("pw_shell")
    val shell: String = "",
    @SerialName("pw_uid")
    val uid: String = "",
    @SerialName("pw_gid")
    val gid: String = "",
    @SerialName("grouplist")
    val groupList: List<String> = emptyList(),
    @SerialName("sid")
    val sid: String? = null,
    @SerialName("source")
    val source: Source? = null,
    @SerialName("local")
    val isLocal: Boolean = true,
    @SerialName("attributes")
    val attributes: Map<String, JsonElement> = emptyMap(),
    @SerialName("two_factor_config")
    val twoFactorConfig: JsonElement? = null,
) {
    /**
     * Directory source of an authenticated user.
     */
    @Serializable
    enum class Source {
        @SerialName("LOCAL")
        Local,
        @SerialName("ACTIVEDIRECTORY")
        ActiveDirectory,
        @SerialName("LDAP")
        Ldap
    }
}

/**
 * Describes an active authentication session on the TrueNAS server.
 *
 * @property id The unique session identifier.
 * @property isCurrent Whether this session is the caller's current session.
 * @property isInternal Whether this session is internal to the system.
 * @property origin The network origin of the session.
 * @property credentials The type of credentials used to establish the session.
 * @property credentialsData Additional metadata describing the credentials.
 * @property createdAt The timestamp when the session was created.
 */
@Serializable
data class Session(
    @SerialName("id")
    val id: String,
    @SerialName("current")
    val isCurrent: Boolean,
    @SerialName("internal")
    val isInternal: Boolean,
    @SerialName("origin")
    val origin: String,
    @SerialName("credentials")
    val credentials: CredentialType? = null,
    @SerialName("credentials_data")
    val credentialsData: Map<String, JsonElement> = emptyMap(),
    @Serializable(with = EDateInstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,
) {
    /**
     * Possible credential types for an active TrueNAS session.
     */
    @Serializable
    enum class CredentialType {
        @SerialName("UNIX_SOCKET")
        UnixSocket,
        @SerialName("ROOT_TCP_SOCKET")
        RootTcpSocket,
        @SerialName("LOGIN_PASSWORD")
        LoginPassword,
        @SerialName("API_KEY")
        ApiKey,
        @SerialName("TOKEN")
        Token
    }
}
