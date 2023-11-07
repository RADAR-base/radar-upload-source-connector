package org.radarbase.upload.api

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.radarbase.upload.Config
import org.radarbase.upload.doa.entity.Record
import org.radarbase.upload.doa.entity.RecordContent
import java.net.URI
import java.time.Instant

internal class RecordMapperImplTest {

    @Test
    fun fromContent() = runBlocking {
        val record = Record().apply {
            id = 1L
        }
        val content = RecordContent().apply {
            this.record = record
            fileName = "test 123 (1).zip"
            contentType = "application/zip"
            size = 11L
            createdDate = Instant.EPOCH
        }
        record.contents = mutableSetOf(content)

        val mapper = RecordMapperImpl(
            mock {
                on { baseUri } doReturn URI.create("http://localhost/upload/")
            },
            mock(),
            mock(),
            Config(advertisedBaseUri = URI.create("https://localhost/upload/")),
            mock(),
        )

        val expected = ContentsDTO(
            url = "https://localhost/upload/records/1/contents/test%20123%20%281%29.zip",
            createdDate = Instant.EPOCH,
            contentType = "application/zip",
            size = 11L,
            fileName = "test 123 (1).zip",
        )

        val actual = mapper.fromContent(content)

        assertEquals(expected, actual)
    }
}
