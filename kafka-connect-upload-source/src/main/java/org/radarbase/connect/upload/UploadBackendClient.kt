package org.radarbase.connect.upload

import okhttp3.OkHttpClient

class UploadBackendClient(
        private val auth: Authorizer,
        private val httpClient: OkHttpClient) {

    // TODO: implement stubs for API calls to upload backend
    fun pollRecords(): List<Any> {
        return emptyList()
    }

    fun requestConnectorConfig(name: String) {

    }

    fun requestAllConnectors(): List<Any> {
        return emptyList()
    }

    fun retrieveFile() {

    }

    fun updateStatus() {

    }
}
