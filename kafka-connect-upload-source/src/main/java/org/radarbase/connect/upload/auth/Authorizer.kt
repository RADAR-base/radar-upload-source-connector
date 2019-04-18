package org.radarbase.connect.upload.auth

import okhttp3.OkHttpClient

interface Authorizer {
    fun accessToken(forceRefresh: Boolean = false): String
}

class ClientCredentialsAuthorizer(
        private val httpClient: OkHttpClient,
        private val clientId: String,
        private val clientSecret: String,
        private val resourceName: String,
        private val scopes: List<String>): Authorizer {


    override fun accessToken(forceRefresh: Boolean): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
