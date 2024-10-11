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
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.radarbase.auth.authorization.AuthorityReference
import org.radarbase.auth.authorization.Permission
import org.radarbase.auth.authorization.RoleAuthority.PARTICIPANT
import org.radarbase.auth.authorization.RoleAuthority.SYS_ADMIN
import org.radarbase.auth.token.DataRadarToken
import org.radarbase.auth.token.RadarToken
import org.radarbase.jersey.config.ConfigLoader
import org.radarbase.jersey.enhancer.MapperResourceEnhancer
import org.radarbase.jersey.hibernate.config.DatabaseConfig
import org.radarbase.upload.Config
import org.radarbase.upload.api.RecordDTO
import org.radarbase.upload.api.RecordDataDTO
import org.radarbase.upload.api.SourceTypeDTO
import org.radarbase.upload.doa.entity.RecordContent
import org.radarbase.upload.doa.entity.RecordLogs
import org.radarbase.upload.doa.entity.RecordMetadata
import org.radarbase.upload.doa.entity.SourceType
import org.radarbase.upload.mock.MockResourceEnhancerFactory
import java.io.IOException
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.collections.HashSet
import kotlin.reflect.jvm.jvmName

internal class RecordResourceTest : JerseyTest() {
    lateinit var config: Config

    lateinit var authQueue: Queue<Pair<RadarToken?, Boolean>>
    lateinit var auth: RadarToken

    @BeforeEach
    fun doSetup() {
        assertThat(client, not(nullValue()))
        client.register(
            ContextResolver {
                MapperResourceEnhancer().mapper
            },
        )
    }

    @AfterEach
    fun doTeardown() {
        super.tearDown()
    }

    override fun configure(): Application {
        config = Config(
            baseUri = URI.create("http://localhost:10313/upload/api/"),
            sourceTypes = listOf(
                SourceTypeDTO("type1", null, null, null, null, null, null),
            ),
        )
        authQueue = ArrayDeque()
        auth = DataRadarToken(
            roles = setOf(AuthorityReference(SYS_ADMIN), AuthorityReference(PARTICIPANT, "a")),
            scopes = Permission.entries.mapTo(HashSet()) { it.scope() },
            clientId = "radar_upload_frontend",
            username = "u1",
            subject = "u1",
            expiresAt = Instant.now() + Duration.ofMinutes(1),
            audience = listOf("res_upload"),
            grantType = "authorization_code",
        )
        val projects = mapOf(
            "a" to listOf("u1", "u2"),
            "b" to listOf("u3"),
        )

        val databaseConfig = DatabaseConfig(
            managedClasses = listOf(
                org.radarbase.upload.doa.entity.Record::class.jvmName,
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
            .post(
                Entity.json(
                    RecordDTO(
                        id = null,
                        data = RecordDataDTO(
                            projectId = "a",
                            userId = "u1",
                            sourceId = "s1",
                        ),
                        sourceType = "type1",
                        metadata = null,
                    ),
                ),
            ).use { response ->
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
