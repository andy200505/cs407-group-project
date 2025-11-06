package com.cs407.chatgplaylist.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

object SpotifyAuth {
    private const val AUTH_BASE = "https://accounts.spotify.com"
    private const val AUTH_ENDPOINT = "$AUTH_BASE/authorize"
    private const val TOKEN_ENDPOINT = "$AUTH_BASE/api/token"
    private const val SCOPE =
        "user-read-email playlist-modify-private playlist-modify-public"

    private var authService: AuthorizationService? = null

    //Launch web authorization
    fun startSignIn(activity: Activity, clientId: String, redirectUri: String) {
        if (authService == null) authService = AuthorizationService(activity)

        val cfg = AuthorizationServiceConfiguration(
            Uri.parse(AUTH_ENDPOINT),
            Uri.parse(TOKEN_ENDPOINT)
        )

        val request = AuthorizationRequest.Builder(
            cfg,
            clientId,
            ResponseTypeValues.CODE,
            Uri.parse(redirectUri)
        )
            .setScope(SCOPE)
            .build()

        activity.startActivity(authService?.getAuthorizationRequestIntent(request))
    }

    //Return true if back on app
    fun isSpotifyRedirect(intent: Intent): Boolean {
        val d = intent.data ?: return false
        return d.scheme == "chatgplaylist" && d.host == "callback"
    }
}
