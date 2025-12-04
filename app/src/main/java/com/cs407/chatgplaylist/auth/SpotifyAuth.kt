package com.cs407.chatgplaylist.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.CodeVerifierUtil
import net.openid.appauth.ResponseTypeValues

object SpotifyAuth {
    private const val AUTH_BASE = "https://accounts.spotify.com"
    private const val AUTH_ENDPOINT = "$AUTH_BASE/authorize"
    private const val TOKEN_ENDPOINT = "$AUTH_BASE/api/token"
    private const val SCOPE =
        "user-read-email user-read-private playlist-modify-private playlist-modify-public user-read-recently-played"

    sealed class Result {
        data class Success(val accessToken: String, val refreshToken: String?) : Result()
        data class Error(val message: String) : Result()
    }

    private var authService: AuthorizationService? = null
    private var authState: AuthState? = null
    private var currentRequest: AuthorizationRequest? = null
    private var lastAccessToken: String? = null
    private var lastRefreshToken: String? = null

    // Launch web authorization with PKCE
    fun startSignIn(activity: Activity, clientId: String, redirectUri: String) {
        if (authService == null) authService = AuthorizationService(activity)

        val cfg = AuthorizationServiceConfiguration(
            Uri.parse(AUTH_ENDPOINT),
            Uri.parse(TOKEN_ENDPOINT)
        )
        if (authState == null) {
            authState = AuthState(cfg)
        }

        val verifier = CodeVerifierUtil.generateRandomCodeVerifier()
        val challenge = CodeVerifierUtil.deriveCodeVerifierChallenge(verifier)

        val request = AuthorizationRequest.Builder(
            cfg,
            clientId,
            ResponseTypeValues.CODE,
            Uri.parse(redirectUri)
        )
            .setScope(SCOPE)
            .setCodeVerifier(
                verifier,
                challenge,
                AuthorizationRequest.CODE_CHALLENGE_METHOD_S256
            )
            .build()

        currentRequest = request
        authService
            ?.getAuthorizationRequestIntent(request)
            ?.let { activity.startActivity(it) }
    }

    // Return true if back on app
    fun isSpotifyRedirect(intent: Intent): Boolean {
        val d = intent.data ?: return false
        return d.scheme == "chatgplaylist" && d.host == "callback"
    }

    fun handleRedirect(intent: Intent, callback: (Result) -> Unit) {
        val dataUri = intent.data
        val response = AuthorizationResponse.fromIntent(intent)
            ?: buildResponseFromUri(dataUri)
        val authException = AuthorizationException.fromIntent(intent)
            ?: buildExceptionFromUri(dataUri)
        val state = authState
        if (state == null) {
            callback(Result.Error("Auth state unavailable, please restart sign in."))
            return
        }

        if (response == null && authException == null) {
            callback(Result.Error("Missing authorization response."))
            return
        }

        state.update(response, authException)
        currentRequest = null

        if (authException != null) {
            callback(
                Result.Error(authException.errorDescription ?: "Authorization failed.")
            )
            return
        }

        if (response?.authorizationCode.isNullOrEmpty()) {
            callback(Result.Error("Authorization canceled before completion."))
            return
        }

        val tokenRequest = response.createTokenExchangeRequest()

        val service = authService
        if (service == null) {
            callback(Result.Error("Authorization service unavailable."))
            return
        }

        service.performTokenRequest(tokenRequest) { tokenResponse, tokenException ->
            state.update(tokenResponse, tokenException)
            when {
                tokenResponse != null -> {
                    lastAccessToken = tokenResponse.accessToken
                    lastRefreshToken = tokenResponse.refreshToken
                    callback(
                        Result.Success(
                            tokenResponse.accessToken.orEmpty(),
                            tokenResponse.refreshToken
                        )
                    )
                }
                else -> callback(
                    Result.Error(
                        tokenException?.errorDescription ?: "Token exchange failed."
                    )
                )
            }
        }
    }

    fun dispose() {
        authService?.dispose()
        authService = null
    }

    fun currentAccessToken(): String? = lastAccessToken
    fun currentRefreshToken(): String? = lastRefreshToken

    private fun buildResponseFromUri(uri: Uri?): AuthorizationResponse? {
        val request = currentRequest ?: return null
        val safeUri = uri ?: return null
        return try {
            AuthorizationResponse.Builder(request)
                .fromUri(safeUri)
                .build()
        } catch (ex: IllegalArgumentException) {
            null
        }
    }

    private fun buildExceptionFromUri(uri: Uri?): AuthorizationException? {
        uri ?: return null
        val hasError = uri.getQueryParameter(AuthorizationException.PARAM_ERROR) != null
        return if (hasError) AuthorizationException.fromOAuthRedirect(uri) else null
    }
}
