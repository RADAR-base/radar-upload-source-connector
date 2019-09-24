package org.radarbase.upload.api

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.mock
import org.radarbase.upload.Config
import org.radarbase.upload.doa.SourceTypeRepository
import org.radarbase.upload.doa.entity.Record
import org.radarbase.upload.doa.entity.RecordContent
import java.net.URI
import java.time.Instant


internal class RecordMapperImplTest {

    @Test
    fun fromContent() {
        val content = RecordContent().apply {
            fileName = "test 123 (1).zip"
            contentType = "application/zip"
            size = 11L
            createdDate = Instant.EPOCH
        }

        content.record = Record().apply {
            id = 1L
            contents = mutableSetOf(content)
        }

        val mapper = RecordMapperImpl(
                mock {
                    on { baseUri } doReturn URI.create("http://localhost/upload/")
                },
                mock(SourceTypeRepository::class.java),
                Config(advertisedBaseUri = URI.create("https://localhost/upload/")))

        val expected = ContentsDTO(
                url = "https://localhost/upload/records/1/contents/test%20123%20%281%29.zip",
                createdDate = Instant.EPOCH,
                contentType = "application/zip",
                size = 11L,
                fileName = "test 123 (1).zip")

        val actual = mapper.fromContent(content)

        assertEquals(expected, actual)
    }
}
