package org.radarbase.connect.upload

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.OkHttpClient
import org.apache.kafka.common.config.AbstractConfig
import org.apache.kafka.common.config.ConfigDef
import org.radarbase.connect.upload.auth.Authorizer
import org.radarbase.connect.upload.auth.ClientCredentialsAuthorizer
import org.radarbase.connect.upload.converter.AccelerometerCsvRecordConverter
import java.util.concurrent.TimeUnit

class UploadSourceConnectorConfig(config: ConfigDef, parsedConfig: Map<String, String>) :
        AbstractConfig(config, parsedConfig) {

    constructor(parsedConfig: Map<String, String>) : this(conf(), parsedConfig) {}

    fun getHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()


    fun getAuthorizer(): Authorizer {
        return ClientCredentialsAuthorizer(
                getHttpClient(),
                getString(UPLOAD_SOURCE_CLIENT_CONFIG),
                getString(UPLOAD_SOURCE_MP_SECRET_CONFIG),
                getString(UPLOAD_SOURCE_CLIENT_TOKEN_URL_CONFIG),
                getString(UPLOAD_SOURCE_CLIENT_SCOPE_CONFIG)?.split(",")?.toSet()
        )
    }

    fun getBaseUrl(): String = getString(UPLOAD_SOURCE_SERVER_BASE_URL_CONFIG)

    fun getConverterClasses(): List<String> = getList(UPLOAD_SOURCE_CONVERTERS_CONFIG)

    companion object {

        const val UPLOAD_SOURCE_CLIENT_CONFIG = "upload.source.client.id"
        private const val UPLOAD_SOURCE_CLIENT_DOC = "Client ID for the for the upload connector"
        private const val UPLOAD_SOURCE_MP_CLIENT_DISPLAY = "Upload connector client ID"
        private const val UPLOAD_SOURCE_MP_CLIENT_DEFAULT = "radar-upload-connector-client"

        const val UPLOAD_SOURCE_MP_SECRET_CONFIG = "upload.source.client.secret"
        private const val UPLOAD_SOURCE_MP_SECRET_DOC = "Secret for the Upload connector client set in upload.source.mp.client."
        private const val UPLOAD_SOURCE_MP_SECRET_DISPLAY = "Upload connector client secret"

        const val UPLOAD_SOURCE_CLIENT_SCOPE_CONFIG = "upload.source.client.scopes"
        private const val UPLOAD_SOURCE_CLIENT_SCOPE_DOC = "Scopes of the upload connector client as comma separated values"
        private const val UPLOAD_SOURCE_CLIENT_SCOPE_DISPLAY = "Scopes of upload connector"
        private const val UPLOAD_SOURCE_CLIENT_SCOPE_DEFAULT = "MEASUREMENT.CREATE"

        const val UPLOAD_SOURCE_CLIENT_TOKEN_URL_CONFIG = "upload.source.client.tokenUrl"
        private const val UPLOAD_SOURCE_CLIENT_TOKEN_URL_DOC = "Complete Token URL to get access token"
        private const val UPLOAD_SOURCE_CLIENT_TOKEN_URL_DISPLAY = "Token URL to get access token"
        private const val UPLOAD_SOURCE_CLIENT_TOKEN_URL_DEFAULT = "http://managementportal-app:8080/managementportal/oauth/token"

        const val UPLOAD_SOURCE_SERVER_BASE_URL_CONFIG = "upload.source.backend.baseUrl"
        private const val UPLOAD_SOURCE_SERVER_BASE_URL_DOC = "Base URL of the file upload backend"
        private const val UPLOAD_SOURCE_SERVER_BASE_URL_DISPLAY = "Base URL of the file upload backend"
        private const val UPLOAD_SOURCE_SERVER_BASE_URL_DEFAULT = "http://radar-upload-backend:8080/"

        const val SOURCE_POLL_INTERVAL_CONFIG = "upload.source.poll.interval.ms"
        private const val SOURCE_POLL_INTERVAL_DOC = "How often to poll the source URL."
        private const val SOURCE_POLL_INTERVAL_DISPLAY = "Polling interval"
        private const val SOURCE_POLL_INTERVAL_DEFAULT = 60000L

        const val UPLOAD_SOURCE_CONVERTERS_CONFIG = "upload.source.record.converter.classes"
        private const val UPLOAD_SOURCE_CONVERTERS_DOC = "List record converter classes that are in class-path"
        private const val UPLOAD_SOURCE_CONVERTERS_DISPLAY = "List of record converter class"
        private val UPLOAD_SOURCE_CONVERTERS_DEFAULT = listOf(AccelerometerCsvRecordConverter())


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
                            ConfigDef.Importance.LOW,
                            SOURCE_POLL_INTERVAL_DOC,
                            groupName,
                            ++orderInGroup,
                            ConfigDef.Width.SHORT,
                            SOURCE_POLL_INTERVAL_DISPLAY)

                    .define(UPLOAD_SOURCE_CLIENT_CONFIG,
                            ConfigDef.Type.STRING,
                            UPLOAD_SOURCE_MP_CLIENT_DEFAULT,
                            ConfigDef.Importance.LOW,
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
                            ConfigDef.Importance.HIGH,
                            UPLOAD_SOURCE_SERVER_BASE_URL_DOC,
                            groupName,
                            ++orderInGroup,
                            ConfigDef.Width.SHORT,
                            UPLOAD_SOURCE_SERVER_BASE_URL_DISPLAY)

                    .define(UPLOAD_SOURCE_CLIENT_SCOPE_CONFIG,
                            ConfigDef.Type.STRING,
                            UPLOAD_SOURCE_CLIENT_SCOPE_DEFAULT,
                            ConfigDef.Importance.HIGH,
                            UPLOAD_SOURCE_CLIENT_SCOPE_DOC,
                            groupName,
                            ++orderInGroup,
                            ConfigDef.Width.SHORT,
                            UPLOAD_SOURCE_CLIENT_SCOPE_DISPLAY)

                    .define(UPLOAD_SOURCE_CLIENT_TOKEN_URL_CONFIG,
                            ConfigDef.Type.STRING,
                            UPLOAD_SOURCE_CLIENT_TOKEN_URL_DEFAULT,
                            ConfigDef.Importance.HIGH,
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
        }
    }

}
