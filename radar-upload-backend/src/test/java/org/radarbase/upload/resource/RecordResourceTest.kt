package org.radarbase.upload.resource

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.BufferedSink
import org.glassfish.jersey.test.JerseyTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.config.ConfigLoader
import org.radarbase.upload.Config
import org.radarbase.upload.api.RecordDTO
import org.radarbase.upload.api.RecordDataDTO
import org.radarbase.upload.api.SourceTypeDTO
import org.radarbase.upload.logger
import org.radarbase.upload.mock.MockResourceEnhancerFactory
import org.radarcns.auth.token.RadarToken
import java.io.IOException
import java.net.URI
import java.nio.file.Path
import java.util.*
import javax.ws.rs.client.Entity
import javax.ws.rs.core.Application
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ContextResolver

internal class RecordResourceTest: JerseyTest() {
    lateinit var config: Config

    lateinit var authQueue: Queue<Pair<Auth?, Boolean>>
    lateinit var auth: Auth

    @BeforeEach
    fun doSetup() {
        super.setUp()
        assertThat(client, not(nullValue()))
        client.register(ContextResolver { ObjectMapper()
                .registerModule(KotlinModule())
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) })
    }

    @AfterEach
    fun doTeardown() {
        super.tearDown()
    }

    override fun configure(): Application {
        config = Config(
                baseUri = URI.create("http://localhost:10313/upload/api/"),
                jdbcUrl = "jdbc:h2:file:${tempDir.resolve("db.h2")};DB_CLOSE_DELAY=-1",
                sourceTypes = listOf(
                        SourceTypeDTO("type1", null, null, null, null, null)
                ))
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
                "b" to listOf("u3"))

        val enhancerFactory = MockResourceEnhancerFactory(config, authQueue, projects)
        return ConfigLoader.createResourceConfig(enhancerFactory.createEnhancers())
    }

    @Test
    fun create() {
        authQueue.add(Pair(auth, true))
        val record = target("records")
                .request()
                .header("Authorization", "Bearer abcdef")
                .post(Entity.json(RecordDTO(
                        id = null,
                        data = RecordDataDTO(
                                projectId = "a",
                                userId = "u1",
                                sourceId = "s1"
                        ),
                        sourceType = "type1",
                        metadata = null
                ))).use { response ->
                    logger.info(response.toString())
                    response.readEntity(RecordDTO::class.java)
                }

        val okClient = OkHttpClient()
        val request = okhttp3.Request.Builder()
                .url(baseUri.resolve("records/${record.id}/contents/test2.jpg").toURL())
                .header("Authorization", "Bearer abcdef")
                .put(object : RequestBody() {
                    override fun contentType() = "text/plain".toMediaType()
                    override fun writeTo(sink: BufferedSink) {
                        repeat(100) {
                            sink.write(ByteArray(64_000))
                            sink.flush()
                            Thread.sleep(100)
                        }
                    }
                })
                .build()
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

    companion object {
        @JvmStatic
        @TempDir
        lateinit var tempDir: Path
    }
}
