package org.radarbase.upload.resource

import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.Application
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ContextResolver
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.BufferedSink
import org.glassfish.jersey.test.JerseyTest
import org.glassfish.jersey.test.TestProperties
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.radarbase.auth.token.RadarToken
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.config.ConfigLoader
import org.radarbase.jersey.enhancer.MapperResourceEnhancer
import org.radarbase.jersey.hibernate.config.DatabaseConfig
import org.radarbase.upload.Config
import org.radarbase.upload.api.RecordDTO
import org.radarbase.upload.api.RecordDataDTO
import org.radarbase.upload.api.SourceTypeDTO
import org.radarbase.upload.doa.entity.*
import org.radarbase.upload.mock.MockResourceEnhancerFactory
import java.io.IOException
import java.net.URI
import java.nio.file.Path
import java.util.*
import kotlin.reflect.jvm.jvmName

internal class RecordResourceTest: JerseyTest() {
    lateinit var config: Config

    lateinit var authQueue: Queue<Pair<Auth?, Boolean>>
    lateinit var auth: Auth

    @BeforeEach
    fun doSetup() {
        assertThat(client, not(nullValue()))
        client.register(ContextResolver {
            MapperResourceEnhancer().mapper
        })
    }

    @AfterEach
    fun doTeardown() {
        super.tearDown()
    }

    override fun configure(): Application {
        config = Config(
            baseUri = URI.create("http://localhost:10313/upload/api/"),
            sourceTypes = listOf(
                SourceTypeDTO("type1", null, null, null, null, null, null)
            ),
        )
        authQueue = ArrayDeque()
        val radarToken = mock<RadarToken> {
            on { hasPermissionOnProject(anyOrNull(), anyOrNull()) } doReturn true
            on { hasPermissionOnSubject(anyOrNull(), anyOrNull(), anyOrNull()) } doReturn true
            on { hasPermissionOnSource(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()) } doReturn true
            on { hasPermission(anyOrNull()) } doReturn true
        }

        auth = mock {
            on { clientId } doReturn "radar_upload_frontend"
            on { defaultProject } doReturn "a"
            on { userId } doReturn "u1"
            on { token } doReturn radarToken
        }
        val projects = mapOf(
            "a" to listOf("u1", "u2"),
            "b" to listOf("u3"),
        )

        val databaseConfig = DatabaseConfig(
            managedClasses = listOf(
                Record::class.jvmName,
                RecordMetadata::class.jvmName,
                RecordLogs::class.jvmName,
                RecordContent::class.jvmName,
                SourceType::class.jvmName,
            ),
            url = "jdbc:hsqldb:mem:test3;DB_CLOSE_DELAY=-1",
            driver = "org.hsqldb.jdbc.JDBCDriver",
            dialect = "org.hibernate.dialect.HSQLDialect",
        )
        val enhancerFactory = MockResourceEnhancerFactory(config, authQueue, projects, databaseConfig)
        return ConfigLoader.createResourceConfig(enhancerFactory.createEnhancers()).apply {

        }
    }

    @Test
    fun create() {
        target("health")
            .request()
            .get()
            .use { response ->
                assertThat(response.status, equalTo(Response.Status.OK.statusCode))
            }

        authQueue.add(Pair(auth, true))
        val record = target("records")
            .request()
            .header("Authorization", "Bearer abcdef")
            .post(Entity.json(RecordDTO(
                id = null,
                data = RecordDataDTO(
                    projectId = "a",
                    userId = "u1",
                    sourceId = "s1",
                ),
                sourceType = "type1",
                metadata = null,
            ))).use { response ->
                response.readEntity(RecordDTO::class.java)
            }

        val okClient = OkHttpClient()
        val request = okhttp3.Request.Builder().apply {
            url(baseUri.resolve("records/${record.id}/contents/test2.jpg").toURL())
            header("Authorization", "Bearer abcdef")
            header("Content-Type", "image/jpeg")
            header("Content-Length", "100")
            put(object : RequestBody() {
                override fun contentType() = "text/plain".toMediaType()
                override fun writeTo(sink: BufferedSink) {
                    repeat(100) {
                        sink.write(ByteArray(64_000))
                        sink.flush()
                        Thread.sleep(100)
                    }
                }
            })
        }.build()
        val call = okClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                assertThat("Should fail", true)
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                assertThat("Should fail", false)
            }
        })
        Thread.sleep(200)
        call.cancel()

        authQueue.add(Pair(auth, true))
        target("records/${record.id}/contents/test2.jpg")
            .request()
            .header("Authorization", "Bearer abcdef")
            .put(Entity.text("something")).use { response ->
                assertThat(response.statusInfo.family, `is`(Response.Status.Family.SUCCESSFUL))
            }
    }
}
