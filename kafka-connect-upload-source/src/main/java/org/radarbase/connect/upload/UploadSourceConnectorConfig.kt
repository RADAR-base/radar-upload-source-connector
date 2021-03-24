/*
 *
 *  * Copyright 2019 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.radarbase.connect.upload

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import org.apache.kafka.common.config.AbstractConfig
import org.apache.kafka.common.config.ConfigDef
import org.radarbase.connect.upload.auth.ClientCredentialsAuthorizer
import org.radarbase.connect.upload.converter.altoida.AltoidaConverterFactory
import org.radarbase.connect.upload.converter.axivity.AxivityConverterFactory
import org.radarbase.connect.upload.converter.gaitup.Physilog5ConverterFactory
import org.radarbase.connect.upload.converter.oxford.WearableCameraConverterFactory
import org.radarbase.connect.upload.converter.phone.AcceleratometerZipConverterFactory
import org.radarbase.connect.upload.converter.phone.AccelerometerConverterFactory
import org.radarbase.connect.upload.io.FileUploaderFactory
import org.radarbase.connect.upload.io.UploadType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.TimeUnit

class UploadSourceConnectorConfig(config: ConfigDef, parsedConfig: Map<String, String>) :
    AbstractConfig(config, parsedConfig) {

    private lateinit var authenticator: Authenticator

    fun getAuthenticator(): Authenticator {
        if(!::authenticator.isInitialized) {
            logger.info("Initializing authenticator")
            authenticator = ClientCredentialsAuthorizer(
                httpClient,
                oauthClientId,
                oauthClientSecret,
                tokenRequestUrl)
        }
        return authenticator
    }

    constructor(parsedConfig: Map<String, String>) : this(conf(), parsedConfig)

    val oauthClientId: String = getString(UPLOAD_SOURCE_CLIENT_CONFIG)

    val oauthClientSecret: String = getString(UPLOAD_SOURCE_MP_SECRET_CONFIG)

    val tokenRequestUrl: String = getString(UPLOAD_SOURCE_CLIENT_TOKEN_URL_CONFIG)

    val uploadBackendBaseUrl: String = getString(UPLOAD_SOURCE_SERVER_BASE_URL_CONFIG)

    val converterClasses: List<String> = getList(UPLOAD_SOURCE_CONVERTERS_CONFIG)

    val fileUploaderType: UploadType? = UploadType.valueOf(getString(UPLOAD_FILE_UPLOADER_TYPE_CONFIG).toUpperCase())

    val fileUploadConfig: FileUploaderFactory.FileUploaderConfig = FileUploaderFactory.FileUploaderConfig(
        targetEndpoint = getString(UPLOAD_FILE_UPLOADER_TARGET_ENDPOINT_URL_CONFIG),
        targetRoot = getString(UPLOAD_FILE_UPLOADER_TARGET_ROOT_DIRECTORY_CONFIG),
        username = getString(UPLOAD_FILE_UPLOADER_USERNAME_CONFIG),
        password = getPassword(UPLOAD_FILE_UPLOADER_PASSWORD_CONFIG)?.value(),
        sshPrivateKey = getString(UPLOAD_FILE_UPLOADER_SSH_PRIVATE_KEY_FILE_CONFIG),
        sshPassPhrase = getPassword(UPLOAD_FILE_UPLOADER_SSH_PASSPHRASE_CONFIG)?.value()
    )

    val httpClient: OkHttpClient
        get() = globalHttpClient

    companion object {

        val logger: Logger = LoggerFactory.getLogger(UploadSourceConnectorConfig::class.java)

        const val UPLOAD_SOURCE_CLIENT_CONFIG = "upload.source.client.id"
        private const val UPLOAD_SOURCE_CLIENT_DOC = "Client ID for the for the upload connector"
        private const val UPLOAD_SOURCE_MP_CLIENT_DISPLAY = "Upload connector client ID"
        private const val UPLOAD_SOURCE_MP_CLIENT_DEFAULT = "radar-upload-connector-client"

        const val UPLOAD_SOURCE_MP_SECRET_CONFIG = "upload.source.client.secret"
        private const val UPLOAD_SOURCE_MP_SECRET_DOC = "Secret for the Upload connector client set in upload.source.mp.client."
        private const val UPLOAD_SOURCE_MP_SECRET_DISPLAY = "Upload connector client secret"

        const val UPLOAD_SOURCE_CLIENT_TOKEN_URL_CONFIG = "upload.source.client.tokenUrl"
        private const val UPLOAD_SOURCE_CLIENT_TOKEN_URL_DOC = "Complete Token URL to get access token"
        private const val UPLOAD_SOURCE_CLIENT_TOKEN_URL_DISPLAY = "Token URL to get access token"
        private const val UPLOAD_SOURCE_CLIENT_TOKEN_URL_DEFAULT = "http://managementportal-app:8080/managementportal/oauth/token"

        const val UPLOAD_SOURCE_SERVER_BASE_URL_CONFIG = "upload.source.backend.baseUrl"
        private const val UPLOAD_SOURCE_SERVER_BASE_URL_DOC = "Base URL of the file upload backend"
        private const val UPLOAD_SOURCE_SERVER_BASE_URL_DISPLAY = "Base URL of the file upload backend"
        private const val UPLOAD_SOURCE_SERVER_BASE_URL_DEFAULT = "http://radar-upload-connect-backend:8085/radar-upload/"

        const val UPLOAD_SOURCE_CONVERTERS_CONFIG = "upload.source.record.converter.classes"
        private const val UPLOAD_SOURCE_CONVERTERS_DOC = "List record converter classes that are in class-path"
        private const val UPLOAD_SOURCE_CONVERTERS_DISPLAY = "List of record converter factory class"
        private val UPLOAD_SOURCE_CONVERTERS_DEFAULT: List<String> = listOf(
            AccelerometerConverterFactory::class.java.name,
            AcceleratometerZipConverterFactory::class.java.name,
            AltoidaConverterFactory::class.java.name,
            WearableCameraConverterFactory::class.java.name,
            AxivityConverterFactory::class.java.name,
            Physilog5ConverterFactory::class.java.name)

        const val SOURCE_POLL_INTERVAL_CONFIG = "upload.source.poll.interval.ms"
        private const val SOURCE_POLL_INTERVAL_DOC = "How often to poll the source URL."
        private const val SOURCE_POLL_INTERVAL_DISPLAY = "Polling interval"
        private const val SOURCE_POLL_INTERVAL_DEFAULT = 60000L


        const val SOURCE_QUEUE_SIZE_CONFIG = "upload.source.queue.size"
        private const val SOURCE_QUEUE_SIZE_DOC = "Capacity of the records queue."
        private const val SOURCE_QUEUE_SIZE_DISPLAY = "Records queue size"
        private const val SOURCE_QUEUE_SIZE_DEFAULT = 1000

        private const val UPLOAD_FILE_UPLOADER_TYPE_CONFIG = "upload.source.file.uploader.type"
        private const val UPLOAD_FILE_UPLOADER_TYPE_DOC = "Choose which type of file uploader should be used to upload files to target location from local, sftp, s3."
        private const val UPLOAD_FILE_UPLOADER_TYPE_DISPLAY = "File uploader type"
        private val UPLOAD_FILE_UPLOADER_TYPE_DEFAULT: String = "s3"

        private const val UPLOAD_FILE_UPLOADER_TARGET_ENDPOINT_URL_CONFIG = "upload.source.file.uploader.target.endpoint"
        private const val UPLOAD_FILE_UPLOADER_TARGET_ENDPOINT_URL_DOC = "Advertised URL Endpoint of the file upload target."
        private const val UPLOAD_FILE_UPLOADER_TARGET_ENDPOINT_URL_DISPLAY = "File upload target endpoint"
        private val UPLOAD_FILE_UPLOADER_TARGET_ENDPOINT_URL_DEFAULT: String = "http://minio:9000/"

        private const val UPLOAD_FILE_UPLOADER_TARGET_ROOT_DIRECTORY_CONFIG = "upload.source.file.uploader.target.root.directory"
        private const val UPLOAD_FILE_UPLOADER_TARGET_ROOT_DIRECTORY_DOC = "Target root directory or s3 bucket where files should be uploaded to."
        private const val UPLOAD_FILE_UPLOADER_TARGET_ROOT_DIRECTORY_DISPLAY = "Root directory/bucket to upload files"
        private val UPLOAD_FILE_UPLOADER_TARGET_ROOT_DIRECTORY_DEFAULT: String = "radar-output-storage"

        private const val UPLOAD_FILE_UPLOADER_USERNAME_CONFIG = "upload.source.file.uploader.username"
        private const val UPLOAD_FILE_UPLOADER_USERNAME_DOC = "Username to upload files to the target."
        private const val UPLOAD_FILE_UPLOADER_USERNAME_DISPLAY = "File Uploader username"
        private val UPLOAD_FILE_UPLOADER_USERNAME_DEFAULT: String? = null

        private const val UPLOAD_FILE_UPLOADER_PASSWORD_CONFIG = "upload.source.file.uploader.password"
        private const val UPLOAD_FILE_UPLOADER_PASSWORD_DOC = "Password to upload files to the target."
        private const val UPLOAD_FILE_UPLOADER_PASSWORD_DISPLAY = "File Uploader password"
        private val UPLOAD_FILE_UPLOADER_PASSWORD_DEFAULT: String? = null

        private const val UPLOAD_FILE_UPLOADER_SSH_PRIVATE_KEY_FILE_CONFIG = "upload.source.file.uploader.sftp.private.key.file"
        private const val UPLOAD_FILE_UPLOADER_SSH_PRIVATE_KEY_FILE_DOC = "Path of private-key file if using private key for uploading files using sftp."
        private const val UPLOAD_FILE_UPLOADER_SSH_PRIVATE_KEY_FILE_DISPLAY = "Sftp private key file path."
        private val UPLOAD_FILE_UPLOADER_SSH_PRIVATE_KEY_FILE_DEFAULT: String? = null

        private const val UPLOAD_FILE_UPLOADER_SSH_PASSPHRASE_CONFIG = "upload.source.file.uploader.sftp.passphrase"
        private const val UPLOAD_FILE_UPLOADER_SSH_PASSPHRASE_DOC = "Passphrase of the private-key file if using private key for uploading files using sftp."
        private const val UPLOAD_FILE_UPLOADER_SSH_PASSPHRASE_DISPLAY = "Pass phrase for private key."
        private val UPLOAD_FILE_UPLOADER_SSH_PASSPHRASE_DEFAULT: String? = null

        var mapper: ObjectMapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        fun conf(): ConfigDef {
            val groupName = "upload"
            var orderInGroup = 0

            return ConfigDef()
                .define(SOURCE_POLL_INTERVAL_CONFIG,
                    ConfigDef.Type.LONG,
                    SOURCE_POLL_INTERVAL_DEFAULT,
                    ConfigDef.Importance.HIGH,
                    SOURCE_POLL_INTERVAL_DOC,
                    groupName,
                    ++orderInGroup,
                    ConfigDef.Width.SHORT,
                    SOURCE_POLL_INTERVAL_DISPLAY)

                .define(SOURCE_QUEUE_SIZE_CONFIG,
                    ConfigDef.Type.INT,
                    SOURCE_QUEUE_SIZE_DEFAULT,
                    ConfigDef.Importance.HIGH,
                    SOURCE_QUEUE_SIZE_DOC,
                    groupName,
                    ++orderInGroup,
                    ConfigDef.Width.SHORT,
                    SOURCE_QUEUE_SIZE_DISPLAY)

                .define(UPLOAD_SOURCE_CLIENT_CONFIG,
                    ConfigDef.Type.STRING,
                    UPLOAD_SOURCE_MP_CLIENT_DEFAULT,
                    ConfigDef.Importance.HIGH,
                    UPLOAD_SOURCE_CLIENT_DOC,
                    groupName,
                    ++orderInGroup,
                    ConfigDef.Width.SHORT,
                    UPLOAD_SOURCE_MP_CLIENT_DISPLAY)

                .define(UPLOAD_SOURCE_MP_SECRET_CONFIG,
                    ConfigDef.Type.STRING,
                    "",
                    ConfigDef.Importance.HIGH,
                    UPLOAD_SOURCE_MP_SECRET_DOC,
                    groupName,
                    ++orderInGroup,
                    ConfigDef.Width.SHORT,
                    UPLOAD_SOURCE_MP_SECRET_DISPLAY)

                .define(UPLOAD_SOURCE_SERVER_BASE_URL_CONFIG,
                    ConfigDef.Type.STRING,
                    UPLOAD_SOURCE_SERVER_BASE_URL_DEFAULT,
                    ConfigDef.Importance.LOW,
                    UPLOAD_SOURCE_SERVER_BASE_URL_DOC,
                    groupName,
                    ++orderInGroup,
                    ConfigDef.Width.SHORT,
                    UPLOAD_SOURCE_SERVER_BASE_URL_DISPLAY)

                .define(UPLOAD_SOURCE_CLIENT_TOKEN_URL_CONFIG,
                    ConfigDef.Type.STRING,
                    UPLOAD_SOURCE_CLIENT_TOKEN_URL_DEFAULT,
                    ConfigDef.Importance.LOW,
                    UPLOAD_SOURCE_CLIENT_TOKEN_URL_DOC,
                    groupName,
                    ++orderInGroup,
                    ConfigDef.Width.SHORT,
                    UPLOAD_SOURCE_CLIENT_TOKEN_URL_DISPLAY)

                .define(UPLOAD_SOURCE_CONVERTERS_CONFIG,
                    ConfigDef.Type.LIST,
                    UPLOAD_SOURCE_CONVERTERS_DEFAULT,
                    ConfigDef.Importance.HIGH,
                    UPLOAD_SOURCE_CONVERTERS_DOC,
                    groupName,
                    ++orderInGroup,
                    ConfigDef.Width.LONG,
                    UPLOAD_SOURCE_CONVERTERS_DISPLAY)

                .define(UPLOAD_FILE_UPLOADER_TYPE_CONFIG,
                    ConfigDef.Type.STRING,
                    UPLOAD_FILE_UPLOADER_TYPE_DEFAULT,
                    ConfigDef.Importance.LOW,
                    UPLOAD_FILE_UPLOADER_TYPE_DOC,
                    groupName,
                    ++orderInGroup,
                    ConfigDef.Width.LONG,
                    UPLOAD_FILE_UPLOADER_TYPE_DISPLAY)

                .define(UPLOAD_FILE_UPLOADER_TARGET_ENDPOINT_URL_CONFIG,
                    ConfigDef.Type.STRING,
                    UPLOAD_FILE_UPLOADER_TARGET_ENDPOINT_URL_DEFAULT,
                    ConfigDef.Importance.LOW,
                    UPLOAD_FILE_UPLOADER_TARGET_ENDPOINT_URL_DOC,
                    groupName,
                    ++orderInGroup,
                    ConfigDef.Width.MEDIUM,
                    UPLOAD_FILE_UPLOADER_TARGET_ENDPOINT_URL_DISPLAY)

                .define(UPLOAD_FILE_UPLOADER_TARGET_ROOT_DIRECTORY_CONFIG,
                    ConfigDef.Type.STRING,
                    UPLOAD_FILE_UPLOADER_TARGET_ROOT_DIRECTORY_DEFAULT,
                    ConfigDef.Importance.LOW,
                    UPLOAD_FILE_UPLOADER_TARGET_ROOT_DIRECTORY_DOC,
                    groupName,
                    ++orderInGroup,
                    ConfigDef.Width.SHORT,
                    UPLOAD_FILE_UPLOADER_TARGET_ROOT_DIRECTORY_DISPLAY)

                .define(UPLOAD_FILE_UPLOADER_USERNAME_CONFIG,
                    ConfigDef.Type.STRING,
                    UPLOAD_FILE_UPLOADER_USERNAME_DEFAULT,
                    ConfigDef.Importance.LOW,
                    UPLOAD_FILE_UPLOADER_USERNAME_DOC,
                    groupName,
                    ++orderInGroup,
                    ConfigDef.Width.MEDIUM,
                    UPLOAD_FILE_UPLOADER_USERNAME_DISPLAY)

                .define(UPLOAD_FILE_UPLOADER_PASSWORD_CONFIG,
                    ConfigDef.Type.PASSWORD,
                    UPLOAD_FILE_UPLOADER_PASSWORD_DEFAULT,
                    ConfigDef.Importance.LOW,
                    UPLOAD_FILE_UPLOADER_PASSWORD_DOC,
                    groupName,
                    ++orderInGroup,
                    ConfigDef.Width.MEDIUM,
                    UPLOAD_FILE_UPLOADER_PASSWORD_DISPLAY)

                .define(UPLOAD_FILE_UPLOADER_SSH_PRIVATE_KEY_FILE_CONFIG,
                    ConfigDef.Type.STRING,
                    UPLOAD_FILE_UPLOADER_SSH_PRIVATE_KEY_FILE_DEFAULT,
                    ConfigDef.Importance.LOW,
                    UPLOAD_FILE_UPLOADER_SSH_PRIVATE_KEY_FILE_DOC,
                    groupName,
                    ++orderInGroup,
                    ConfigDef.Width.MEDIUM,
                    UPLOAD_FILE_UPLOADER_SSH_PRIVATE_KEY_FILE_DISPLAY)

                .define(UPLOAD_FILE_UPLOADER_SSH_PASSPHRASE_CONFIG,
                    ConfigDef.Type.PASSWORD,
                    UPLOAD_FILE_UPLOADER_SSH_PASSPHRASE_DEFAULT,
                    ConfigDef.Importance.LOW,
                    UPLOAD_FILE_UPLOADER_SSH_PASSPHRASE_DOC,
                    groupName,
                    ++orderInGroup,
                    ConfigDef.Width.MEDIUM,
                    UPLOAD_FILE_UPLOADER_SSH_PASSPHRASE_DISPLAY)

        }

        private val globalHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
