package org.radarbase.upload.api

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.mock
import org.radarbase.upload.doa.entity.Record
import org.radarbase.upload.doa.entity.RecordContent
import java.net.URI
import java.time.Instant
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.PathSegment
import javax.ws.rs.core.UriBuilder
import javax.ws.rs.core.UriInfo

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

        val mapper = RecordMapperImpl().apply {
            uri = mock {
                on { baseUri } doReturn URI.create("http://localhost/upload/")
            }
        }

        val expected = ContentsDTO(
                url = "http://localhost/upload/records/1/contents/test%20123%20%281%29.zip",
                createdDate = Instant.EPOCH,
                contentType = "application/zip",
                size = 11L,
                fileName = "test 123 (1).zip")

        val actual = mapper.fromContent(content)

        assertEquals(expected, actual)
    }
}
