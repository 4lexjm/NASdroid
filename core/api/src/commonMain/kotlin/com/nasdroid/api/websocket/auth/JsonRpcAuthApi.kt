package com.nasdroid.api.websocket.auth

import com.nasdroid.api.websocket.jsonrpc.JsonRpcWebsocketClient
import com.nasdroid.api.websocket.jsonrpc.MethodCallError
import com.nasdroid.api.websocket.jsonrpc.callMethod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.time.Duration

/**
 * An implementation of [AuthApi] backed by the TrueNAS JSON-RPC 2.0 WebSocket API.
 *
 * Uses `auth.login_ex` (introduced in TrueNAS 25.04 and standard in TrueNAS 26.04+) with automatic
 * fallback to legacy `auth.login` and `auth.login_with_api_key` for older TrueNAS versions.
 */
class JsonRpcAuthApi(
    private val client: JsonRpcWebsocketClient
) : AuthApi {
    override suspend fun checkPassword(username: String, password: String): Boolean {
        return client.callMethod<Boolean, String>(
            method = "auth.check_password",
            params = listOf(username, password),
        )
    }

    override suspend fun checkUser(username: String, password: String): Boolean {
        return client.callMethod<Boolean, String>(
            method = "auth.check_user",
            params = listOf(username, password),
        )
    }

    override suspend fun generateToken(
        timeToLive: Duration,
        attrs: Map<String, Any>,
        matchOrigin: Boolean
    ): String {
        val params = listOf<JsonElement>(
            JsonPrimitive(timeToLive.inWholeSeconds),
            buildJsonObject {
                attrs.forEach { (key, value) ->
                    put(key, JsonPrimitive(value.toString()))
                }
            },
            JsonPrimitive(matchOrigin)
        )
        return client.callMethod(
            method = "auth.generate_token",
            params = params,
        )
    }

    override suspend fun logIn(username: String, password: String, otpToken: String?): Boolean {
        return try {
            val response = client.callMethod<LoginExResult, LoginExPasswordParams>(
                method = "auth.login_ex",
                params = listOf(
                    LoginExPasswordParams(
                        username = username,
                        password = password,
                    )
                )
            )
            when (response.responseType) {
                 RESPONSE_SUCCESS -> true
                 RESPONSE_OTP_REQUIRED -> {
                    if (otpToken == null) {
                        false
                    } else {
                        val otpResponse = client.callMethod<LoginExResult, LoginExOtpParams>(
                            method = "auth.login_ex_continue",
                            params = listOf(LoginExOtpParams(otpToken = otpToken))
                        )
                        otpResponse.responseType == RESPONSE_SUCCESS
                    }
                }
                else -> false
            }
        } catch (_: MethodCallError) {
            val params = listOf(
                JsonPrimitive(username),
                JsonPrimitive(password),
                otpToken?.let { JsonPrimitive(it) } ?: JsonNull
            )
            client.callMethod("auth.login", params)
        }
    }

    override suspend fun logInWithApiKey(apiKey: String, username: String?): Boolean {
        if (!username.isNullOrBlank()) {
            try {
                val response = client.callMethod<LoginExResult, LoginExApiKeyParams>(
                    method = "auth.login_ex",
                    params = listOf(
                        LoginExApiKeyParams(
                            username = username,
                            apiKey = apiKey,
                        )
                    )
                )
                return response.responseType == RESPONSE_SUCCESS
            } catch (_: MethodCallError) {
                // Fall back to legacy auth.login_with_api_key below
            }
        }
        return try {
            client.callMethod("auth.login_with_api_key", listOf(apiKey))
        } catch (_: MethodCallError) {
            // If auth.login_with_api_key was removed (TrueNAS 26/27+) and username wasn't provided,
            // attempt default TrueNAS admin accounts with API_KEY_PLAIN.
            for (candidateUser in listOf("truenas_admin", "admin", "root")) {
                val succeeded = runCatching {
                    val res = client.callMethod<LoginExResult, LoginExApiKeyParams>(
                        method = "auth.login_ex",
                        params = listOf(
                            LoginExApiKeyParams(
                                username = candidateUser,
                                apiKey = apiKey,
                            )
                        )
                    )
                    res.responseType == RESPONSE_SUCCESS
                }.getOrDefault(false)
                if (succeeded) {
                    return true
                }
            }
            false
        }
    }

    override suspend fun logInWithToken(token: String): Boolean {
        return try {
            val response = client.callMethod<LoginExResult, LoginExTokenParams>(
                method = "auth.login_ex",
                params = listOf(LoginExTokenParams(token = token))
            )
            response.responseType == RESPONSE_SUCCESS
        } catch (_: MethodCallError) {
            client.callMethod("auth.login_with_token", listOf(token))
        }
    }

    override suspend fun logOut(): Boolean {
        return client.callMethod("auth.logout")
    }

    override suspend fun me(): AuthenticatedUser {
        return client.callMethod("auth.me")
    }

    override suspend fun sessions(): List<Session> {
        return client.callMethod("auth.sessions")
    }

    override suspend fun setAttribute(key: String, value: String) {
        return client.callMethod("auth.set_attribute", listOf(key, value))
    }

    override suspend fun terminateOtherSessions() {
        return client.callMethod("auth.terminate_other_sessions")
    }

    override suspend fun terminateSession(sessionId: String): Boolean {
        return client.callMethod("auth.terminate_session", listOf(sessionId))
    }

    override suspend fun twoFactorAuth(username: String, password: String): Boolean {
        return client.callMethod<Boolean, String>("auth.two_factor_auth", listOf(username, password))
    }

    companion object {
        private const val RESPONSE_SUCCESS = "SUCCESS"
        private const val RESPONSE_OTP_REQUIRED = "OTP_REQUIRED"
    }
}

@Serializable
internal data class LoginExPasswordParams(
    @SerialName("mechanism")
    val mechanism: String = "PASSWORD_PLAIN",
    @SerialName("username")
    val username: String,
    @SerialName("password")
    val password: String,
)

@Serializable
internal data class LoginExApiKeyParams(
    @SerialName("mechanism")
    val mechanism: String = "API_KEY_PLAIN",
    @SerialName("username")
    val username: String,
    @SerialName("api_key")
    val apiKey: String,
)

@Serializable
internal data class LoginExOtpParams(
    @SerialName("mechanism")
    val mechanism: String = "OTP_TOKEN",
    @SerialName("otp_token")
    val otpToken: String,
)

@Serializable
internal data class LoginExTokenParams(
    @SerialName("mechanism")
    val mechanism: String = "TOKEN_PLAIN",
    @SerialName("token")
    val token: String,
)

@Serializable
internal data class LoginExResult(
    @SerialName("response_type")
    val responseType: String,
)
